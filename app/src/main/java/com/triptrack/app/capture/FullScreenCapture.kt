package com.triptrack.app.capture

import android.app.KeyguardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import com.triptrack.app.model.parseWazeScreenText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

/**
 * One complete display PNG -> OCR -> persisted candidate -> deletion at a time.
 * Intermediate ImageReader buffers are drained without saving while that cycle
 * runs. No fixed frame rate, region selection, crop, video or retained bitmap.
 */
class FullScreenCapture(
    context: Context,
    private val projection: MediaProjection,
    scope: CoroutineScope,
    private val isWazeVisible: () -> Boolean,
    private val onFrame: suspend (CapturedReading) -> Unit,
    onGap: suspend (String?) -> Unit,
    private val onStopped: () -> Unit,
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)
    private val keyguard = appContext.getSystemService(KeyguardManager::class.java)
    private val store = TemporaryFrameStore(appContext.cacheDir)
    private val recognizer = FrameTextRecognizer()
    private val captureJob = SupervisorJob(scope.coroutineContext[Job])
    private val processingScope = CoroutineScope(scope.coroutineContext + captureJob + Dispatchers.Default)
    private val captureThread = HandlerThread("TripTrack-full-screen").apply { start() }
    private val handler = Handler(captureThread.looper)
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val processing = AtomicBoolean(false)
    private val terminalFailure = AtomicBoolean(false)
    private val finalized = AtomicBoolean(false)
    private val generation = AtomicLong(0)
    private val previousCycleEndedMonotonicNanos = AtomicLong(0)
    private val size = AtomicReference<CaptureSize?>(null)
    private val contentVisible = AtomicBoolean(true)
    private val resourcesReleased = CompletableDeferred<Unit>()
    private val closeCompleted = CompletableDeferred<Unit>()
    private val gaps = CaptureGapReporter(processingScope, onGap) {
        requestClose(stopProjection = true, notifyStopped = true)
    }
    private val projectionWindowManager by lazy {
        if (Build.VERSION.SDK_INT >= 30) {
            val display = checkNotNull(displayManager.getDisplay(Display.DEFAULT_DISPLAY))
            appContext.createDisplayContext(display)
                .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION, null)
                .getSystemService(WindowManager::class.java)
        } else appContext.getSystemService(WindowManager::class.java)
    }

    // MediaProjection and ImageReader access, and these fields, stay on handler.
    private var reader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var lastEligibilityReason: String? = "Waiting for visible Waze"
    private var eligibleSinceMonotonicNanos = Long.MAX_VALUE

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            requestClose(stopProjection = false, notifyStopped = true)
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (!closed.get() && width > 0 && height > 0) {
                try {
                    resize(CaptureSize(width, height, displaySize().densityDpi))
                } catch (_: Exception) {
                    failCapture("Full-screen capture resize failed")
                }
            }
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            contentVisible.set(isVisible)
            updateEligibility(eligibilityReason(size.get()), System.nanoTime())
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) failCapture("Device display unavailable")
        }

        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY && Build.VERSION.SDK_INT < 34 && !closed.get()) {
                try {
                    resize(displaySize())
                } catch (_: Exception) {
                    failCapture("Full-screen capture resize failed")
                }
            }
        }
    }

    init {
        captureJob.invokeOnCompletion {
            close()
            completeCloseWhenReady()
        }
    }

    fun start() {
        check(!closed.get()) { "A closed projection needs new user consent" }
        check(started.compareAndSet(false, true)) { "Capture has already started" }
        handler.post {
            if (closed.get()) return@post
            try {
                TemporaryFrameStore.cleanup(appContext.cacheDir)
                val initialSize = displaySize()
                projection.registerCallback(projectionCallback, handler)
                displayManager.registerDisplayListener(displayListener, handler)
                reader = createReader(initialSize)
                size.set(initialSize)
                virtualDisplay = projection.createVirtualDisplay(
                    "TripTrack complete display", initialSize.width, initialSize.height,
                    initialSize.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader!!.surface, null, handler,
                )
                updateEligibility(eligibilityReason(initialSize), System.nanoTime())
            } catch (_: Exception) {
                failCapture("Full-screen capture could not start; new consent is required")
            }
        }
    }

    private fun createReader(captureSize: CaptureSize): ImageReader = ImageReader.newInstance(
        captureSize.width, captureSize.height, PixelFormat.RGBA_8888, 2,
    ).apply {
        setOnImageAvailableListener(::receiveFrame, handler)
    }

    private fun resize(nextSize: CaptureSize) {
        if (nextSize == size.get()) return
        generation.incrementAndGet()
        eligibleSinceMonotonicNanos = System.nanoTime()
        val previousReader = reader
        val nextReader = createReader(nextSize)
        try {
            virtualDisplay?.resize(nextSize.width, nextSize.height, nextSize.densityDpi)
            virtualDisplay?.surface = nextReader.surface
            reader = nextReader
            size.set(nextSize)
            previousReader?.setOnImageAvailableListener(null, null)
            previousReader?.close()
            updateEligibility(eligibilityReason(nextSize), System.nanoTime())
        } catch (failure: Exception) {
            nextReader.close()
            throw failure
        }
    }

    private fun receiveFrame(sourceReader: ImageReader) {
        if (closed.get() || sourceReader !== reader) return
        val image = try {
            sourceReader.acquireLatestImage()
        } catch (_: Exception) {
            failCapture("Full-screen frame unavailable")
            return
        } ?: return
        var bitmap: Bitmap? = null
        var ownsProcessing = false
        try {
            val receipt = clockSnapshot()
            val frameSize = size.get() ?: return
            val reason = eligibilityReason(frameSize)
            updateEligibility(reason, receipt.monotonicNanos)
            if (reason != null || processing.get() || closed.get() || terminalFailure.get()) return
            // Drop queued pixels from before Waze was confirmed visible, including
            // the first frame on return. A following source frame must be newer.
            if (image.timestamp < maxOf(eligibleSinceMonotonicNanos, previousCycleEndedMonotonicNanos.get())) return
            if (image.width != frameSize.width || image.height != frameSize.height) {
                reportGapAsync("Incomplete display frame", generation.get())
                return
            }
            val frameTime = mapFrameTime(image.timestamp, receipt)
            if (frameTime == null) {
                reportGapAsync("Capture source clock needs validation", generation.get())
                return
            }
            if (!processing.compareAndSet(false, true)) return
            ownsProcessing = true
            val frameGeneration = generation.get()
            val sourceTimestamp = image.timestamp
            bitmap = image.toFullScreenBitmap()
            val ownedBitmap = bitmap
            // Enter the finally-bearing method immediately, even if cancellation
            // races launch. Once OCR suspends, resumption uses Dispatchers.Default.
            processingScope.launch(start = CoroutineStart.UNDISPATCHED) {
                processFrame(ownedBitmap, frameTime, sourceTimestamp, receipt, frameGeneration, frameSize)
            }
            bitmap = null // The processing coroutine owns it through its finally.
            ownsProcessing = false
        } catch (_: Exception) {
            reportGapAsync("Full-screen frame conversion failed", generation.get())
        } finally {
            image.close()
            bitmap?.recycle()
            if (ownsProcessing) processing.set(false)
        }
    }

    private suspend fun processFrame(
        bitmap: Bitmap,
        frameTime: FrameTime,
        sourceTimestamp: Long,
        receipt: FrameClockSnapshot,
        frameGeneration: Long,
        frameSize: CaptureSize,
    ) {
        var temporaryFrame: java.io.File? = null
        var deletionFailed = false
        try {
            coroutineContext.ensureActive()
            if (!eligibleForPersistence(frameSize, frameGeneration)) return
            // Recheck after conversion, before creating ANY temporary pixel file.
            temporaryFrame = store.create()
            temporaryFrame.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    "Full-screen temporary save failed"
                }
            }
            coroutineContext.ensureActive()
            val recognized = recognizer.recognize(bitmap)
            coroutineContext.ensureActive()
            if (!eligibleForPersistence(frameSize, frameGeneration)) return
            if (recognized.text.isBlank()) {
                reportGap("No readable Waze text", frameGeneration)
                return
            }
            val extraction = parseWazeScreenText(recognized.text, recognized.elements,
                bitmap.width, bitmap.height)
            gaps.flush()
            // Final package/keyguard/portrait/resize gate directly before Room callback.
            if (!eligibleForPersistence(frameSize, frameGeneration)) return
            onFrame(CapturedReading(
                observedAtEpochMillis = frameTime.epochMillis,
                observedElapsedRealtimeNanos = frameTime.elapsedRealtimeNanos,
                receivedAtEpochMillis = receipt.epochMillis,
                processingMillis = (SystemClock.elapsedRealtimeNanos() - receipt.elapsedRealtimeNanos) / 1_000_000L,
                extraction = extraction,
                sourceFrameTimestampNanos = sourceTimestamp,
                textBlocksJson = recognized.textBlocksJson,
                frameWidth = bitmap.width,
                frameHeight = bitmap.height,
            ))
            if (eligibleForPersistence(frameSize, frameGeneration)) reportGap(null, frameGeneration)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            reportGap("Full-screen extraction or data save failed", frameGeneration)
        } finally {
            try {
                temporaryFrame?.let(store::delete)
            } catch (_: Exception) {
                deletionFailed = true
            } finally {
                bitmap.recycle()
                // Clear the slot only AFTER file deletion and bitmap release.
                if (deletionFailed) {
                    failCapture("Temporary frame cleanup failed; capture stopped")
                } else {
                    previousCycleEndedMonotonicNanos.set(System.nanoTime())
                    processing.set(false)
                }
                Log.d("TripTrackCapture", "Complete frame cycle ms=" +
                    (SystemClock.elapsedRealtimeNanos() - receipt.elapsedRealtimeNanos) / 1_000_000L)
            }
        }
    }

    private suspend fun eligibleForPersistence(expectedSize: CaptureSize, expectedGeneration: Long): Boolean {
        if (closed.get() || terminalFailure.get() || generation.get() != expectedGeneration) return false
        val reason = eligibilityReason(expectedSize)
        if (reason != null) {
            reportGap(reason, expectedGeneration)
            return false
        }
        return true
    }

    private fun eligibilityReason(expectedSize: CaptureSize?): String? {
        if (keyguard.isKeyguardLocked) return "Screen locked; Waze capture unavailable"
        if (!runCatching(isWazeVisible).getOrDefault(false)) return "Waze is not displayed"
        if (!contentVisible.get()) return "Captured display is not visible"
        if (expectedSize == null || expectedSize.width >= expectedSize.height ||
            appContext.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return "Landscape Waze capture is unsupported"
        }
        val fullDisplay = runCatching { displaySize() }.getOrNull()
            ?: return "Device display unavailable"
        if (expectedSize.width != fullDisplay.width || expectedSize.height != fullDisplay.height) {
            return "Complete display capture required; restart screen sharing"
        }
        return null
    }

    private fun updateEligibility(reason: String?, nowMonotonicNanos: Long) {
        if (reason == lastEligibilityReason) return
        lastEligibilityReason = reason
        generation.incrementAndGet()
        eligibleSinceMonotonicNanos = if (reason == null) nowMonotonicNanos else Long.MAX_VALUE
        if (reason != null) reportGapAsync(reason, generation.get())
        // Only a successfully analyzed and saved Waze frame closes a gap.
    }

    private fun reportGapAsync(reason: String, expectedGeneration: Long) {
        if (!closed.get() && generation.get() == expectedGeneration) gaps.reportAsync(reason)
    }

    private suspend fun reportGap(reason: String?, expectedGeneration: Long) {
        if (!closed.get() && generation.get() == expectedGeneration) gaps.report(reason)
    }

    private fun failCapture(reason: String) {
        if (closed.get() || !terminalFailure.compareAndSet(false, true)) return
        // Stop acquisition immediately; report terminal detail before cancellation.
        generation.incrementAndGet()
        val failedGeneration = generation.get()
        processingScope.launch {
            try {
                reportGap(reason, failedGeneration)
            } finally {
                requestClose(stopProjection = true, notifyStopped = true)
            }
        }
    }

    /** Explicit shutdown does not call onStopped; system/failure shutdown does. */
    override fun close() = requestClose(stopProjection = true, notifyStopped = false)

    private fun requestClose(stopProjection: Boolean, notifyStopped: Boolean) {
        if (!closed.compareAndSet(false, true)) return
        generation.incrementAndGet()
        processingScope.cancel()
        handler.post {
            try {
                runCatching { displayManager.unregisterDisplayListener(displayListener) }
                runCatching { projection.unregisterCallback(projectionCallback) }
                runCatching { virtualDisplay?.release() }
                virtualDisplay = null
                runCatching { reader?.setOnImageAvailableListener(null, null) }
                runCatching { reader?.close() }
                reader = null
                if (stopProjection) runCatching { projection.stop() }
            } finally {
                resourcesReleased.complete(Unit)
                captureThread.quitSafely()
                completeCloseWhenReady()
                if (notifyStopped) onStopped()
            }
        }
    }

    /** Join safe OCR completion and bitmap/file cleanup before replacing capture. */
    suspend fun awaitClosed() = closeCompleted.await()

    private fun completeCloseWhenReady() {
        if (resourcesReleased.isCompleted && captureJob.isCompleted && finalized.compareAndSet(false, true)) {
            try {
                recognizer.close()
            } finally {
                closeCompleted.complete(Unit)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun displaySize(): CaptureSize {
        val display = checkNotNull(displayManager.getDisplay(Display.DEFAULT_DISPLAY))
        val metrics = DisplayMetrics().also(display::getRealMetrics)
        if (Build.VERSION.SDK_INT >= 30) {
            val bounds = projectionWindowManager.maximumWindowMetrics.bounds
            return CaptureSize(bounds.width(), bounds.height(), metrics.densityDpi)
        }
        return CaptureSize(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
    }

    private data class CaptureSize(val width: Int, val height: Int, val densityDpi: Int)

    companion object {
        /** Remove abandoned PNGs on process startup, without touching other cache content. */
        fun cleanupTemporaryFrames(context: Context) = TemporaryFrameStore.cleanup(context.cacheDir)

        private fun clockSnapshot(): FrameClockSnapshot {
            val before = System.nanoTime()
            val elapsed = SystemClock.elapsedRealtimeNanos()
            val epoch = System.currentTimeMillis()
            val after = System.nanoTime()
            return FrameClockSnapshot(before + (after - before) / 2, elapsed, epoch)
        }
    }
}

package com.triptrack.app.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.triptrack.app.MainActivity
import com.triptrack.app.R
import com.triptrack.app.TripTrackApplication
import com.triptrack.app.capture.FullScreenCapture
import com.triptrack.app.data.local.RecordingGapEntity
import com.triptrack.app.data.local.WazeFrameEntity
import com.triptrack.app.model.MeasurementSource
import com.triptrack.app.model.SampleQuality
import com.triptrack.app.model.TelemetrySample
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.json.JSONArray

/** Phone collection is independent of projection; a stopped projection never ends the ride. */
class RideRecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { (application as TripTrackApplication).container.recorder }
    private val samples = Channel<PhoneEvent>(64)
    private val overflowAt = AtomicLong(0)
    private var rideId: String? = null
    private var starting = false
    @Volatile private var stopping = false
    @Volatile private var saved = false
    @Volatile private var finishAsInterrupted = true
    private val finishMutex = Mutex()
    private var phone: PhoneTelemetryCollector? = null
    private var capture: FullScreenCapture? = null
    private var writer: Job? = null
    private var startup: Job? = null
    private var gaps: RecordingGaps? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private sealed interface PhoneEvent {
        data class Samples(val values: List<TelemetrySample>) : PhoneEvent
        data class Gap(val source: MeasurementSource, val reason: String?) : PhoneEvent
    }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Ride recording", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            scope.launch { stopAndSave(interrupted = false) }
            return START_NOT_STICKY
        }
        if (intent?.action != START || starting || stopping) return START_NOT_STICKY
        val consent: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(CONSENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(CONSENT)
        }
        try {
            foreground(consent != null)
        } catch (_: Exception) {
            if (rideId != null) {
                captureUnavailable("Phone recording continues. Screen capture could not restart.")
            } else {
                RecorderRuntime.status.update { it.copy(phase = RecorderPhase.ERROR,
                    message = "Could not start recording. Check location permission.") }
                stopSelf()
            }
            return START_NOT_STICKY
        }
        starting = true
        startup = scope.launch {
            var phoneReady = phone != null
            try {
                if (rideId == null) {
                    startPhoneRecording()
                    phoneReady = phone != null && !stopping
                }
                if (!stopping && consent != null) attachCapture(intent.getIntExtra(RESULT, -1), consent)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (phoneReady) {
                    gaps?.set(MeasurementSource.WAZE_SCREEN.name, "Waze capture could not start")
                    captureUnavailable("Phone recording continues. Resume Waze capture to try again.")
                } else {
                    scope.launch { stopAndSave(interrupted = true) }
                }
            } finally {
                starting = false
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun startPhoneRecording() {
        RecorderRuntime.status.value = RecorderStatus(phase = RecorderPhase.STARTING,
            message = "Starting recording…")
        repository.recoverInterruptedRides(System.currentTimeMillis())
        if (stopping) return
        FullScreenCapture.cleanupTemporaryFrames(this)
        val ride = repository.createRide()
        rideId = ride.id
        if (stopping) return
        gaps = RecordingGaps(repository, ride.id)
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TripTrack:phone-recording")
        wakeLock!!.acquire()
        writer = writerScope.launch {
            try {
                for (event in samples) {
                    val droppedAt = overflowAt.getAndSet(0)
                    if (droppedAt != 0L) repository.saveGap(RecordingGapEntity(
                        UUID.randomUUID().toString(), ride.id, "PHONE", droppedAt,
                        System.currentTimeMillis(), "Phone buffer overflow; readings are missing"))
                    when (event) {
                        is PhoneEvent.Samples -> {
                            repository.appendSamples(event.values)
                            RecorderRuntime.status.update {
                                it.copy(phoneSamples = it.phoneSamples + event.values.size)
                            }
                        }
                        is PhoneEvent.Gap -> gaps?.set(event.source.name, event.reason)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                scope.launch { stopAndSave(interrupted = true) }
            }
        }
        phone = PhoneTelemetryCollector(this, ride.id,
            onSamples = { enqueue(PhoneEvent.Samples(it)) },
            onGap = { source, reason -> enqueue(PhoneEvent.Gap(source, reason)) })
        phone!!.start()
        RecorderRuntime.status.value = RecorderStatus(RecorderPhase.RECORDING, ride.id,
            "Phone recording continues, including with the screen locked.")
    }

    private fun enqueue(event: PhoneEvent) {
        if (!samples.trySend(event).isSuccess && !stopping) {
            overflowAt.compareAndSet(0, System.currentTimeMillis())
        }
    }

    private suspend fun attachCapture(resultCode: Int, consent: Intent) {
        val old = capture
        capture = null
        old?.close()
        old?.awaitClosed()
        if (stopping) return
        val id = rideId ?: return
        val projection = checkNotNull(getSystemService(MediaProjectionManager::class.java)
            .getMediaProjection(resultCode, consent)) { "Screen capture consent is unavailable" }
        val visibility = WazeVisibility(this)
        lateinit var engine: FullScreenCapture
        engine = FullScreenCapture(this, projection, scope, visibility::isVisible,
            onFrame = { reading ->
                if (!stopping && capture === engine) {
                    repository.saveFrame(WazeFrameEntity(UUID.randomUUID().toString(), id,
                        reading.observedAtEpochMillis, reading.observedElapsedRealtimeNanos,
                        reading.receivedAtEpochMillis, reading.processingMillis,
                        reading.extraction.rawText, JSONObject(reading.extraction.fields)
                            .put("sourceFrameTimestampNanos", reading.sourceFrameTimestampNanos)
                            .put("clockBasis", reading.clockBasis)
                            .put("frameWidth", reading.frameWidth)
                            .put("frameHeight", reading.frameHeight)
                            .put("recognizedBlocks", JSONArray(reading.textBlocksJson))
                            .put("foregroundValidation", "usage-access").toString(),
                        SampleQuality.SUSPECT.name))
                    RecorderRuntime.status.update { it.copy(frameCount = it.frameCount + 1) }
                }
            },
            onGap = { reason ->
                if (!stopping && capture === engine) gaps?.set(MeasurementSource.WAZE_SCREEN.name, reason)
            },
            onStopped = {
                scope.launch {
                    if (!stopping && capture === engine) {
                        gaps?.set(MeasurementSource.WAZE_SCREEN.name,
                            "Screen capture stopped; phone collection continues")
                        captureUnavailable("Phone recording continues. Resume Waze capture after unlocking.")
                        foreground(false)
                    }
                }
            })
        capture = engine
        try {
            engine.start()
            RecorderRuntime.status.update { it.copy(phase = RecorderPhase.RECORDING,
                message = "Recording phone data. Open Waze for live screen analysis.",
                screenCaptureActive = true) }
        } catch (error: Exception) {
            engine.close()
            engine.awaitClosed()
            capture = null
            throw error
        }
    }

    private fun captureUnavailable(message: String) {
        RecorderRuntime.status.update { it.copy(phase = RecorderPhase.RECORDING,
            message = message, screenCaptureActive = false) }
    }

    private suspend fun stopAndSave(interrupted: Boolean) {
        if (stopping) return
        finishAsInterrupted = interrupted
        stopping = true
        RecorderRuntime.status.update { it.copy(phase = RecorderPhase.STOPPING,
            message = "Saving recording…", screenCaptureActive = false) }
        // Let an in-progress Room insert return its identity before finishing it.
        startup?.join()
        phone?.close()
        val endingCapture = capture
        capture = null
        endingCapture?.close()
        endingCapture?.awaitClosed()
        samples.close()
        writer?.join()
        try {
            finishRecording(finishAsInterrupted)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            RecorderRuntime.status.update { it.copy(phase = RecorderPhase.ERROR,
                message = "Recording stopped. Reopen the app to recover saved data.") }
        } finally {
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopping = true
        phone?.close()
        val endingCapture = capture
        capture = null
        endingCapture?.close()
        samples.close()
        releaseWakeLock()
        if (!saved) {
            val pendingStartup = startup
            val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            RecorderRuntime.status.update { it.copy(phase = RecorderPhase.STOPPING,
                message = "Recovering interrupted recording…", screenCaptureActive = false) }
            cleanup.launch {
                try {
                    // Room may have committed a ride before returning its identity.
                    // Keep startup alive until that identity can be finalized.
                    pendingStartup?.join()
                    endingCapture?.awaitClosed()
                    writer?.join()
                    finishRecording(finishAsInterrupted)
                } catch (_: Exception) {
                    // A still-RECORDING row is recovered on the next process startup.
                    RecorderRuntime.status.update { it.copy(phase = RecorderPhase.ERROR,
                        message = "Recording stopped. Reopen the app to recover saved data.",
                        screenCaptureActive = false) }
                } finally {
                    writerScope.cancel()
                    scope.cancel()
                    cleanup.cancel()
                }
            }
        } else {
            writerScope.cancel()
            scope.cancel()
        }
        super.onDestroy()
    }

    /** Explicit Stop and destruction cleanup must finalize a ride exactly once. */
    private suspend fun finishRecording(interrupted: Boolean) = finishMutex.withLock {
        if (saved) return@withLock
        val now = System.currentTimeMillis()
        gaps?.closeAll(now)
        if (interrupted) saveInterruptionGap(now)
        val id = rideId
        if (id != null) repository.finishRide(id, now, interrupted)
        saved = true
        RecorderRuntime.status.update { it.copy(
            phase = if (interrupted) RecorderPhase.INTERRUPTED else RecorderPhase.IDLE,
            message = when {
                id == null -> "Recording stopped before collection started."
                interrupted -> "Recording interrupted. Collected data is saved."
                else -> "Recording saved on this phone."
            }, screenCaptureActive = false) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private suspend fun saveInterruptionGap(now: Long) {
        val id = rideId ?: return
        val lastReceipt = repository.database.recordings().latestPersistedAtEpochMillis(id) ?: now
        repository.saveGap(RecordingGapEntity(UUID.randomUUID().toString(), id, "ALL",
            minOf(lastReceipt, now), now, "Recording interrupted; collection coverage is uncertain"))
    }

    private fun foreground(withCapture: Boolean) {
        val notification = notification()
        if (Build.VERSION.SDK_INT >= 29) {
            val types = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                if (withCapture) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION else 0
            startForeground(NOTIFICATION, notification, types)
        } else startForeground(NOTIFICATION, notification)
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("TripTrack is recording")
            .setContentText("Phone data continues while the screen is locked.")
            .setContentIntent(open).setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Stop ride", stop).build())
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL = "ride-recording"
        private const val NOTIFICATION = 10
        private const val START = "com.triptrack.app.START_RECORDING"
        private const val STOP = "com.triptrack.app.STOP_RECORDING"
        private const val RESULT = "projection-result"
        private const val CONSENT = "projection-consent"
        fun startIntent(context: Context, resultCode: Int, consent: Intent) =
            Intent(context, RideRecordingService::class.java).setAction(START)
                .putExtra(RESULT, resultCode).putExtra(CONSENT, consent)
        fun stopIntent(context: Context) =
            Intent(context, RideRecordingService::class.java).setAction(STOP)
    }
}

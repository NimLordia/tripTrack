package com.triptrack.app.capture

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** Preserve transition order, including a brief switch away and back during OCR. */
internal class CaptureGapReporter(
    scope: CoroutineScope,
    private val onGap: suspend (String?) -> Unit,
    private val onFailure: () -> Unit,
) {
    private data class Request(val reason: String?, val barrierOnly: Boolean, val done: CompletableDeferred<Unit>?)
    private val requests = Channel<Request>(Channel.UNLIMITED)

    init {
        scope.launch {
            var reported = false
            var previousReason: String? = null
            try {
                for (request in requests) {
                    try {
                        if (!request.barrierOnly && (!reported || previousReason != request.reason)) {
                            onGap(request.reason)
                            previousReason = request.reason
                            reported = true
                        }
                        request.done?.complete(Unit)
                    } catch (cancelled: CancellationException) {
                        request.done?.cancel(cancelled)
                        throw cancelled
                    } catch (failure: Exception) {
                        request.done?.completeExceptionally(failure)
                        onFailure()
                        return@launch
                    }
                }
            } finally {
                while (true) {
                    val pending = requests.tryReceive().getOrNull() ?: break
                    pending.done?.cancel()
                }
                requests.cancel()
            }
        }
    }

    fun reportAsync(reason: String) {
        requests.trySend(Request(reason, false, null))
    }

    suspend fun report(reason: String?) {
        val done = CompletableDeferred<Unit>()
        requests.send(Request(reason, false, done))
        done.await()
    }

    /** Prior gap transitions reach Room before another extracted item does. */
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        requests.send(Request(null, true, done))
        done.await()
    }
}

package com.triptrack.app.recording

import kotlinx.coroutines.flow.MutableStateFlow

enum class RecorderPhase { IDLE, STARTING, RECORDING, STOPPING, INTERRUPTED, ERROR }

data class RecorderStatus(
    val phase: RecorderPhase = RecorderPhase.IDLE,
    val rideId: String? = null,
    val message: String = "Ready",
    val phoneSamples: Long = 0,
    val frameCount: Long = 0,
    val screenCaptureActive: Boolean = false,
)

object RecorderRuntime {
    val status = MutableStateFlow(RecorderStatus())
}

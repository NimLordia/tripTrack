package com.triptrack.app.model

enum class MeasurementSource { PHONE_LOCATION, PHONE_MOTION, WAZE_SCREEN, NAVIGATION_CONNECT, EXTERNAL_GNSS }
enum class SampleQuality { ACCEPTED, SUSPECT, UNAVAILABLE }

enum class TelemetryChannel {
    SPEED_MPS, LATITUDE_DEGREES, LONGITUDE_DEGREES, ALTITUDE_METRES, BEARING_DEGREES,
    ACCELERATION_X_MPS2, ACCELERATION_Y_MPS2, ACCELERATION_Z_MPS2,
    ROTATION_X_RAD_PER_SEC, ROTATION_Y_RAD_PER_SEC, ROTATION_Z_RAD_PER_SEC,
}

data class TelemetrySample(
    val id: String,
    val rideId: String,
    val source: MeasurementSource,
    val channel: TelemetryChannel,
    val value: Double?,
    val quality: SampleQuality,
    val observedAtEpochMillis: Long?,
    val observedElapsedRealtimeNanos: Long?,
    val receivedAtEpochMillis: Long,
    val receivedElapsedRealtimeNanos: Long?,
    val accuracy: Double? = null,
    val evidenceRef: String? = null,
)

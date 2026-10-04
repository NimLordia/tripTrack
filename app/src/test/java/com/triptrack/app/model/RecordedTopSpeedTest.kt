package com.triptrack.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordedTopSpeedTest {
    private fun sample(value: Double?, quality: SampleQuality = SampleQuality.ACCEPTED,
        channel: TelemetryChannel = TelemetryChannel.SPEED_MPS) = TelemetrySample(
        id = "sample", rideId = "ride", source = MeasurementSource.PHONE_LOCATION,
        channel = channel, value = value, quality = quality,
        observedAtEpochMillis = 1_000L, observedElapsedRealtimeNanos = 1_000_000L,
        receivedAtEpochMillis = 1_100L, receivedElapsedRealtimeNanos = 1_100_000L,
    )

    @Test fun missingReadingsDoNotBecomeZeroSpeed() {
        assertNull(recordedTopSpeedMetersPerSecond(listOf(sample(null, SampleQuality.UNAVAILABLE))))
        assertNull(recordedTopSpeedMetersPerSecond(emptyList()))
    }

    @Test fun suspectedSpikesAndOtherChannelsDoNotWin() {
        val result = recordedTopSpeedMetersPerSecond(listOf(sample(20.0), sample(32.0),
            sample(500.0, SampleQuality.SUSPECT), sample(800.0, channel = TelemetryChannel.ALTITUDE_METRES)))
        assertEquals(32.0, result!!, 0.0)
    }

    @Test fun malformedAcceptedValuesCannotBecomeTopSpeed() {
        assertNull(recordedTopSpeedMetersPerSecond(listOf(sample(Double.NaN),
            sample(Double.POSITIVE_INFINITY), sample(-1.0))))
    }

    @Test fun aMeasuredZeroRemainsAValidReading() {
        assertEquals(0.0, recordedTopSpeedMetersPerSecond(listOf(sample(0.0)))!!, 0.0)
    }
}

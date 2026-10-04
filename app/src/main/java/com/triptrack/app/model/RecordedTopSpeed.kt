package com.triptrack.app.model

// Selection only: collectors/quality analysis must establish acceptance separately.
fun recordedTopSpeedMetersPerSecond(samples: Iterable<TelemetrySample>): Double? =
    samples.asSequence()
        .filter { it.channel == TelemetryChannel.SPEED_MPS && it.quality == SampleQuality.ACCEPTED }
        .mapNotNull { it.value }
        .filter { it.isFinite() && it >= 0.0 }
        .maxOrNull()

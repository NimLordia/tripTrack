package com.triptrack.app.data

import com.triptrack.app.data.local.RecordingGapEntity
import com.triptrack.app.data.local.RideEntity
import com.triptrack.app.data.local.TelemetryEntity
import com.triptrack.app.data.local.WazeFrameEntity

data class RideInspection(
    val ride: RideEntity?,
    val sampleCount: Long,
    val samples: List<TelemetryEntity>,
    val frameCount: Long,
    val frames: List<WazeFrameEntity>,
    val gaps: List<RecordingGapEntity>,
)

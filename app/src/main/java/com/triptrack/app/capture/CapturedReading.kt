package com.triptrack.app.capture

import com.triptrack.app.model.WazeExtraction

/** A candidate OCR reading, not an accepted speed measurement. */
data class CapturedReading(
    val observedAtEpochMillis: Long,
    val observedElapsedRealtimeNanos: Long,
    val receivedAtEpochMillis: Long,
    /** Time from receipt through full-frame conversion, temporary save and OCR. */
    val processingMillis: Long,
    val extraction: WazeExtraction,
    /** Original Image.timestamp, before mapping its clock to elapsed realtime. */
    val sourceFrameTimestampNanos: Long,
    /** AOSP uses CLOCK_MONOTONIC; the actual phone's mapping still needs validation. */
    val clockBasis: String = "aosp_monotonic_device_unvalidated",
    /** OCR text geometry only, in original complete-frame pixel coordinates. */
    val textBlocksJson: String = "[]",
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
)

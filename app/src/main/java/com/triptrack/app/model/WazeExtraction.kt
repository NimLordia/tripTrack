package com.triptrack.app.model

/** Text and layout candidates from a complete frame, awaiting phone validation. */
data class WazeExtraction(
    val rawText: String,
    val fields: Map<String, String>,
)

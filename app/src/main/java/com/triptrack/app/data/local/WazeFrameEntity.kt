package com.triptrack.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Recognized text and candidate fields; the source frame's pixels are never stored here. */
@Entity(tableName = "waze_frames", indices = [Index("rideId")])
data class WazeFrameEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val observedAtEpochMillis: Long,
    val observedElapsedRealtimeNanos: Long,
    val receivedAtEpochMillis: Long,
    val processingMillis: Long,
    val rawText: String,
    val fieldsJson: String,
    val quality: String,
)

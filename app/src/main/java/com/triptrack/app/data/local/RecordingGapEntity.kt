package com.triptrack.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recording_gaps", indices = [Index("rideId")])
data class RecordingGapEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val source: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long?,
    val reason: String,
)

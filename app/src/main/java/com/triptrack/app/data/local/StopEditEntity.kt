package com.triptrack.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "stop_edits", indices = [Index("rideId")])
data class StopEditEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val excludedFromAnalysis: Boolean,
    val editedAtEpochMillis: Long,
)

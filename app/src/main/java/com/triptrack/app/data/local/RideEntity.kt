package com.triptrack.app.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long? = null,
    val title: String = "Ride",
    @ColumnInfo(defaultValue = "'SAVED'") val recordingState: String = "SAVED",
)

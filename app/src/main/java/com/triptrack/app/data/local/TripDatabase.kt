package com.triptrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RideEntity::class, TelemetryEntity::class, RecordingGapEntity::class,
        StopEditEntity::class, PreferencesEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TripDatabase : RoomDatabase() {
    abstract fun rides(): RideDao
    abstract fun recordings(): RecordingDao
    abstract fun preferences(): PreferencesDao
}

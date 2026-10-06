package com.triptrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RideEntity::class, TelemetryEntity::class, RecordingGapEntity::class,
        StopEditEntity::class, PreferencesEntity::class, WazeFrameEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class TripDatabase : RoomDatabase() {
    abstract fun rides(): RideDao
    abstract fun recordings(): RecordingDao
    abstract fun preferences(): PreferencesDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rides ADD COLUMN recordingState TEXT NOT NULL DEFAULT 'SAVED'")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS waze_frames (
                        id TEXT NOT NULL PRIMARY KEY,
                        rideId TEXT NOT NULL,
                        observedAtEpochMillis INTEGER NOT NULL,
                        observedElapsedRealtimeNanos INTEGER NOT NULL,
                        receivedAtEpochMillis INTEGER NOT NULL,
                        processingMillis INTEGER NOT NULL,
                        rawText TEXT NOT NULL,
                        fieldsJson TEXT NOT NULL,
                        quality TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_waze_frames_rideId ON waze_frames (rideId)")
            }
        }
    }
}

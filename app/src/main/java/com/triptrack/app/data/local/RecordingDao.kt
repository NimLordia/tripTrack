package com.triptrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RecordingDao {
    @Insert
    suspend fun appendSamples(samples: List<TelemetryEntity>)

    @Query("SELECT * FROM telemetry WHERE rideId = :rideId ORDER BY receivedAtEpochMillis, id")
    suspend fun samplesForRide(rideId: String): List<TelemetryEntity>

    @Insert
    suspend fun insertGap(gap: RecordingGapEntity)

    @Upsert
    suspend fun saveStopEdit(edit: StopEditEntity)
}

package com.triptrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Insert
    suspend fun appendSamples(samples: List<TelemetryEntity>)

    @Query("SELECT * FROM telemetry WHERE rideId = :rideId ORDER BY receivedAtEpochMillis, id")
    suspend fun samplesForRide(rideId: String): List<TelemetryEntity>

    @Upsert
    suspend fun insertGap(gap: RecordingGapEntity)

    @Insert
    suspend fun insertFrame(frame: WazeFrameEntity)

    @Query("SELECT COUNT(*) FROM telemetry WHERE rideId = :rideId")
    fun observeSampleCount(rideId: String): Flow<Long>

    @Query("SELECT * FROM telemetry WHERE rideId = :rideId ORDER BY receivedAtEpochMillis DESC, id DESC LIMIT 40")
    fun observeRecentSamples(rideId: String): Flow<List<TelemetryEntity>>

    @Query("SELECT COUNT(*) FROM waze_frames WHERE rideId = :rideId")
    fun observeFrameCount(rideId: String): Flow<Long>

    @Query("SELECT * FROM waze_frames WHERE rideId = :rideId ORDER BY observedAtEpochMillis DESC, id DESC LIMIT 20")
    fun observeRecentFrames(rideId: String): Flow<List<WazeFrameEntity>>

    @Query("SELECT * FROM recording_gaps WHERE rideId = :rideId ORDER BY startedAtEpochMillis DESC, id DESC LIMIT 40")
    fun observeRecentGaps(rideId: String): Flow<List<RecordingGapEntity>>

    @Query("""
        SELECT MAX(receivedAtEpochMillis) FROM (
            SELECT receivedAtEpochMillis FROM telemetry WHERE rideId = :rideId
            UNION ALL
            SELECT receivedAtEpochMillis FROM waze_frames WHERE rideId = :rideId
        )
    """)
    suspend fun latestPersistedAtEpochMillis(rideId: String): Long?

    @Upsert
    suspend fun saveStopEdit(edit: StopEditEntity)
}

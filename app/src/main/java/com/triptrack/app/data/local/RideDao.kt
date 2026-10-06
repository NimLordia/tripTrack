package com.triptrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY startedAtEpochMillis DESC LIMIT 50")
    fun observeRecent(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    fun observeRide(rideId: String): Flow<RideEntity?>

    @Query("SELECT * FROM rides WHERE recordingState = 'RECORDING'")
    suspend fun recordingRides(): List<RideEntity>

    @Query("UPDATE rides SET recordingState = :state, endedAtEpochMillis = :endedAt WHERE id = :rideId")
    suspend fun updateRecordingState(rideId: String, state: String, endedAt: Long?)

    @Insert
    suspend fun insert(ride: RideEntity)
}

package com.triptrack.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM rides ORDER BY startedAtEpochMillis DESC LIMIT 50")
    fun observeRecent(): Flow<List<RideEntity>>

    @Insert
    suspend fun insert(ride: RideEntity)
}

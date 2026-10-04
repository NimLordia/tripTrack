package com.triptrack.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE id = 1")
    fun observe(): Flow<PreferencesEntity?>

    @Upsert
    suspend fun save(preferences: PreferencesEntity)
}

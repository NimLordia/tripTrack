package com.triptrack.app.data

import com.triptrack.app.data.local.PreferencesDao
import com.triptrack.app.data.local.PreferencesEntity
import com.triptrack.app.model.RidePreferences
import kotlinx.coroutines.flow.map

class PreferencesRepository(private val dao: PreferencesDao) {
    val preferences = dao.observe().map { it?.toModel() ?: RidePreferences() }

    suspend fun save(value: RidePreferences) = dao.save(PreferencesEntity.from(value))
}

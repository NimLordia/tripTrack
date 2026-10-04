package com.triptrack.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.triptrack.app.model.RidePreferences

@Entity(tableName = "preferences")
data class PreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val stopFilterMinutes: Int,
    val inactivityMinutes: Int,
    val snoozeMinutes: Int,
) {
    fun toModel() = RidePreferences(stopFilterMinutes, inactivityMinutes, snoozeMinutes)

    companion object {
        fun from(value: RidePreferences) = PreferencesEntity(
            stopFilterMinutes = value.stopFilterMinutes,
            inactivityMinutes = value.inactivityMinutes,
            snoozeMinutes = value.snoozeMinutes,
        )
    }
}

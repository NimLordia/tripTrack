package com.triptrack.app.model

data class RidePreferences(
    val stopFilterMinutes: Int = 5,
    val inactivityMinutes: Int = 5,
    val snoozeMinutes: Int = 5,
) {
    init {
        require(stopFilterMinutes in VALID_MINUTES)
        require(inactivityMinutes in VALID_MINUTES)
        require(snoozeMinutes in VALID_MINUTES)
    }

    companion object {
        val VALID_MINUTES = 1..1440
    }
}

package com.triptrack.app.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RidePreferencesTest {
    @Test fun changingInactivityDoesNotChangeFilterOrSnooze() {
        val changed = RidePreferences().copy(inactivityMinutes = 12)
        assertEquals(5, changed.stopFilterMinutes)
        assertEquals(12, changed.inactivityMinutes)
        assertEquals(5, changed.snoozeMinutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroDurationIsRejected() { RidePreferences(snoozeMinutes = 0) }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStoredDurationIsRejected() { RidePreferences(stopFilterMinutes = -1) }
}

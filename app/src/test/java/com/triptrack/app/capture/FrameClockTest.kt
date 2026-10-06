package com.triptrack.app.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameClockTest {
    @Test fun preservesSourceFrameAgeAcrossSuspendClockOffset() {
        val snapshot = FrameClockSnapshot(
            monotonicNanos = 10_000_000_000L,
            elapsedRealtimeNanos = 610_000_000_000L,
            epochMillis = 1_800_000_000_000L,
        )
        val result = mapFrameTime(9_875_000_000L, snapshot)!!
        assertEquals(1_799_999_999_875L, result.epochMillis)
        assertEquals(609_875_000_000L, result.elapsedRealtimeNanos)
    }

    @Test fun rejectsMissingFutureAndStaleForeignClockValues() {
        val snapshot = FrameClockSnapshot(10_000_000_000L, 610_000_000_000L, 1_800_000_000_000L)
        assertNull(mapFrameTime(0, snapshot))
        assertNull(mapFrameTime(snapshot.elapsedRealtimeNanos, snapshot))
        assertNull(mapFrameTime(4_000_000_000L, snapshot))
    }
}

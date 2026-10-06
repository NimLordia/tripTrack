package com.triptrack.app.capture

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class CaptureGapReporterTest {
    @Test fun keepsBriefInterruptionsInOrderBeforeNextSavedReading() = runTest {
        val events = mutableListOf<String?>()
        val reporterScope = CoroutineScope(coroutineContext + SupervisorJob())
        try {
            val reporter = CaptureGapReporter(reporterScope, onGap = { reason ->
                delay(20) // Simulate a Room write while the screen switches again.
                events.add(reason)
            }, onFailure = { fail("Gap writer unexpectedly failed") })

            reporter.reportAsync("Waze is not displayed")
            reporter.reportAsync("Landscape Waze capture is unsupported")
            reporter.flush()
            events.add("saved candidate")
            reporter.report(null)

            assertEquals(listOf("Waze is not displayed", "Landscape Waze capture is unsupported", "saved candidate", null), events)
        } finally {
            reporterScope.cancel()
        }
    }
}

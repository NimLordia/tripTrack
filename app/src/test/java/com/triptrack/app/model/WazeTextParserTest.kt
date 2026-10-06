package com.triptrack.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WazeTextParserTest {
    @Test fun rawRecognitionTextIsPreservedExactly() {
        val text = "  Waze\r\nSpeed: 54 km/h\nETA 20:10  "
        assertEquals(text, parseWazeText(text).rawText)
    }

    @Test fun bareNumbersAndSpeedLimitsNeverBecomeSpeed() {
        listOf("54", "54\nkm/h", "Speed limit: 54 km/h", "Limit\n54 mph", "Waze\n80\n20:10").forEach { text ->
            assertFalse(text, "speedMps" in parseWazeText(text).fields)
        }
    }

    @Test fun explicitSpeedAndUnitsProduceCandidateConversion() {
        val metric = parseWazeText("Speed:\n54\nkm/h").fields
        assertEquals("km/h", metric["speedUnit"])
        assertEquals(15.0, metric.getValue("speedMps").toDouble(), 0.000001)
        val imperial = parseWazeText("Current speed 50 mph").fields
        assertEquals("mph", imperial["speedUnit"])
        assertEquals(22.352, imperial.getValue("speedMps").toDouble(), 0.000001)
    }

    @Test fun conflictingLabeledSpeedIsNotSelectedArbitrarily() {
        assertFalse("speedMps" in parseWazeText("Speed 54 km/h\nSpeed 72 km/h").fields)
    }

    @Test fun labeledArrivalAndRemainingFieldsAreExtracted() {
        val fields = parseWazeText("ETA 8:10 PM\nRemaining time: 1 hr 12 min\nDistance left: 34.2 km\nDestination: Lake\nTurn right onto Main Street").fields
        assertEquals("8:10 PM", fields["eta"])
        assertEquals("1 hr 12 min", fields["remainingTime"])
        assertEquals("34.2", fields["remainingDistance"])
        assertEquals("km", fields["remainingDistanceUnit"])
        assertEquals("Lake", fields["destination"])
        assertEquals("Turn right onto Main Street", fields["turnInstruction"])
    }

    @Test fun unlabeledUnitsStayCandidatesAndStatusClockIsNotEta() {
        val fields = parseWazeText("Waze\n19:20\n12 min\n8.4 km").fields
        assertEquals("12 min", fields["durationCandidate"])
        assertEquals("8.4 km", fields["distanceCandidate"])
        assertFalse("eta" in fields)
        assertFalse("remainingDistance" in fields)
    }

    @Test fun speedUnitsAreNotMistakenForDistanceOrDuration() {
        val fields = parseWazeText("Speed: 54 km/h").fields
        assertTrue("speedMps" in fields)
        assertFalse("distanceCandidate" in fields)
        assertFalse("durationCandidate" in fields)
    }

    @Test fun impossibleArrivalClockIsNotExtracted() {
        assertFalse("eta" in parseWazeText("ETA 99:99").fields)
        assertFalse("eta" in parseWazeText("ETA 18:30 PM").fields)
    }

    @Test fun hourMinuteDurationIsKeptWholeWithoutInferringFooterRoles() {
        val fields = parseWazeText("12:21\n13:34\n1:13 h\n138 km").fields
        assertEquals("1:13 h", fields["durationCandidate"])
        assertFalse("remainingTime" in fields)
        assertFalse("eta" in fields)
        assertEquals("1:13 h", parseWazeText("Remaining time: 1:13 h").fields["remainingTime"])
    }

    @Test fun invalidColonDurationDoesNotBecomeItsHoursSuffix() {
        assertFalse("durationCandidate" in parseWazeText("1:99 h").fields)
        assertFalse("remainingTime" in parseWazeText("Remaining time: 1:99 h").fields)
    }
}

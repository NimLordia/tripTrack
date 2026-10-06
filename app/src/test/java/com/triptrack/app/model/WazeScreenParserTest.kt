package com.triptrack.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WazeScreenParserTest {
    // Manually transcribed geometry from the supplied portrait screenshot, NOT OCR output.
    // These tests validate candidate assignment, not ML Kit accuracy or live device behavior.
    private val width = 1000
    private val height = 2200
    private val observed = listOf(
        element("12:21", .04, .01, .14, .03),
        element("10", .23, .065, .32, .095), element("m", .34, .065, .40, .095),
        element("0", .12, .785, .16, .810), element("km/h", .10, .811, .18, .827),
        element("13:34", .41, .905, .59, .933),
        element("1:13", .33, .945, .43, .967), element("h", .445, .945, .47, .967),
        element("138", .535, .945, .625, .967), element("km", .635, .945, .705, .967),
        element("שמריהו לוין", .24, .11, .58, .145),
    )
    private val raw = "12:21\n10 m\nשמריהו לוין\n0\nkm/h\n13:34\n1:13 h\n138 km"

    @Test fun observedPortraitAssignsZeroSpeedAndSeparateNavigationDistances() {
        val extraction = parse(raw, observed)
        assertEquals(raw, extraction.rawText)
        assertEquals("0.0", extraction.fields["speed"])
        assertEquals("0.0", extraction.fields["speedMps"])
        assertEquals("km/h", extraction.fields["speedUnit"])
        assertEquals("13:34", extraction.fields["eta"])
        assertEquals("1:13 h", extraction.fields["remainingTime"])
        assertEquals("73", extraction.fields["remainingTimeMinutes"])
        assertEquals("138", extraction.fields["remainingDistance"])
        assertEquals("km", extraction.fields["remainingDistanceUnit"])
        assertEquals("10", extraction.fields["maneuverDistance"])
        assertEquals("m", extraction.fields["maneuverDistanceUnit"])
        assertFalse("distanceCandidate" in extraction.fields)
        assertFalse("durationCandidate" in extraction.fields)
        assertFalse("destination" in extraction.fields)
        assertFalse("turnInstruction" in extraction.fields)
    }

    @Test fun scalingAndOcrElementOrderDoNotChangeAssignment() {
        val scaled = observed.reversed().map {
            it.copy(left = it.left * 2, top = it.top * 2, right = it.right * 2, bottom = it.bottom * 2)
        }
        assertEquals(parse(raw, observed), parseWazeScreenText(raw, scaled, width * 2, height * 2))
    }

    @Test fun combinedElementsAndSpeedConversionAreSupported() {
        val text = observed.filterNot { it.text in listOf("0", "km/h", "1:13", "h", "138", "km") } + listOf(
            element("54\nkm/h", .10, .785, .18, .827),
            element("1:13 h", .33, .945, .47, .967), element("138 km", .535, .945, .705, .967),
        )
        assertEquals("15.0", parse(raw, text).fields["speedMps"])
        assertEquals("73", parse(raw, text).fields["remainingTimeMinutes"])
    }

    @Test fun statusClockAndMapDistancesDoNotBecomeFooterValues() {
        val text = observed + element("600 km", .45, .60, .60, .62)
        assertEquals("13:34", parse(raw, text).fields["eta"])
        assertEquals("138", parse(raw, text).fields["remainingDistance"])
        assertFalse("eta" in parse("12:21", listOf(observed.first())).fields)
    }

    @Test fun missingFooterAnchorsAndInvalidColonMinutesDoNotPromoteTuple() {
        listOf("13:34", "h", "138", "km").forEach { missing ->
            assertNoFooter(parse(raw, observed.filterNot { it.text == missing }).fields)
        }
        val malformed = observed.map { if (it.text == "1:13") it.copy(text = "1:60") else it }
        assertNoFooter(parse(raw.replace("1:13", "1:60"), malformed).fields)
    }

    @Test fun competingNumeralsOrSpeedGroupsAreRejected() {
        val competingNumeral = observed + element("60", .13, .785, .17, .810)
        assertFalse("speedMps" in parse(raw, competingNumeral).fields)
        val competingGroup = observed + element("60 mph", .20, .785, .27, .827)
        assertFalse("speedMps" in parse(raw, competingGroup).fields)
    }

    @Test fun nearbyLimitLabelDoesNotBecomeCurrentSpeed() {
        val labeledLimit = observed + element("Speed limit", .06, .755, .22, .777)
        assertFalse("speedMps" in parse(raw, labeledLimit).fields)
    }

    @Test fun distantSpeedLimitLabelDoesNotSuppressCurrentGauge() {
        val labeledLimit = observed + element("Speed limit", .45, .60, .65, .62)
        assertEquals("0.0", parse("Speed limit: 60 km/h\n$raw", labeledLimit).fields["speedMps"])
    }

    @Test fun agreeingDistanceLabelDoesNotVetoTheRemainingTimeRole() {
        val fields = parse("Remaining distance: 138 km\n$raw", observed).fields
        assertEquals("13:34", fields["eta"])
        assertEquals("73", fields["remainingTimeMinutes"])
        assertEquals("138", fields["remainingDistance"])
    }

    @Test fun ambiguousFooterClockOrDistanceIsRejected() {
        val competingClock = observed + element("14:00", .42, .865, .58, .889)
        assertNoFooter(parse(raw, competingClock).fields)
        val competingDistance = observed + element("140 km", .535, .945, .705, .967)
        assertNoFooter(parse(raw, competingDistance).fields)
    }

    @Test fun unpairedAndDistantUnitsDoNotProduceSpeed() {
        val noNumber = observed.filterNot { it.text == "0" }
        assertFalse("speedMps" in parse(raw, noNumber).fields)
        val distant = observed.map { if (it.text == "0") it.copy(top = 1200, bottom = 1230) else it }
        assertFalse("speedMps" in parse(raw, distant).fields)
    }

    @Test fun missingInvalidOrLandscapeGeometryDoesNotPromoteUnlabeledText() {
        assertNoPromotedFields(parse(raw, emptyList()).fields)
        val invalid = observed.map { it.copy(left = -1) }
        assertNoPromotedFields(parse(raw, invalid).fields)
        assertNoPromotedFields(parseWazeScreenText(raw, observed, height, width).fields)
        assertNoPromotedFields(parseWazeScreenText(raw, observed, 0, height).fields)
    }

    @Test fun explicitFallbackIsPreservedWithoutGeometryAndConflictRemovesContestedRoles() {
        val labeled = "Speed: 54 km/h\nETA 14:00\nRemaining time: 1 h\nDistance left: 150 km"
        assertEquals(parseWazeText(labeled), parse(labeled, emptyList()))
        assertEquals(emptyMap<String, String>(), parseWazeScreenText(labeled, observed, height, width).fields)
        val conflicting = parse(labeled, observed).fields
        assertFalse("speedMps" in conflicting)
        assertNoFooter(conflicting)
    }

    @Test fun geometryDoesNotReintroduceRolesRejectedByContradictoryExplicitLabels() {
        val conflictingLabels = "Speed: 54 km/h\nSpeed: 72 km/h\nETA 14:00\nETA 15:00"
        val fields = parse(conflictingLabels, observed).fields
        assertFalse("speedMps" in fields)
        assertNoFooter(fields)
    }

    private fun parse(raw: String, text: List<WazeTextElement>) = parseWazeScreenText(raw, text, width, height)
    private fun element(text: String, left: Double, top: Double, right: Double, bottom: Double) = WazeTextElement(
        text, (left * width).toInt(), (top * height).toInt(), (right * width).toInt(), (bottom * height).toInt(),
    )
    private fun assertNoFooter(fields: Map<String, String>) {
        listOf("eta", "remainingTime", "remainingTimeMinutes", "remainingDistance", "remainingDistanceUnit")
            .forEach { assertFalse(it, it in fields) }
    }
    private fun assertNoPromotedFields(fields: Map<String, String>) {
        assertNoFooter(fields)
        assertFalse("speedMps" in fields)
        assertFalse("maneuverDistance" in fields)
    }
}

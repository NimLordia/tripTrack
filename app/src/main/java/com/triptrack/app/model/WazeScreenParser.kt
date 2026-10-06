package com.triptrack.app.model

/** One OCR element from recognition of the complete frame, in original bitmap coordinates. */
data class WazeTextElement(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * Assigns candidate roles after complete-frame recognition, using the observed portrait layout.
 * These candidates still need OCR and device validation; geometry does not validate Waze identity.
 */
fun parseWazeScreenText(
    rawText: String,
    elements: List<WazeTextElement>,
    frameWidth: Int,
    frameHeight: Int,
): WazeExtraction {
    if (frameWidth <= 0 || frameHeight <= frameWidth) return WazeExtraction(rawText, emptyMap())
    val fields = parseWazeText(rawText).fields.toMutableMap()
    val text = elements.mapNotNull { element ->
        if (element.left < 0 || element.top < 0 || element.right > frameWidth ||
            element.bottom > frameHeight || element.left >= element.right || element.top >= element.bottom
        ) return@mapNotNull null
        LocatedText(cleanText(element.text), Box(
            element.left.toDouble() / frameWidth, element.top.toDouble() / frameHeight,
            element.right.toDouble() / frameWidth, element.bottom.toDouble() / frameHeight,
        ))
    }.distinct()

    val speeds = measurements(text, speedUnits, allowStacked = true)
        .filter { it.box.centerX in 0.03..0.28 && it.box.centerY in 0.68..0.86 }
        .filterNot { candidate -> text.any { label ->
            limitLabelRegex.matches(label.text) &&
                kotlin.math.abs(label.box.centerX - candidate.box.centerX) <= 0.10 &&
                kotlin.math.abs(label.box.centerY - candidate.box.centerY) <= 0.05
        } }
    when (speeds.size) {
        1 -> {
            val speed = speeds.single().value
            val value = speed.number.toDouble()
            val metersPerSecond = if (speed.unit == "mph") value * 0.44704 else value / 3.6
            val candidate = mapOf("speed" to value.toString(), "speedUnit" to speed.unit,
                "speedMps" to metersPerSecond.toString())
            if (explicitSpeedLabel.containsMatchIn(rawText) && "speedMps" !in fields ||
                fields["speedUnit"]?.let { it != speed.unit } == true ||
                fields["speedMps"]?.let { !sameNumber(it, candidate.getValue("speedMps")) } == true
            ) speedKeys.forEach(fields::remove) else fields.putAll(candidate)
        }
        in 2..Int.MAX_VALUE -> speedKeys.forEach(fields::remove)
    }

    val distances = measurements(text, distanceUnits)
    distances.filter { it.box.centerX in 0.15..0.85 && it.box.centerY in 0.04..0.17 }
        .singleOrNull()?.let { maneuver ->
            fields["maneuverDistance"] = maneuver.value.number
            fields["maneuverDistanceUnit"] = maneuver.value.unit
            fields.remove("distanceCandidate")
        }

    val durationCandidates = durations(text).filter { it.box.isFooterRow() }
    val distanceCandidates = distances.filter { it.box.isFooterRow() }
    val clocks = text.mapNotNull { element -> parseClock(element.text)?.let { Candidate(it, element.box) } }
        .filter { it.box.centerX in 0.30..0.70 && it.box.centerY in 0.86..0.96 }
    val footers = buildList {
        for (clock in clocks) for (duration in durationCandidates) for (distance in distanceCandidates) {
            if (coherentFooter(clock.box, duration.box, distance.box)) add(Footer(clock, duration, distance))
        }
    }.distinct()
    when (footers.size) {
        1 -> {
            val footer = footers.single()
            val failedExplicitRole = explicitFooterLabels.any { (key, label) ->
                label.containsMatchIn(rawText) && key !in fields
            }
            if (failedExplicitRole || footer.conflictsWith(fields)) {
                footerKeys.forEach(fields::remove)
            } else {
                fields["eta"] = footer.clock.value.text
                fields["remainingTime"] = footer.duration.value.text
                fields["remainingTimeMinutes"] = formatNumber(footer.duration.value.minutes)
                fields["remainingDistance"] = footer.distance.value.number
                fields["remainingDistanceUnit"] = footer.distance.value.unit
                fields.remove("durationCandidate")
                fields.remove("distanceCandidate")
            }
        }
        in 2..Int.MAX_VALUE -> footerKeys.forEach(fields::remove)
    }
    return WazeExtraction(rawText, fields)
}

private data class LocatedText(val text: String, val box: Box)
private data class Candidate<T>(val value: T, val box: Box)
private data class Measurement(val number: String, val unit: String)
private data class Duration(val text: String, val minutes: Double)
private data class Clock(val text: String, val minutesOfDay: Int)
private data class Box(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val centerX get() = (left + right) / 2
    val centerY get() = (top + bottom) / 2
    val height get() = bottom - top
    fun isFooterRow() = centerX in 0.15..0.85 && centerY in 0.89..0.99
    fun union(other: Box) = Box(minOf(left, other.left), minOf(top, other.top),
        maxOf(right, other.right), maxOf(bottom, other.bottom))
}

private data class Footer(
    val clock: Candidate<Clock>, val duration: Candidate<Duration>, val distance: Candidate<Measurement>,
) {
    fun conflictsWith(fields: Map<String, String>): Boolean =
        fields["eta"]?.let { parseClock(it)?.minutesOfDay != clock.value.minutesOfDay } == true ||
            fields["remainingTime"]?.let { parseDuration(it)?.minutes != duration.value.minutes } == true ||
            fields["remainingDistance"]?.let { !sameNumber(it, distance.value.number) } == true ||
            fields["remainingDistanceUnit"]?.let { it != distance.value.unit } == true
}

private fun measurements(
    text: List<LocatedText>, units: Map<String, String>, allowStacked: Boolean = false,
): List<Candidate<Measurement>> = buildList {
    val unitExpression = units.keys.joinToString("|") { Regex.escape(it) }
    val combined = Regex("($number)\\s*($unitExpression)", RegexOption.IGNORE_CASE)
    for (element in text) {
        combined.matchEntire(element.text)?.let { match ->
            finiteNumber(match.groupValues[1])?.let {
                add(Candidate(Measurement(match.groupValues[1], units.getValue(match.groupValues[2].lowercase())), element.box))
            }
        }
        val unit = units[element.text.lowercase()] ?: continue
        for (numeral in text) {
            if (numberRegex.matches(numeral.text) && finiteNumber(numeral.text) != null &&
                adjacent(numeral.box, element.box, allowStacked)
            ) add(Candidate(Measurement(numeral.text, unit), numeral.box.union(element.box)))
        }
    }
}.distinct()

private fun durations(text: List<LocatedText>): List<Candidate<Duration>> = buildList {
    for (element in text) {
        parseDuration(element.text)?.let { add(Candidate(it, element.box)) }
        if (!durationUnitRegex.matches(element.text)) continue
        for (numeral in text) {
            if (!adjacent(numeral.box, element.box)) continue
            parseDuration("${numeral.text} ${element.text}")?.let {
                add(Candidate(it, numeral.box.union(element.box)))
            }
        }
    }
}.distinct()

private fun adjacent(number: Box, unit: Box, allowStacked: Boolean = false): Boolean {
    val sameRow = kotlin.math.abs(number.centerY - unit.centerY) <= maxOf(number.height, unit.height) * 0.6 &&
        number.centerX < unit.centerX && unit.left - number.right in -0.01..0.06
    val stacked = allowStacked && kotlin.math.abs(number.centerX - unit.centerX) <= 0.04 &&
        number.centerY < unit.centerY && unit.top - number.bottom in -0.005..0.025
    return sameRow || stacked
}

private fun coherentFooter(clock: Box, duration: Box, distance: Box): Boolean {
    val row = duration.union(distance)
    return duration.centerX < distance.centerX && distance.left - duration.right in -0.01..0.15 &&
        kotlin.math.abs(duration.centerY - distance.centerY) <= maxOf(duration.height, distance.height) * 0.6 &&
        row.centerY - clock.centerY in 0.01..0.10 && clock.bottom <= row.top + 0.01 &&
        kotlin.math.abs(clock.centerX - row.centerX) <= 0.12
}

private fun parseDuration(text: String): Duration? {
    colonDurationRegex.matchEntire(text)?.let { match ->
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        return Duration("$hours:${match.groupValues[2]} h", hours * 60.0 + minutes)
    }
    hoursAndMinutesRegex.matchEntire(text)?.let { match ->
        val hours = finiteNumber(match.groupValues[1]) ?: return null
        val minutes = finiteNumber(match.groupValues[2]) ?: return null
        return Duration("${match.groupValues[1]} h ${match.groupValues[2]} min", hours * 60 + minutes)
            .takeIf { it.minutes.isFinite() }
    }
    plainDurationRegex.matchEntire(text)?.let { match ->
        val value = finiteNumber(match.groupValues[1]) ?: return null
        val hours = match.groupValues[2].lowercase().startsWith("h")
        return Duration("${match.groupValues[1]} ${if (hours) "h" else "min"}", if (hours) value * 60 else value)
            .takeIf { it.minutes.isFinite() }
    }
    return null
}

private fun parseClock(text: String): Clock? {
    val match = clockRegex.matchEntire(text) ?: return null
    var hour = match.groupValues[1].toInt()
    val suffix = match.groupValues[3].lowercase()
    if (suffix.isNotEmpty()) {
        if (hour !in 1..12) return null
        hour = hour % 12 + if (suffix == "pm") 12 else 0
    }
    return Clock(text, hour * 60 + match.groupValues[2].toInt())
}

private fun finiteNumber(text: String) = text.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0 }
private fun sameNumber(first: String, second: String) = finiteNumber(first) == finiteNumber(second)
private fun formatNumber(value: Double) = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
private fun cleanText(text: String) = text.trim().replace(Regex("\\s+"), " ")
private const val number = "\\d+(?:\\.\\d+)?"
private val numberRegex = Regex(number)
private val speedUnits = mapOf("km/h" to "km/h", "kmh" to "km/h", "kph" to "km/h", "mph" to "mph")
private val limitLabelRegex = Regex("(?:speed\\s+)?limit:?", RegexOption.IGNORE_CASE)
private val distanceUnits = mapOf("km" to "km", "mi" to "mi", "m" to "m")
private val speedKeys = listOf("speed", "speedUnit", "speedMps")
private val footerKeys = listOf("eta", "remainingTime", "remainingTimeMinutes", "remainingDistance", "remainingDistanceUnit")
private fun explicitLabel(expression: String) = Regex("^[ \\t]*(?:$expression)(?=[ \\t:\\r\\n]|$)",
    setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE))
private val explicitSpeedLabel = explicitLabel("(?:current[ \\t]+)?speed(?![ \\t]+limit\\b)")
private val explicitFooterLabels = mapOf(
    "eta" to explicitLabel("eta|arrival|arrive(?:[ \\t]+at)?"),
    "remainingTime" to explicitLabel("remaining[ \\t]+time|time[ \\t]+(?:remaining|left)|remaining(?=[ \\t]*(?::|\\d|[\\r\\n]|$))"),
    "remainingDistance" to explicitLabel("remaining[ \\t]+distance|distance[ \\t]+(?:remaining|left|to[ \\t]+destination)"),
)
private val colonDurationRegex = Regex("(\\d{1,3}):([0-5]\\d)\\s*(?:hours?|hrs?|h)", RegexOption.IGNORE_CASE)
private val hoursAndMinutesRegex = Regex("($number)\\s*(?:hours?|hrs?|h)\\s+($number)\\s*(?:minutes?|mins?|min)", RegexOption.IGNORE_CASE)
private val plainDurationRegex = Regex("($number)\\s*(hours?|hrs?|h|minutes?|mins?|min)", RegexOption.IGNORE_CASE)
private val durationUnitRegex = Regex("hours?|hrs?|h|minutes?|mins?|min", RegexOption.IGNORE_CASE)
private val clockRegex = Regex("([01]?\\d|2[0-3]):([0-5]\\d)(?:\\s*([ap]m))?", RegexOption.IGNORE_CASE)

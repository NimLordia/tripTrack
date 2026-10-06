package com.triptrack.app.model

/** Conservative English text parsing. Bare numbers and speed-limit labels are not speed readings. */
fun parseWazeText(text: String): WazeExtraction {
    val fields = linkedMapOf<String, String>()
    val speedMatches = speedPattern.findAll(text).toList()
    val speeds = speedMatches.map { match ->
        val value = match.groupValues[1].toDouble()
        val unit = if (match.groupValues[2].equals("mph", ignoreCase = true)) "mph" else "km/h"
        Triple(value, unit, if (unit == "mph") value * 0.44704 else value / 3.6)
    }.distinct()
    // Conflicting labels may belong to separate UI elements; don't choose an arbitrary one.
    if (speeds.size == 1) {
        val (value, unit, metersPerSecond) = speeds.single()
        if (value.isFinite() && metersPerSecond.isFinite()) {
            fields["speed"] = value.toString()
            fields["speedUnit"] = unit
            fields["speedMps"] = metersPerSecond.toString()
        }
    }

    singleCandidate(remainingDistancePattern, text)?.let { match ->
        fields["remainingDistance"] = match.groupValues[1]
        fields["remainingDistanceUnit"] = normalizeDistanceUnit(match.groupValues[2])
    }
    singleCandidate(remainingTimePattern, text)?.let { fields["remainingTime"] = it.groupValues[1].trim() }
    singleCandidate(etaPattern, text)?.let { match ->
        val clock = match.groupValues[1].trim()
        val hour = clock.substringBefore(':').toInt()
        if (!clock.contains(Regex("[ap]m", RegexOption.IGNORE_CASE)) || hour in 1..12) {
            fields["eta"] = clock
        }
    }
    singleCandidate(destinationPattern, text)?.let { fields["destination"] = it.groupValues[1].trim() }
    singleCandidate(turnPattern, text)?.let { fields["turnInstruction"] = it.groupValues[1].trim() }

    // Unlabeled units are retained as candidates, without asserting remaining route distance/time.
    if ("remainingDistance" !in fields) {
        singleCandidate(distanceCandidatePattern, text)?.let { match ->
            fields["distanceCandidate"] = match.value.trim()
        }
    }
    if ("remainingTime" !in fields) {
        singleCandidate(durationCandidatePattern, text)?.let { fields["durationCandidate"] = it.value.trim() }
    }
    return WazeExtraction(rawText = text, fields = fields)
}

private fun singleCandidate(pattern: Regex, text: String): MatchResult? {
    val matches = pattern.findAll(text).toList()
    return matches.firstOrNull()?.takeIf { matches.map { it.value.trim().lowercase() }.distinct().size == 1 }
}

private fun normalizeDistanceUnit(unit: String): String = when (unit.lowercase()) {
    "mi", "mile", "miles" -> "mi"
    "m", "meter", "meters", "metre", "metres" -> "m"
    else -> "km"
}

private const val number = "\\d+(?:\\.\\d+)?"
private const val distanceUnit = "(?:km|mi|miles?|meters?|metres?|m)"
private const val duration = "(?:\\d+:[0-5]\\d[ \\t]*(?:hours?|hrs?|h)|$number[ \\t]*(?:hours?|hrs?|h)(?:[ \\t]+$number[ \\t]*(?:minutes?|mins?|min))?|$number[ \\t]*(?:minutes?|mins?|min))"
private val multilineInsensitive = setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)
private val speedPattern = Regex(
    "^[ \\t]*(?:current[ \\t]+)?speed[ \\t]*:?[ \\t\\r\\n]*($number)[ \\t\\r\\n]*(km/h|kmh|kph|mph)\\b",
    multilineInsensitive,
)
private val remainingDistancePattern = Regex(
    "^[ \\t]*(?:remaining[ \\t]+distance|distance[ \\t]+(?:remaining|left|to[ \\t]+destination))[ \\t]*:?[ \\t\\r\\n]*($number)[ \\t]*($distanceUnit)\\b(?![ \\t]*/[ \\t]*h\\b)",
    multilineInsensitive,
)
private val remainingTimePattern = Regex(
    "^[ \\t]*(?:remaining[ \\t]+time|time[ \\t]+(?:remaining|left)|remaining)[ \\t]*:?[ \\t\\r\\n]*($duration)\\b",
    multilineInsensitive,
)
private val etaPattern = Regex(
    "^[ \\t]*(?:eta|arrival|arrive(?:[ \\t]+at)?)[ \\t]*:?[ \\t\\r\\n]*((?:[01]?\\d|2[0-3]):[0-5]\\d(?:[ \\t]*[ap]m)?)\\b",
    multilineInsensitive,
)
private val destinationPattern = Regex("^[ \\t]*destination[ \\t]*:[ \\t]*([^\\r\\n]+)", multilineInsensitive)
private val turnPattern = Regex("^[ \\t]*((?:turn|keep)[ \\t]+(?:left|right)(?:[^\\r\\n]*))", multilineInsensitive)
private val distanceCandidatePattern = Regex("(?<![\\w.])$number[ \\t]*$distanceUnit\\b(?![ \\t]*/[ \\t]*h\\b)", RegexOption.IGNORE_CASE)
private val durationCandidatePattern = Regex("(?<![\\w.:])$duration\\b", RegexOption.IGNORE_CASE)

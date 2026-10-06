package com.triptrack.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.triptrack.app.data.RideInspection
import com.triptrack.app.data.local.RecordingGapEntity
import com.triptrack.app.data.local.TelemetryEntity
import com.triptrack.app.data.local.WazeFrameEntity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RideDetailScreen(
    inspection: RideInspection?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    LazyColumn(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("‹ Saved rides") }
        }
        if (inspection == null) {
            item { Text("Loading recorded readings…") }
            return@LazyColumn
        }
        val ride = inspection.ride
        if (ride == null) {
            item { Text("This ride is unavailable.") }
            return@LazyColumn
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ride.title, style = MaterialTheme.typography.headlineLarge)
                Text(rideStateLabel(ride.recordingState), color = MaterialTheme.colorScheme.primary)
                Text("Started ${recordedTime(ride.startedAtEpochMillis)}")
                ride.endedAtEpochMillis?.let { Text("Ended ${recordedTime(it)}") }
                Text("${inspection.sampleCount} readings · ${inspection.frameCount} Waze results")
                Text("Original readings and capture details for this test run.",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        item { DetailHeading("Waze extraction results", "Showing the latest ${inspection.frames.size} results.") }
        if (inspection.frames.isEmpty()) item { Text("No Waze extraction results saved.") }
        items(inspection.frames, key = { "frame-${it.id}" }) { WazeFrameCard(it) }
        item { DetailHeading("Recorded readings", "Showing the latest ${inspection.samples.size} readings.") }
        if (inspection.samples.isEmpty()) item { Text("No readings saved.") }
        items(inspection.samples, key = { "sample-${it.id}" }) { TelemetryCard(it) }
        item { DetailHeading("Recording gaps", "Missing readings are recorded as gaps.") }
        if (inspection.gaps.isEmpty()) item { Text("No recorded gaps.") }
        items(inspection.gaps, key = { "gap-${it.id}" }) { GapCard(it) }
        item { Text("", Modifier.padding(bottom = 12.dp)) }
    }
}

@Composable
private fun DetailHeading(title: String, description: String) {
    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun WazeFrameCard(frame: WazeFrameEntity) {
    val candidateFields = remember(frame.fieldsJson) { readableFields(frame.fieldsJson) }
    val clockNote = remember(frame.fieldsJson) { captureClockNote(frame.fieldsJson) }
    DetailCard {
        Text("Captured ${recordedTime(frame.observedAtEpochMillis)}", style = MaterialTheme.typography.titleSmall)
        Text("Waze screen · ${readableLabel(frame.quality)}")
        Text("Received ${recordedTime(frame.receivedAtEpochMillis)} · ${frame.processingMillis} ms processing",
            style = MaterialTheme.typography.bodySmall)
        Text("Device capture time: ${frame.observedElapsedRealtimeNanos} ns", style = MaterialTheme.typography.bodySmall)
        clockNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Text("Recognized text", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(frame.rawText.ifBlank { "No text recognized." }) }
        Text("Candidate fields", style = MaterialTheme.typography.labelLarge)
        SelectionContainer { Text(candidateFields, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun TelemetryCard(sample: TelemetryEntity) {
    DetailCard {
        Text("${readableLabel(sample.source)} · ${channelLabel(sample.channel)}", style = MaterialTheme.typography.titleSmall)
        Text(sample.value?.let { "$it ${channelUnit(sample.channel)}" }?.trim() ?: "Unavailable")
        Text("Quality: ${readableLabel(sample.quality)}")
        Text("Observed ${recordedTime(sample.observedAtEpochMillis)}", style = MaterialTheme.typography.bodySmall)
        Text("Received ${recordedTime(sample.receivedAtEpochMillis)}", style = MaterialTheme.typography.bodySmall)
        sample.accuracy?.let { Text("Source accuracy: $it", style = MaterialTheme.typography.bodySmall) }
        sample.observedElapsedRealtimeNanos?.let {
            Text("Device observation time: $it ns", style = MaterialTheme.typography.bodySmall)
        }
        sample.evidenceRef?.let { Text("Evidence: $it", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun GapCard(gap: RecordingGapEntity) {
    DetailCard {
        Text(readableLabel(gap.source), style = MaterialTheme.typography.titleSmall)
        Text(gap.reason)
        Text("From ${recordedTime(gap.startedAtEpochMillis)}", style = MaterialTheme.typography.bodySmall)
        Text(gap.endedAtEpochMillis?.let { "Until ${recordedTime(it)}" } ?: "Gap is ongoing", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

private fun recordedTime(epochMillis: Long?): String = epochMillis?.let {
    SimpleDateFormat("dd MMM yyyy HH:mm:ss.SSS", Locale.getDefault()).format(Date(it))
} ?: "unavailable"

private fun readableLabel(value: String): String = value.lowercase(Locale.ROOT)
    .replace('_', ' ').replaceFirstChar { it.titlecase(Locale.getDefault()) }

private val captureDiagnosticKeys = listOf(
    "recognizedBlocks", "frameWidth", "frameHeight", "sourceFrameTimestampNanos",
    "clockBasis", "foregroundValidation",
)

private fun readableFields(fields: String): String = if (fields.isBlank()) {
    "No candidate fields extracted."
} else runCatching {
    val candidates = JSONObject(fields)
    captureDiagnosticKeys.forEach { candidates.remove(it) }
    if (candidates.length() == 0) "No candidate fields extracted." else candidates.toString(2)
}.getOrDefault("Candidate fields could not be displayed.")

private fun captureClockNote(fields: String): String? = runCatching {
    if (JSONObject(fields).optString("clockBasis").contains("unvalidated", ignoreCase = true)) {
        "Capture timing has not been validated on this phone."
    } else null
}.getOrNull()

private fun channelLabel(channel: String): String = when (channel) {
    "SPEED_MPS" -> "Speed"
    "LATITUDE_DEGREES" -> "Latitude"
    "LONGITUDE_DEGREES" -> "Longitude"
    "ALTITUDE_METRES" -> "Altitude"
    "BEARING_DEGREES" -> "Bearing"
    "ACCELERATION_X_MPS2" -> "Acceleration X"
    "ACCELERATION_Y_MPS2" -> "Acceleration Y"
    "ACCELERATION_Z_MPS2" -> "Acceleration Z"
    "ROTATION_X_RAD_PER_SEC" -> "Rotation X"
    "ROTATION_Y_RAD_PER_SEC" -> "Rotation Y"
    "ROTATION_Z_RAD_PER_SEC" -> "Rotation Z"
    else -> readableLabel(channel)
}

private fun channelUnit(channel: String): String = when (channel) {
    "SPEED_MPS" -> "m/s"
    "LATITUDE_DEGREES", "LONGITUDE_DEGREES", "BEARING_DEGREES" -> "°"
    "ALTITUDE_METRES" -> "m"
    "ACCELERATION_X_MPS2", "ACCELERATION_Y_MPS2", "ACCELERATION_Z_MPS2" -> "m/s²"
    "ROTATION_X_RAD_PER_SEC", "ROTATION_Y_RAD_PER_SEC", "ROTATION_Z_RAD_PER_SEC" -> "rad/s"
    else -> ""
}

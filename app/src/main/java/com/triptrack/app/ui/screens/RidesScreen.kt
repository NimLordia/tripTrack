package com.triptrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.triptrack.app.data.local.RideEntity
import com.triptrack.app.recording.RecorderPhase
import com.triptrack.app.recording.RecorderStatus
import java.text.DateFormat
import java.util.Date

@Composable
fun RidesScreen(
    rides: List<RideEntity>,
    status: RecorderStatus,
    onStartRide: () -> Unit,
    onStopRide: () -> Unit,
    onUsageAccess: () -> Unit,
    usageAccessGranted: Boolean,
    hasLocationPermission: Boolean,
    onSelectRide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.padding(top = 28.dp, bottom = 8.dp)) {
                Text("TRIPTRACK", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Your rides", style = MaterialTheme.typography.headlineLarge)
                Text("Record a ride, then inspect it when you stop.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { RecordingCard(status, onStartRide, onStopRide, usageAccessGranted, hasLocationPermission) }
        if (!usageAccessGranted) item {
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Allow Waze detection", style = MaterialTheme.typography.titleMedium)
                    Text("Lets TripTrack check when Waze is on screen.")
                    OutlinedButton(onClick = onUsageAccess) { Text("Allow usage access") }
                }
            }
        }
        item { Text("Saved rides", style = MaterialTheme.typography.titleLarge) }
        if (rides.isEmpty()) item {
            Card {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No rides recorded", style = MaterialTheme.typography.titleMedium)
                    Text("Start a short test, open Waze, then return here to stop and inspect the saved readings.")
                }
            }
        }
        items(rides, key = { it.id }) { ride ->
            Card(onClick = { onSelectRide(ride.id) }) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(ride.title, style = MaterialTheme.typography.titleMedium)
                    Text(DateFormat.getDateTimeInstance().format(Date(ride.startedAtEpochMillis)))
                    Text(rideStateLabel(ride.recordingState), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text("View recorded readings", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("", Modifier.padding(bottom = 8.dp)) }
    }
}

@Composable
private fun RecordingCard(
    status: RecorderStatus,
    onStartRide: () -> Unit,
    onStopRide: () -> Unit,
    usageAccessGranted: Boolean,
    hasLocationPermission: Boolean,
) {
    val active = status.phase in listOf(RecorderPhase.STARTING, RecorderPhase.RECORDING, RecorderPhase.STOPPING)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when (status.phase) {
                RecorderPhase.IDLE -> "Ready to record"
                RecorderPhase.STARTING -> "Starting recording"
                RecorderPhase.RECORDING -> "Recording your ride"
                RecorderPhase.STOPPING -> "Saving your ride"
                RecorderPhase.INTERRUPTED -> "Recording interrupted"
                RecorderPhase.ERROR -> "Recording needs attention"
            }, style = MaterialTheme.typography.titleLarge)
            Text(status.message)
            if (active || status.rideId != null) {
                Text("${status.phoneSamples} phone readings · ${status.frameCount} Waze results",
                    style = MaterialTheme.typography.bodyMedium)
            }
            if (status.phase == RecorderPhase.RECORDING && !status.screenCaptureActive) {
                Text("Phone recording continues. Resume Waze capture to collect its visible readings again.")
                OutlinedButton(onClick = onStartRide, enabled = usageAccessGranted,
                    modifier = Modifier.fillMaxWidth()) { Text("Resume Waze capture") }
            }
            if (!active) {
                Text("Open Waze after starting. Full-screen frames are analyzed and deleted; extracted readings stay on this phone.")
                if (!hasLocationPermission) Text("Starting will ask for location permission to record phone readings.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (active) {
                Button(onClick = onStopRide, enabled = status.phase != RecorderPhase.STOPPING,
                    modifier = Modifier.fillMaxWidth()) { Text("Stop recording") }
            } else {
                Button(onClick = onStartRide, enabled = usageAccessGranted,
                    modifier = Modifier.fillMaxWidth()) { Text("Start recording") }
            }
        }
    }
}

internal fun rideStateLabel(state: String): String = when (state) {
    "RECORDING" -> "Recording"
    "FINISHED" -> "Finished"
    "INTERRUPTED" -> "Interrupted · saved readings available"
    else -> "Saved"
}

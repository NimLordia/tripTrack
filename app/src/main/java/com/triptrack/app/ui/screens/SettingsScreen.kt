package com.triptrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.triptrack.app.model.RidePreferences
import com.triptrack.app.sync.CloudReadiness

@Composable
fun SettingsScreen(
    preferences: RidePreferences,
    cloud: CloudReadiness,
    onSave: (RidePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    var stop by rememberSaveable(preferences.stopFilterMinutes) { mutableStateOf(preferences.stopFilterMinutes.toString()) }
    var inactivity by rememberSaveable(preferences.inactivityMinutes) { mutableStateOf(preferences.inactivityMinutes.toString()) }
    var snooze by rememberSaveable(preferences.snoozeMinutes) { mutableStateOf(preferences.snoozeMinutes.toString()) }
    val values = listOf(stop, inactivity, snooze).map { it.toIntOrNull() }
    val valid = values.all { it != null && it in RidePreferences.VALID_MINUTES }

    Column(modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ride preferences", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 8.dp))
        Text("Set each timing independently. These preferences will be used when recording is available.")
        MinuteInput("Short-stop filter", stop, { stop = it }, "Stops shorter than this can be excluded from analysis.")
        MinuteInput("Inactivity reminder", inactivity, { inactivity = it }, "Time without movement before a reminder.")
        MinuteInput("Remind me later", snooze, { snooze = it }, "How long to snooze a reminder.")
        Button(onClick = {
            onSave(RidePreferences(values[0]!!, values[1]!!, values[2]!!))
        }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Save preferences") }
        Card {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your data", style = MaterialTheme.typography.titleMedium)
                Text("Preferences are saved on this phone.")
                Text(if (cloud == CloudReadiness.NOT_CONFIGURED) "Cloud backup is not connected."
                    else "Cloud connection configured. Ride backup is not available yet.")
            }
        }
    }
}

@Composable
private fun MinuteInput(label: String, value: String, onChange: (String) -> Unit, explanation: String) {
    val valid = value.toIntOrNull()?.let { it in RidePreferences.VALID_MINUTES } == true
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        suffix = { Text("min") }, singleLine = true, isError = !valid,
        supportingText = { Text(if (valid) explanation else "Enter 1–1440 minutes.") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

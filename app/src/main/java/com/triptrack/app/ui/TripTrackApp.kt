package com.triptrack.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.triptrack.app.ui.screens.RidesScreen
import com.triptrack.app.ui.screens.SettingsScreen

@Composable
fun TripTrackApp(model: TripViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val rides by model.rides.collectAsStateWithLifecycle()
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val message by model.message.collectAsStateWithLifecycle()

    Scaffold(bottomBar = {
        NavigationBar {
            listOf("Rides", "Settings").forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    icon = { Text(if (index == 0) "◉" else "⚙") },
                    label = { Text(label) },
                )
            }
        }
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            if (selectedTab == 0) RidesScreen(rides, Modifier.weight(1f))
            else preferences?.let { saved ->
                SettingsScreen(saved, model.cloud, model::savePreferences, Modifier.weight(1f))
            } ?: Text("Loading preferences…", Modifier.padding(20.dp))
            message?.let {
                Text(it, Modifier.padding(PaddingValues(20.dp, 8.dp)),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

package com.triptrack.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.triptrack.app.data.local.RideEntity
import java.text.DateFormat
import java.util.Date

@Composable
fun RidesScreen(rides: List<RideEntity>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.padding(top = 28.dp, bottom = 8.dp)) {
                Text("TRIPTRACK", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Your rides", style = MaterialTheme.typography.headlineLarge)
                Text("The story of every ride, ready when you stop.",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Getting ready to ride", style = MaterialTheme.typography.titleLarge)
                    Text("This first build saves your preferences. Ride recording and route analysis are coming next.")
                }
            }
        }
        if (rides.isEmpty()) item {
            Card {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No rides recorded", style = MaterialTheme.typography.titleMedium)
                    Text("Your saved rides will appear here. You can set your stop and reminder preferences now.")
                }
            }
        }
        items(rides, key = { it.id }) { ride ->
            Card {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(ride.title, style = MaterialTheme.typography.titleMedium)
                    Text(DateFormat.getDateTimeInstance().format(Date(ride.startedAtEpochMillis)))
                }
            }
        }
    }
}

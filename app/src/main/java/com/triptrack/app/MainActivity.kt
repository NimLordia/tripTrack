package com.triptrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.triptrack.app.ui.TripTrackApp
import com.triptrack.app.ui.TripViewModel
import com.triptrack.app.ui.theme.TripTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TripTrackApplication).container
        setContent {
            TripTrackTheme {
                val model: TripViewModel = viewModel(factory = TripViewModel.factory(container))
                TripTrackApp(model)
            }
        }
    }
}

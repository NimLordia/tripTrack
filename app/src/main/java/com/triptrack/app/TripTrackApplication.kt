package com.triptrack.app

import android.app.Application
import com.triptrack.app.data.AppContainer

class TripTrackApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

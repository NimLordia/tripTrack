package com.triptrack.app.data

import android.content.Context
import androidx.room.Room
import com.triptrack.app.data.local.TripDatabase
import com.triptrack.app.sync.FirebaseReadiness

class AppContainer(context: Context) {
    val database = Room.databaseBuilder(context, TripDatabase::class.java, "triptrack.db").build()
    val preferences = PreferencesRepository(database.preferences())
    val cloud = FirebaseReadiness.read(context)
}

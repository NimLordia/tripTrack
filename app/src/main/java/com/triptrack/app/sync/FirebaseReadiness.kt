package com.triptrack.app.sync

import android.content.Context
import com.google.firebase.FirebaseApp

enum class CloudReadiness { NOT_CONFIGURED, CONFIGURED_SYNC_PENDING }

object FirebaseReadiness {
    fun read(context: Context): CloudReadiness =
        if (FirebaseApp.getApps(context).isEmpty()) CloudReadiness.NOT_CONFIGURED
        else CloudReadiness.CONFIGURED_SYNC_PENDING
}

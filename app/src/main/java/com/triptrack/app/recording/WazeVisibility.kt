package com.triptrack.app.recording

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.os.PowerManager

/** Foreground attribution is conservative: unknown or inaccessible state means unavailable. */
class WazeVisibility(private val context: Context) {
    private var cursor = System.currentTimeMillis() - 60_000
    private var foregroundPackage: String? = null

    @Synchronized
    fun isVisible(): Boolean {
        if (!hasUsageAccess(context)) return false
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        if (!power.isInteractive || keyguard.isKeyguardLocked) return false
        val now = System.currentTimeMillis()
        if (now < cursor) {
            cursor = now - 60_000
            foregroundPackage = null
        }
        val events = context.getSystemService(UsageStatsManager::class.java)
            .queryEvents(cursor, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> foregroundPackage = event.packageName
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (foregroundPackage == event.packageName) foregroundPackage = null
                }
            }
        }
        cursor = now - 1
        return foregroundPackage == "com.waze"
    }

    companion object {
        fun hasUsageAccess(context: Context): Boolean {
            val ops = context.getSystemService(AppOpsManager::class.java)
            @Suppress("DEPRECATION")
            val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.packageName)
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }
}

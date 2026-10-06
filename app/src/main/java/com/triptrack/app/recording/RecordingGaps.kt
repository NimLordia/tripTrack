package com.triptrack.app.recording

import com.triptrack.app.data.RecorderRepository
import com.triptrack.app.data.local.RecordingGapEntity
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecordingGaps(private val repository: RecorderRepository, private val rideId: String) {
    private val mutex = Mutex()
    private val open = mutableMapOf<String, RecordingGapEntity>()

    suspend fun set(source: String, reason: String?) = mutex.withLock {
        val previous = open[source]
        if (previous?.reason == reason) return@withLock
        val now = System.currentTimeMillis()
        if (previous != null) {
            repository.saveGap(previous.copy(endedAtEpochMillis = now))
            open.remove(source)
        }
        if (reason != null) {
            val gap = RecordingGapEntity(UUID.randomUUID().toString(), rideId, source, now, null, reason)
            repository.saveGap(gap)
            open[source] = gap
        }
    }

    suspend fun closeAll(atEpochMillis: Long) = mutex.withLock {
        open.values.forEach { repository.saveGap(it.copy(endedAtEpochMillis = atEpochMillis)) }
        open.clear()
    }
}

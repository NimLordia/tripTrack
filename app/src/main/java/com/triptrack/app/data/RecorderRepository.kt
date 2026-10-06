package com.triptrack.app.data

import androidx.room.withTransaction
import com.triptrack.app.data.local.RecordingGapEntity
import com.triptrack.app.data.local.RideEntity
import com.triptrack.app.data.local.TelemetryEntity
import com.triptrack.app.data.local.TripDatabase
import com.triptrack.app.data.local.WazeFrameEntity
import com.triptrack.app.model.TelemetrySample
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecorderRepository(val database: TripDatabase) {
    private val rideDao = database.rides()
    private val recordingDao = database.recordings()
    private val recoveryMutex = Mutex()
    private var recoveredInterruptedRides = false
    val rides: Flow<List<RideEntity>> = rideDao.observeRecent()

    suspend fun createRide(): RideEntity {
        val ride = RideEntity(
            id = UUID.randomUUID().toString(),
            startedAtEpochMillis = System.currentTimeMillis(),
            recordingState = "RECORDING",
        )
        rideDao.insert(ride)
        return ride
    }

    suspend fun finishRide(rideId: String, atEpochMillis: Long, interrupted: Boolean) {
        rideDao.updateRecordingState(
            rideId = rideId,
            state = if (interrupted) "INTERRUPTED" else "FINISHED",
            // An interruption stops collection without deciding the ride's product end time.
            endedAt = if (interrupted) null else atEpochMillis,
        )
    }

    suspend fun appendSamples(samples: List<TelemetrySample>) {
        if (samples.isNotEmpty()) recordingDao.appendSamples(samples.map(TelemetryEntity::from))
    }

    suspend fun saveFrame(frame: WazeFrameEntity) = recordingDao.insertFrame(frame)

    suspend fun saveGap(gap: RecordingGapEntity) = recordingDao.insertGap(gap)

    /** Once per repository, before creating this process's first ride; concurrent callers share recovery. */
    suspend fun recoverInterruptedRides(atEpochMillis: Long) = recoveryMutex.withLock {
        if (!recoveredInterruptedRides) {
            database.withTransaction {
                rideDao.recordingRides().forEach { ride ->
                    val lastReceipt = recordingDao.latestPersistedAtEpochMillis(ride.id)
                        ?: ride.startedAtEpochMillis
                    recordingDao.insertGap(RecordingGapEntity(
                        id = UUID.randomUUID().toString(),
                        rideId = ride.id,
                        source = "ALL",
                        startedAtEpochMillis = minOf(lastReceipt, atEpochMillis),
                        endedAtEpochMillis = atEpochMillis,
                        reason = "Recording process interrupted; exact collection stop time is unknown",
                    ))
                    rideDao.updateRecordingState(ride.id, "INTERRUPTED", null)
                }
            }
            // Only a successful transaction consumes recovery for this process's repository.
            recoveredInterruptedRides = true
        }
    }

    fun inspectRide(rideId: String): Flow<RideInspection> {
        val recordings = combine(
            recordingDao.observeSampleCount(rideId),
            recordingDao.observeRecentSamples(rideId),
            recordingDao.observeFrameCount(rideId),
            recordingDao.observeRecentFrames(rideId),
            recordingDao.observeRecentGaps(rideId),
        ) { sampleCount, samples, frameCount, frames, gaps ->
            RideInspection(null, sampleCount, samples, frameCount, frames, gaps)
        }
        return combine(rideDao.observeRide(rideId), recordings) { ride, inspection ->
            inspection.copy(ride = ride)
        }
    }
}

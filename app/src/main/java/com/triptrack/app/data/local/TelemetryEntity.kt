package com.triptrack.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.triptrack.app.model.TelemetrySample

@Entity(tableName = "telemetry", indices = [Index("rideId")])
data class TelemetryEntity(
    @PrimaryKey val id: String,
    val rideId: String,
    val source: String,
    val channel: String,
    val value: Double?,
    val quality: String,
    val observedAtEpochMillis: Long?,
    val observedElapsedRealtimeNanos: Long?,
    val receivedAtEpochMillis: Long,
    val receivedElapsedRealtimeNanos: Long?,
    val accuracy: Double?,
    val evidenceRef: String?,
) {
    companion object {
        fun from(sample: TelemetrySample) = with(sample) {
            TelemetryEntity(id, rideId, source.name, channel.name, value, quality.name,
                observedAtEpochMillis, observedElapsedRealtimeNanos, receivedAtEpochMillis,
                receivedElapsedRealtimeNanos, accuracy, evidenceRef)
        }
    }
}

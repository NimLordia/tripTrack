package com.triptrack.app.recording

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.triptrack.app.model.MeasurementSource
import com.triptrack.app.model.SampleQuality
import com.triptrack.app.model.TelemetryChannel
import com.triptrack.app.model.TelemetrySample
import java.util.UUID

/** Sampling requests are prototype settings, not a promise about delivered cadence or accuracy. */
class PhoneTelemetryCollector(
    context: Context,
    private val rideId: String,
    private val onSamples: (List<TelemetrySample>) -> Unit,
    private val onGap: (MeasurementSource, String?) -> Unit,
) : SensorEventListener, LocationListener {
    private val location = context.getSystemService(LocationManager::class.java)
    private val sensors = context.getSystemService(SensorManager::class.java)
    private val thread = HandlerThread("TripTrack-phone")
    @Volatile private var running = false

    @SuppressLint("MissingPermission")
    fun start() {
        thread.start()
        running = true
        val handler = Handler(thread.looper)
        onGap(MeasurementSource.PHONE_LOCATION, "Waiting for a new GPS measurement")
        try {
            if (location.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f,
                    this, thread.looper)
            } else {
                onGap(MeasurementSource.PHONE_LOCATION, "GPS is disabled")
                location.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f,
                    this, thread.looper)
            }
        } catch (_: SecurityException) {
            onGap(MeasurementSource.PHONE_LOCATION, "Location permission is unavailable")
        } catch (_: IllegalArgumentException) {
            onGap(MeasurementSource.PHONE_LOCATION, "GPS provider is unavailable")
        }
        val missing = mutableListOf<String>()
        listOf(Sensor.TYPE_ACCELEROMETER to "accelerometer", Sensor.TYPE_GYROSCOPE to "gyroscope")
            .forEach { (type, label) ->
                val sensor = sensors.getDefaultSensor(type, true) ?: sensors.getDefaultSensor(type)
                if (sensor == null || !sensors.registerListener(this, sensor, 100_000, handler)) {
                    missing += label
                }
            }
        onGap(MeasurementSource.PHONE_MOTION,
            missing.takeIf { it.isNotEmpty() }?.joinToString(prefix = "Unavailable sensors: "))
    }

    override fun onLocationChanged(value: Location) {
        if (!running) return
        val receiptEpoch = System.currentTimeMillis()
        val receiptNanos = SystemClock.elapsedRealtimeNanos()
        fun sample(channel: TelemetryChannel, reading: Double?, accuracy: Double? = null) =
            TelemetrySample(UUID.randomUUID().toString(), rideId, MeasurementSource.PHONE_LOCATION,
                channel, reading, if (reading == null) SampleQuality.UNAVAILABLE else SampleQuality.SUSPECT,
                value.time, value.elapsedRealtimeNanos, receiptEpoch, receiptNanos, accuracy)
        onSamples(listOf(
            sample(TelemetryChannel.LATITUDE_DEGREES, value.latitude,
                value.accuracy.takeIf { value.hasAccuracy() }?.toDouble()),
            sample(TelemetryChannel.LONGITUDE_DEGREES, value.longitude,
                value.accuracy.takeIf { value.hasAccuracy() }?.toDouble()),
            sample(TelemetryChannel.SPEED_MPS, value.speed.takeIf { value.hasSpeed() }?.toDouble(),
                value.speedAccuracyMetersPerSecond.takeIf { value.hasSpeedAccuracy() }?.toDouble()),
            sample(TelemetryChannel.ALTITUDE_METRES, value.altitude.takeIf { value.hasAltitude() },
                value.verticalAccuracyMeters.takeIf { value.hasVerticalAccuracy() }?.toDouble()),
            sample(TelemetryChannel.BEARING_DEGREES, value.bearing.takeIf { value.hasBearing() }?.toDouble(),
                value.bearingAccuracyDegrees.takeIf { value.hasBearingAccuracy() }?.toDouble()),
        ))
        onGap(MeasurementSource.PHONE_LOCATION, null)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!running || event.values.size < 3) return
        val channels = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> listOf(TelemetryChannel.ACCELERATION_X_MPS2,
                TelemetryChannel.ACCELERATION_Y_MPS2, TelemetryChannel.ACCELERATION_Z_MPS2)
            Sensor.TYPE_GYROSCOPE -> listOf(TelemetryChannel.ROTATION_X_RAD_PER_SEC,
                TelemetryChannel.ROTATION_Y_RAD_PER_SEC, TelemetryChannel.ROTATION_Z_RAD_PER_SEC)
            else -> return
        }
        val nowEpoch = System.currentTimeMillis()
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val observedEpoch = nowEpoch + (event.timestamp - nowNanos) / 1_000_000
        onSamples(channels.mapIndexed { index, channel ->
            TelemetrySample(UUID.randomUUID().toString(), rideId, MeasurementSource.PHONE_MOTION,
                channel, event.values[index].toDouble(), SampleQuality.SUSPECT,
                observedEpoch, event.timestamp, nowEpoch, nowNanos,
                // SensorManager accuracy status, not a physical error bound.
                accuracy = event.accuracy.toDouble())
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    override fun onProviderDisabled(provider: String) {
        if (running) onGap(MeasurementSource.PHONE_LOCATION, "GPS is disabled")
    }
    override fun onProviderEnabled(provider: String) {
        if (running) onGap(MeasurementSource.PHONE_LOCATION, "Waiting for a new GPS measurement")
    }
    @Deprecated("Legacy provider callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    fun close() {
        running = false
        // Permission can be revoked between starting and releasing the subscription.
        runCatching { location.removeUpdates(this) }
        sensors.unregisterListener(this)
        thread.quitSafely()
    }
}

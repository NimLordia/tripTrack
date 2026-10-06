package com.triptrack.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.triptrack.app.ui.TripTrackApp
import com.triptrack.app.ui.TripViewModel
import com.triptrack.app.ui.theme.TripTrackTheme
import com.triptrack.app.recording.RecorderPhase
import com.triptrack.app.recording.RecorderRuntime
import com.triptrack.app.recording.RideRecordingService
import com.triptrack.app.recording.WazeVisibility
import com.triptrack.app.capture.FullScreenCapture
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var usageAccessGranted by mutableStateOf(false)
    private var locationPermissionGranted by mutableStateOf(false)

    private val locationRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        refreshPermissions()
        if (locationPermissionGranted) requestCapture()
        else RecorderRuntime.status.update { state -> state.copy(message =
            "Location permission is needed to record phone data.") }
    }
    private val captureRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                startForegroundService(RideRecordingService.startIntent(this,
                    result.resultCode, result.data!!))
            } catch (_: Exception) {
                RecorderRuntime.status.update { it.copy(message = "Could not start capture. Please try again.") }
            }
        } else RecorderRuntime.status.update { it.copy(message =
            if (it.phase == RecorderPhase.RECORDING) "Screen capture cancelled. Phone recording continues."
            else "Screen capture cancelled. No recording was started.") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TripTrackApplication).container
        if (RecorderRuntime.status.value.phase == RecorderPhase.IDLE) lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { FullScreenCapture.cleanupTemporaryFrames(this@MainActivity) }
                container.recorder.recoverInterruptedRides(System.currentTimeMillis())
            }
            catch (error: CancellationException) { throw error }
            catch (_: Exception) { RecorderRuntime.status.update {
                it.copy(message = "Could not recover recordings. Please reopen the app.") } }
        }
        setContent {
            TripTrackTheme {
                val model: TripViewModel = viewModel(factory = TripViewModel.factory(container))
                TripTrackApp(model, onStartRide = ::requestRecording,
                    onStopRide = { startService(RideRecordingService.stopIntent(this)) },
                    onUsageAccess = ::openUsageAccess,
                    usageAccessGranted = usageAccessGranted,
                    hasLocationPermission = locationPermissionGranted)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        usageAccessGranted = WazeVisibility.hasUsageAccess(this)
        locationPermissionGranted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecording() {
        refreshPermissions()
        if (!usageAccessGranted) {
            openUsageAccess()
            return
        }
        if (!locationPermissionGranted) {
            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
            locationRequest.launch(permissions.toTypedArray())
        } else requestCapture()
    }

    private fun requestCapture() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val intent = if (Build.VERSION.SDK_INT >= 34) {
            manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
        } else manager.createScreenCaptureIntent()
        captureRequest.launch(intent)
    }

    private fun openUsageAccess() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:$packageName"))
        try { startActivity(intent) }
        catch (_: Exception) { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
    }
}

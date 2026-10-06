package com.triptrack.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.triptrack.app.data.AppContainer
import com.triptrack.app.data.RideInspection
import com.triptrack.app.model.RidePreferences
import com.triptrack.app.recording.RecorderRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TripViewModel(private val container: AppContainer) : ViewModel() {
    private val messageState = MutableStateFlow<String?>(null)
    private val selectedRideState = MutableStateFlow<String?>(null)
    val message = messageState.asStateFlow()
    val selectedRideId = selectedRideState.asStateFlow()
    val recorderStatus = RecorderRuntime.status.asStateFlow()
    val cloud = container.cloud
    val preferences = container.preferences.preferences
        .map<RidePreferences, RidePreferences?> { it }
        .catch { messageState.value = "Could not load settings. Please reopen the app." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val rides = container.recorder.rides
        .catch { messageState.value = "Could not load your rides. Please reopen the app." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val inspection = selectedRideState.flatMapLatest { rideId ->
        if (rideId == null) flowOf<RideInspection?>(null)
        else container.recorder.inspectRide(rideId).map<RideInspection, RideInspection?> { it }
            .catch {
                messageState.value = "Could not load this ride. Please try opening it again."
                emit(null)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectRide(rideId: String?) {
        selectedRideState.value = rideId
    }

    fun savePreferences(value: RidePreferences) {
        viewModelScope.launch {
            try {
                container.preferences.save(value)
                messageState.value = "Settings saved on this phone."
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                messageState.value = "Could not save settings. Please try again."
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TripViewModel::class.java))
                return TripViewModel(container) as T
            }
        }
    }
}

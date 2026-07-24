package com.plabin.librespeedo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plabin.librespeedo.location.LocationClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class SpeedometerUiState(
    val speedKmh: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Float = 0f, // For now, we'll extract bearing from GPS if moving
    val altitude: Double = 0.0,
    val accuracy: Double = 0.0,
    val provider: String = "None",
    val isTracking: Boolean = false,
    val error: String? = null
)

class SpeedometerViewModel(application: Application) : AndroidViewModel(application) {

    private val locationClient = LocationClient(application)

    private val _uiState = MutableStateFlow(SpeedometerUiState())
    val uiState: StateFlow<SpeedometerUiState> = _uiState.asStateFlow()

    fun startTracking() {
        if (_uiState.value.isTracking) return
        
        _uiState.value = _uiState.value.copy(isTracking = true, error = null)

        locationClient.getLocationUpdates(1000L)
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isTracking = false,
                    error = e.message ?: "Unknown error"
                )
            }
            .onEach { location ->
                val speedMs = if (location.hasSpeed()) location.speed else 0f
                val speedKmh = speedMs * 3.6f
                
                val bearing = if (location.hasBearing()) location.bearing else _uiState.value.heading

                _uiState.value = _uiState.value.copy(
                    speedKmh = speedKmh,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    heading = bearing, // Fallback to GPS bearing if compass not ready
                    altitude = if (location.hasAltitude()) location.altitude else 0.0,
                    accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0,
                    provider = location.provider ?: "Unknown"
                )
            }
            .launchIn(viewModelScope)
    }
}

/*
 * Copyright (C) 2026 Patricio Labin Correa (f1r3f0x)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plabin.librespeedo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plabin.librespeedo.location.LocationClient
import com.plabin.librespeedo.utils.SpeedConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Represents the current UI state of the speedometer screen.
 * Contains both location metrics and status flags.
 */
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

/**
 * ViewModel responsible for managing the GPS tracking lifecycle and parsing raw location
 * data from the [LocationClient] into human-readable metrics for the UI.
 *
 * Uses an [AndroidViewModel] to easily inject the application context into the client.
 */
class SpeedometerViewModel(application: Application) : AndroidViewModel(application) {

    private val locationClient = LocationClient(application)

    private val _uiState = MutableStateFlow(SpeedometerUiState())
    val uiState: StateFlow<SpeedometerUiState> = _uiState.asStateFlow()

    /**
     * Begins collecting location updates from the [LocationClient].
     *
     * If tracking is already active, this method does nothing. It parses the raw meters-per-second
     * speed into kilometers-per-hour (km/h) and updates the internal [MutableStateFlow].
     */
    fun startTracking() {
        if (_uiState.value.isTracking) return
        
        _uiState.value = _uiState.value.copy(isTracking = true, error = null)

        locationClient.getLocationUpdates(500L)
            .catch { e ->
                _uiState.value = _uiState.value.copy(
                    isTracking = false,
                    error = e.message ?: "Unknown error"
                )
            }
            .onEach { location ->
                val speedMs = if (location.hasSpeed()) location.speed else 0f
                val speedKmh = SpeedConverter.msToKmh(speedMs)
                
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

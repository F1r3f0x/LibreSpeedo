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
import com.plabin.librespeedo.data.SettingsRepository
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.location.LocationClient
import com.plabin.librespeedo.sensors.SensorClient
import com.plabin.librespeedo.utils.SpeedConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Defines the available modular widgets for the main dashboard screen.
 */
enum class WidgetType {
    /** Primary speedometer display showing numerical speed and unit. */
    SPEEDOMETER,
    /** Real-time compass rose showing current heading and North pointer. */
    COMPASS,
    /** Displays current GPS coordinates (latitude and longitude). */
    POSITION,
    /** Diagnostic readout of location provider, raw sensor metrics, and accuracy. */
    DEBUG,
    /** Demonstration widget displaying placeholder greeting. */
    HELLO_WORLD
}

/**
 * Represents the immutable UI state of the speedometer dashboard.
 *
 * @property speedKmh Current speed in kilometers per hour (km/h).
 * @property latitude Current latitude in decimal degrees.
 * @property longitude Current longitude in decimal degrees.
 * @property heading Active fused heading in degrees (GPS bearing if moving > 3 km/h, else compass azimuth).
 * @property altitude Current altitude above sea level in meters.
 * @property accuracy Estimated horizontal accuracy radius in meters.
 * @property provider Active location provider name (e.g., "fused", "gps", "network").
 * @property accelX Filtered accelerometer X-axis acceleration in m/s².
 * @property accelY Filtered accelerometer Y-axis acceleration in m/s².
 * @property accelZ Filtered accelerometer Z-axis acceleration in m/s².
 * @property gyroX Raw gyroscope X-axis angular velocity in rad/s.
 * @property gyroY Raw gyroscope Y-axis angular velocity in rad/s.
 * @property gyroZ Raw gyroscope Z-axis angular velocity in rad/s.
 * @property magX Filtered geomagnetic field X-axis flux density in μT.
 * @property magY Filtered geomagnetic field Y-axis flux density in μT.
 * @property magZ Filtered geomagnetic field Z-axis flux density in μT.
 * @property compassAzimuth Hardware sensor compass heading in degrees [0, 360).
 * @property gpsBearing GNSS location bearing in degrees [0, 360).
 * @property isTracking True if active location/sensor collection is running.
 * @property error Error message if location updates failed, or null if healthy.
 */
data class SpeedometerUiState(
    val speedKmh: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Float = 0f,
    val altitude: Double = 0.0,
    val accuracy: Double = 0.0,
    val provider: String = "None",
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val compassAzimuth: Float = 0f,
    val gpsBearing: Float = 0f,
    val isTracking: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel responsible for managing the GPS and sensor tracking lifecycles,
 * computing smart fused bearings, and managing dashboard widget configuration.
 *
 * Uses an [AndroidViewModel] to inject the application context into [LocationClient],
 * [SensorClient], and [SettingsRepository].
 *
 * @param application The application instance.
 * @param locationClient Optional client for location updates (defaults to a new [LocationClient]).
 * @param sensorClient Optional client for sensor updates (defaults to a new [SensorClient]).
 * @param settingsRepository Optional repository for settings (defaults to a new [SettingsRepository]).
 * @param coroutineScope Optional coroutine scope override for background streams (defaults to [viewModelScope]).
 */
class SpeedometerViewModel(
    application: Application,
    private val locationClient: LocationClient = LocationClient(application),
    private val sensorClient: SensorClient = SensorClient(application),
    private val settingsRepository: SettingsRepository = SettingsRepository(application),
    private val coroutineScope: CoroutineScope? = null
) : AndroidViewModel(application) {

    private val scope: CoroutineScope
        get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SpeedometerUiState())
    /** StateFlow emitting current location metrics, sensor readings, and tracking status. */
    val uiState: StateFlow<SpeedometerUiState> = _uiState.asStateFlow()

    private val defaultWidgets = listOf(
        WidgetType.SPEEDOMETER,
        WidgetType.COMPASS,
        WidgetType.POSITION,
        WidgetType.DEBUG
    )

    private val _activeWidgets = MutableStateFlow(
        settingsRepository.getActiveWidgets(defaultWidgets.map { it.name })
            .mapNotNull { name ->
                try {
                    WidgetType.valueOf(name)
                } catch (_: Exception) {
                    null
                }
            }
            .takeIf { it.isNotEmpty() } ?: defaultWidgets
    )
    /** StateFlow emitting the ordered list of active dashboard widgets. */
    val activeWidgets: StateFlow<List<WidgetType>> = _activeWidgets.asStateFlow()
    /** StateFlow emitting the custom grid spans configured for dashboard widgets. */
    val widgetSpans: StateFlow<Map<String, WidgetSpan>> = settingsRepository.widgetSpans

    /**
     * Updates and persists the grid span size for a widget.
     *
     * @param widget The [WidgetType] being updated.
     * @param span The new [WidgetSpan] size (HALF, FULL_WIDTH, or LARGE).
     */
    fun setWidgetSpan(widget: WidgetType, span: WidgetSpan) = settingsRepository.setWidgetSpan(widget.name, span)

    private fun saveWidgets() {
        settingsRepository.setActiveWidgets(_activeWidgets.value.map { it.name })
    }

    /**
     * Moves a widget from [from] index to [to] index in the active list and persists the new order.
     *
     * @param from The initial item index.
     * @param to The target destination index.
     */
    fun reorderWidget(from: Int, to: Int) {
        val list = _activeWidgets.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _activeWidgets.value = list
            saveWidgets()
        }
    }

    /**
     * Appends a new widget to the active dashboard and persists the change.
     *
     * @param widget The [WidgetType] to add.
     */
    fun addWidget(widget: WidgetType) {
        if (!_activeWidgets.value.contains(widget)) {
            _activeWidgets.value = _activeWidgets.value + widget
            saveWidgets()
        }
    }

    /**
     * Removes a widget from the active dashboard and persists the change.
     *
     * @param widget The [WidgetType] to remove.
     */
    fun removeWidget(widget: WidgetType) {
        _activeWidgets.value = _activeWidgets.value.filter { it != widget }
        saveWidgets()
    }

    /**
     * Resets the dashboard back to the factory default widgets and clears custom span sizes.
     */
    fun resetLayout() {
        _activeWidgets.value = defaultWidgets
        settingsRepository.clearLayout()
    }

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
                
                val newGpsBearing = if (location.hasBearing()) location.bearing else _uiState.value.gpsBearing
                val activeHeading = if (speedKmh > 3.0f) newGpsBearing else _uiState.value.compassAzimuth

                _uiState.value = _uiState.value.copy(
                    speedKmh = speedKmh,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    gpsBearing = newGpsBearing,
                    heading = activeHeading,
                    altitude = if (location.hasAltitude()) location.altitude else 0.0,
                    accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0,
                    provider = location.provider ?: "Unknown"
                )
            }
            .launchIn(scope)

        sensorClient.getSensorUpdates()
            .onEach { sensorData ->
                val activeHeading = if (_uiState.value.speedKmh > 3.0f) _uiState.value.gpsBearing else sensorData.azimuth
                
                _uiState.value = _uiState.value.copy(
                    accelX = sensorData.accelerometer[0],
                    accelY = sensorData.accelerometer[1],
                    accelZ = sensorData.accelerometer[2],
                    gyroX = sensorData.gyroscope[0],
                    gyroY = sensorData.gyroscope[1],
                    gyroZ = sensorData.gyroscope[2],
                    magX = sensorData.magneticField[0],
                    magY = sensorData.magneticField[1],
                    magZ = sensorData.magneticField[2],
                    compassAzimuth = sensorData.azimuth,
                    heading = activeHeading
                )
            }
            .launchIn(scope)
    }
}

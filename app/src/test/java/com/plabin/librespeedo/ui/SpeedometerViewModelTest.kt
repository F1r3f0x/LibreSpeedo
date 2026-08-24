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
import android.content.Context
import android.location.Location
import com.plabin.librespeedo.data.FakeSharedPreferences
import com.plabin.librespeedo.data.SettingsRepository
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.location.LocationClient
import com.plabin.librespeedo.sensors.SensorClient
import com.plabin.librespeedo.sensors.SensorData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying widget management, reordering, resizing, and state bindings in [SpeedometerViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedometerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var fakeLocationClient: FakeLocationClient
    private lateinit var fakeSensorClient: FakeSensorClient
    private lateinit var viewModel: SpeedometerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val app = Application()
        fakePrefs = FakeSharedPreferences()
        settingsRepository = SettingsRepository(fakePrefs)
        fakeLocationClient = FakeLocationClient(app)
        fakeSensorClient = FakeSensorClient(app)
        viewModel = SpeedometerViewModel(
            application = app,
            locationClient = fakeLocationClient,
            sensorClient = fakeSensorClient,
            settingsRepository = settingsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Verifies that the ViewModel loads the default widgets when no prior saved configuration exists.
     */
    @Test
    fun activeWidgets_defaultsToFactoryList() {
        val active = viewModel.activeWidgets.value
        assertEquals(4, active.size)
        assertEquals(WidgetType.SPEEDOMETER, active[0])
        assertEquals(WidgetType.COMPASS, active[1])
        assertEquals(WidgetType.POSITION, active[2])
        assertEquals(WidgetType.DEBUG, active[3])
    }

    /**
     * Verifies adding a new widget updates activeWidgets and persists to repository.
     */
    @Test
    fun addWidget_appendsWidgetAndPersists() {
        assertFalse(viewModel.activeWidgets.value.contains(WidgetType.HELLO_WORLD))
        viewModel.addWidget(WidgetType.HELLO_WORLD)

        assertTrue(viewModel.activeWidgets.value.contains(WidgetType.HELLO_WORLD))
        assertEquals(5, viewModel.activeWidgets.value.size)
        assertTrue(fakePrefs.getString("active_widgets", "")!!.contains("HELLO_WORLD"))

        // Adding already present widget should be a no-op
        viewModel.addWidget(WidgetType.HELLO_WORLD)
        assertEquals(5, viewModel.activeWidgets.value.size)
    }

    /**
     * Verifies removing a widget updates activeWidgets and persists to repository.
     */
    @Test
    fun removeWidget_filtersWidgetAndPersists() {
        assertTrue(viewModel.activeWidgets.value.contains(WidgetType.DEBUG))
        viewModel.removeWidget(WidgetType.DEBUG)

        assertFalse(viewModel.activeWidgets.value.contains(WidgetType.DEBUG))
        assertEquals(3, viewModel.activeWidgets.value.size)
        assertFalse(fakePrefs.getString("active_widgets", "")!!.contains("DEBUG"))
    }

    /**
     * Verifies reordering widgets swaps positions accurately within list indices.
     */
    @Test
    fun reorderWidget_movesItemCorrectly() {
        // Initial: [SPEEDOMETER, COMPASS, POSITION, DEBUG]
        viewModel.reorderWidget(0, 2)
        // Expected: [COMPASS, POSITION, SPEEDOMETER, DEBUG]
        val active = viewModel.activeWidgets.value
        assertEquals(WidgetType.COMPASS, active[0])
        assertEquals(WidgetType.POSITION, active[1])
        assertEquals(WidgetType.SPEEDOMETER, active[2])
        assertEquals(WidgetType.DEBUG, active[3])

        // Invalid indices should do nothing
        viewModel.reorderWidget(-1, 2)
        viewModel.reorderWidget(0, 99)
        assertEquals(WidgetType.COMPASS, viewModel.activeWidgets.value[0])
    }

    /**
     * Verifies setting widget spans delegates to repository and updates widgetSpans StateFlow.
     */
    @Test
    fun setWidgetSpan_updatesRepositoryAndStateFlow() {
        viewModel.setWidgetSpan(WidgetType.SPEEDOMETER, WidgetSpan.LARGE)
        assertEquals(WidgetSpan.LARGE, viewModel.widgetSpans.value["SPEEDOMETER"])
    }

    /**
     * Verifies resetLayout restores activeWidgets and clears custom spans.
     */
    @Test
    fun resetLayout_restoresDefaults() {
        viewModel.removeWidget(WidgetType.DEBUG)
        viewModel.setWidgetSpan(WidgetType.SPEEDOMETER, WidgetSpan.LARGE)

        viewModel.resetLayout()

        assertEquals(4, viewModel.activeWidgets.value.size)
        assertEquals(WidgetType.SPEEDOMETER, viewModel.activeWidgets.value[0])
        assertTrue(viewModel.widgetSpans.value.isEmpty())
    }

    /**
     * Verifies that startTracking transitions isTracking to true.
     */
    @Test
    fun startTracking_initiatesTrackingState() {
        assertFalse(viewModel.uiState.value.isTracking)
        viewModel.startTracking()
        assertTrue(viewModel.uiState.value.isTracking)

        // Second invocation when already tracking should be a no-op
        viewModel.startTracking()
        assertTrue(viewModel.uiState.value.isTracking)
    }

    /**
     * Verifies that sensor data updates from sensorClient update all corresponding fields in uiState.
     */
    @Test
    fun startTracking_processesSensorUpdates() {
        val sensorData = SensorData(
            accelerometer = floatArrayOf(1.2f, 2.3f, 3.4f),
            gyroscope = floatArrayOf(0.1f, 0.2f, 0.3f),
            magneticField = floatArrayOf(10f, 20f, 30f),
            azimuth = 270f
        )
        fakeSensorClient.flow = flowOf(sensorData)

        viewModel.startTracking()

        val state = viewModel.uiState.value
        assertEquals(1.2f, state.accelX, 0.001f)
        assertEquals(2.3f, state.accelY, 0.001f)
        assertEquals(3.4f, state.accelZ, 0.001f)
        assertEquals(0.1f, state.gyroX, 0.001f)
        assertEquals(0.2f, state.gyroY, 0.001f)
        assertEquals(0.3f, state.gyroZ, 0.001f)
        assertEquals(10f, state.magX, 0.001f)
        assertEquals(20f, state.magY, 0.001f)
        assertEquals(30f, state.magZ, 0.001f)
        assertEquals(270f, state.compassAzimuth, 0.001f)
        assertEquals(270f, state.heading, 0.001f)
    }

    private class FakeLocationClient(context: Context) : LocationClient(context) {
        var flow: Flow<Location> = emptyFlow()
        override fun getLocationUpdates(intervalMs: Long): Flow<Location> = flow
    }

    private class FakeSensorClient(context: Context) : SensorClient(context) {
        var flow: Flow<SensorData> = emptyFlow()
        override fun getSensorUpdates(): Flow<SensorData> = flow
    }
}

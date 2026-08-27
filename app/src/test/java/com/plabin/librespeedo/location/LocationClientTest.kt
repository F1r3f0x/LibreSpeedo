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
package com.plabin.librespeedo.location

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLocationManager
import org.robolectric.shadows.ShadowLooper

/**
 * Unit tests verifying location provider priority, updates emission, and lifecycle in [LocationClient].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocationClientTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var locationClient: LocationClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)
        locationClient = LocationClient(context)
    }

    @Test
    fun getLocationUpdates_whenFusedEnabled_emitsLocationUpdates() = runTest(UnconfinedTestDispatcher()) {
        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, true)

        val testLocation = Location(LocationManager.FUSED_PROVIDER).apply {
            latitude = 37.7749
            longitude = -122.4194
            speed = 15.0f
            bearing = 90.0f
            altitude = 100.0
            accuracy = 3.0f
        }

        val emittedLocations = mutableListOf<Location>()
        val job = launch {
            locationClient.getLocationUpdates(500L).collect {
                emittedLocations.add(it)
            }
        }

        shadowLocationManager.simulateLocation(testLocation)
        ShadowLooper.idleMainLooper()

        assertEquals(1, emittedLocations.size)
        assertEquals(37.7749, emittedLocations[0].latitude, 0.0001)
        assertEquals(-122.4194, emittedLocations[0].longitude, 0.0001)

        job.cancel()
    }

    @Test
    fun getLocationUpdates_whenGpsEnabledAndFusedDisabled_emitsLocationUpdates() = runTest(UnconfinedTestDispatcher()) {
        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)

        val testLocation = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 37.7749
            longitude = -122.4194
            speed = 15.0f
            bearing = 90.0f
            altitude = 100.0
            accuracy = 3.0f
        }

        val emittedLocations = mutableListOf<Location>()
        val job = launch {
            locationClient.getLocationUpdates(500L).collect {
                emittedLocations.add(it)
            }
        }

        shadowLocationManager.simulateLocation(testLocation)
        ShadowLooper.idleMainLooper()

        assertEquals(1, emittedLocations.size)
        assertEquals(37.7749, emittedLocations[0].latitude, 0.0001)
        assertEquals(-122.4194, emittedLocations[0].longitude, 0.0001)

        job.cancel()
    }

    @Test
    fun getLocationUpdates_whenOnlyNetworkEnabled_emitsLocationUpdates() = runTest(UnconfinedTestDispatcher()) {
        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        val testLocation = Location(LocationManager.NETWORK_PROVIDER).apply {
            latitude = 40.7128
            longitude = -74.0060
        }

        val emittedLocations = mutableListOf<Location>()
        val job = launch {
            locationClient.getLocationUpdates(500L).collect {
                emittedLocations.add(it)
            }
        }

        shadowLocationManager.simulateLocation(testLocation)
        ShadowLooper.idleMainLooper()

        assertEquals(1, emittedLocations.size)
        assertEquals(40.7128, emittedLocations[0].latitude, 0.0001)

        job.cancel()
    }

    @Test
    fun getLocationUpdates_whenNoProvidersEnabled_closesWithError() = runTest(UnconfinedTestDispatcher()) {
        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, false)

        var caughtError: Throwable? = null
        locationClient.getLocationUpdates(500L)
            .catch { caughtError = it }
            .collect {}

        assertNotNull(caughtError)
        assertTrue(caughtError?.message?.contains("No location providers are enabled") == true)
    }

    @Test
    fun locationListener_providerStatusChanges_handledSafely() = runTest(UnconfinedTestDispatcher()) {
        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, true)

        val job = launch {
            locationClient.getLocationUpdates(500L).collect {}
        }

        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, false)
        shadowLocationManager.setProviderEnabled(LocationManager.FUSED_PROVIDER, true)
        ShadowLooper.idleMainLooper()

        job.cancel()
    }
}

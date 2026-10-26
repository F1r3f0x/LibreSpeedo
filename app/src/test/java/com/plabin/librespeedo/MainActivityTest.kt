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
package com.plabin.librespeedo

import android.Manifest
import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.ViewModelProvider
import com.plabin.librespeedo.ui.SpeedometerViewModel
import com.plabin.librespeedo.trips.TripDatabase
import com.plabin.librespeedo.trips.TripEntity
import com.plabin.librespeedo.trips.TripStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Unit tests verifying [MainActivity] creation, lifecycle, and permission handling.
 */
@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @Test
    fun settingsAndRecorder_shareLivePreferencesAcrossActivityRecreation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val original = ViewModelProvider(controller.get())[SpeedometerViewModel::class.java]
            val settings = controller.get().settingsRepository
            assertSame(original.settingsRepository, settings)
            settings.setAutomaticTripLogging(true)
            settings.setSelectedCar("00:11:22:33:44:55", "Test car")
            assertTrue(original.settingsRepository.automaticTripLogging.value)
            assertEquals("00:11:22:33:44:55", original.settingsRepository.selectedCarAddress.value)
            assertEquals("Test car", original.settingsRepository.selectedCarName.value)

            controller.recreate()
            val recreatedSettings = controller.get().settingsRepository
            assertSame(original, ViewModelProvider(controller.get())[SpeedometerViewModel::class.java])
            assertSame(settings, recreatedSettings)
            recreatedSettings.setAutomaticTripLogging(false)
            recreatedSettings.setSelectedCar(null, null)
            assertFalse(original.settingsRepository.automaticTripLogging.value)
            assertEquals(null, original.settingsRepository.selectedCarAddress.value)
            assertEquals(null, original.settingsRepository.selectedCarName.value)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun tripStartedAfterSettingsChange_usesCurrentRouteAndMountPreferences() = runBlocking {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val recorder = ViewModelProvider(controller.get())[SpeedometerViewModel::class.java]
            awaitState { recorder.tripLogger.value.isReady }
            val settings = controller.get().settingsRepository
            settings.setStoreTripRoute(true)
            settings.setStableMountConfirmed(true)
            recorder.startManualTrip()
            awaitState { recorder.tripLogger.value.trip != null }
            val firstTrip = recorder.tripLogger.value.trip!!
            assertTrue(firstTrip.routeStorageEnabled)
            assertTrue(firstTrip.mountingConfirmed)

            settings.setStoreTripRoute(false)
            settings.setStableMountConfirmed(false)
            // These settings are captured per trip; the next trip gets the new values.
            assertTrue(recorder.tripLogger.value.trip!!.routeStorageEnabled)
            recorder.finishTrip()
            awaitState { recorder.tripLogger.value.trip == null }
            recorder.startManualTrip()
            awaitState { recorder.tripLogger.value.trip != null }
            assertFalse(recorder.tripLogger.value.trip!!.routeStorageEnabled)
            assertFalse(recorder.tripLogger.value.trip!!.mountingConfirmed)
            recorder.finishTrip()
            awaitState { recorder.tripLogger.value.trip == null }
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test
    fun retentionChangedInActivity_cleansUpSavedTripsWithoutRestart() = runBlocking {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        try {
            val recorder = ViewModelProvider(controller.get())[SpeedometerViewModel::class.java]
            awaitState { recorder.tripLogger.value.isReady }
            val dao = TripDatabase.get(controller.get()).tripDao()
            val now = System.currentTimeMillis()
            fun savedTrip(id: String, ageDays: Int) = TripEntity(
                id = id, startUtcMillis = now - ageDays * 86_400_000L,
                endUtcMillis = now - ageDays * 86_400_000L + 1_000L,
                startElapsedNanos = 0L, lastCheckpointElapsedNanos = 1_000_000_000L,
                bootSession = "test", origin = "MANUAL", status = TripStatus.FINISHED.name
            )
            dao.putTrip(savedTrip("old-trip", 10))
            dao.putTrip(savedTrip("recent-trip", 1))
            assertTrue(dao.observeTrips().first().any { it.id == "old-trip" })

            controller.get().settingsRepository.setTripRetentionDays(7)
            awaitState { dao.observeTrips().first().none { it.id == "old-trip" } }
            assertTrue(dao.observeTrips().first().any { it.id == "recent-trip" })
        } finally {
            controller.pause().stop().destroy()
        }
    }

    private suspend fun awaitState(condition: suspend () -> Boolean) {
        withTimeout(10_000L) {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                if (condition()) break
                delay(10L)
            }
        }
    }

    @Test
    fun stopwatchState_survivesActivityRecreation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val original = ViewModelProvider(controller.get())[SpeedometerViewModel::class.java].chronometer
        original.toggleRunning()
        original.recordLap()
        controller.recreate()
        val restored = ViewModelProvider(controller.get())[SpeedometerViewModel::class.java].chronometer
        assertSame(original, restored)
        assertTrue(restored.isRunning)
        assertTrue(restored.laps.isNotEmpty())
        controller.pause().stop().destroy()
    }

    @Test
    fun mainActivity_launchesSuccessfully_withLocationPermissionGranted() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        assertNotNull(activity)
        controller.pause().stop().destroy()
    }

    @Test
    fun mainActivity_launchesSuccessfully_withoutPermission() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()

        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}

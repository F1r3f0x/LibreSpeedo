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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedometerUiStateTest {

    @Test
    fun defaultState_hasExpectedDefaults() {
        val state = SpeedometerUiState()
        assertEquals(0f, state.speedKmh, 0.001f)
        assertEquals(0.0, state.latitude, 0.0001)
        assertEquals(0.0, state.longitude, 0.0001)
        assertEquals(0f, state.heading, 0.001f)
        assertEquals(0.0, state.altitude, 0.0001)
        assertEquals(0.0, state.accuracy, 0.0001)
        assertEquals("None", state.provider)
        assertEquals(0f, state.accelX, 0.001f)
        assertEquals(0f, state.accelY, 0.001f)
        assertEquals(0f, state.accelZ, 0.001f)
        assertEquals(0f, state.gyroX, 0.001f)
        assertEquals(0f, state.gyroY, 0.001f)
        assertEquals(0f, state.gyroZ, 0.001f)
        assertEquals(0f, state.magX, 0.001f)
        assertEquals(0f, state.magY, 0.001f)
        assertEquals(0f, state.magZ, 0.001f)
        assertEquals(0f, state.compassAzimuth, 0.001f)
        assertEquals(0f, state.gpsBearing, 0.001f)
        assertFalse(state.isTracking)
        assertNull(state.error)
    }

    @Test
    fun stateCopy_updatesFieldsCorrectly() {
        val initialState = SpeedometerUiState()
        val updatedState = initialState.copy(
            speedKmh = 60f,
            latitude = -33.4489,
            longitude = -70.6693,
            heading = 180f,
            altitude = 560.0,
            accuracy = 2.5,
            provider = "fused",
            accelX = 1f,
            accelY = 2f,
            accelZ = 3f,
            gyroX = 4f,
            gyroY = 5f,
            gyroZ = 6f,
            magX = 7f,
            magY = 8f,
            magZ = 9f,
            compassAzimuth = 45f,
            gpsBearing = 90f,
            isTracking = true,
            error = null
        )

        assertEquals(60f, updatedState.speedKmh, 0.001f)
        assertEquals(-33.4489, updatedState.latitude, 0.0001)
        assertEquals(-70.6693, updatedState.longitude, 0.0001)
        assertEquals(180f, updatedState.heading, 0.001f)
        assertEquals(560.0, updatedState.altitude, 0.0001)
        assertEquals(2.5, updatedState.accuracy, 0.0001)
        assertEquals("fused", updatedState.provider)
        assertEquals(1f, updatedState.accelX, 0.001f)
        assertEquals(2f, updatedState.accelY, 0.001f)
        assertEquals(3f, updatedState.accelZ, 0.001f)
        assertEquals(4f, updatedState.gyroX, 0.001f)
        assertEquals(5f, updatedState.gyroY, 0.001f)
        assertEquals(6f, updatedState.gyroZ, 0.001f)
        assertEquals(7f, updatedState.magX, 0.001f)
        assertEquals(8f, updatedState.magY, 0.001f)
        assertEquals(9f, updatedState.magZ, 0.001f)
        assertEquals(45f, updatedState.compassAzimuth, 0.001f)
        assertEquals(90f, updatedState.gpsBearing, 0.001f)
        assertTrue(updatedState.isTracking)
        assertNull(updatedState.error)
    }

    @Test
    fun stateCopy_errorHandling() {
        val stateWithError = SpeedometerUiState().copy(
            isTracking = false,
            error = "GPS permission denied"
        )

        assertFalse(stateWithError.isTracking)
        assertEquals("GPS permission denied", stateWithError.error)
    }
}

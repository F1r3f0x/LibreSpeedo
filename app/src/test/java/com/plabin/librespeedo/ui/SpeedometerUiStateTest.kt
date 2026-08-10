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

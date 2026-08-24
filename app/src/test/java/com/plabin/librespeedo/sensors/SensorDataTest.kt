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
package com.plabin.librespeedo.sensors

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying [SensorData] instantiation, equality, and default property values.
 */
class SensorDataTest {

    /**
     * Verifies default initialization values for accelerometer, gyroscope, magnetic field, and azimuth.
     */
    @Test
    fun defaultSensorData_hasExpectedDefaults() {
        val data = SensorData()
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), data.accelerometer, 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), data.gyroscope, 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), data.magneticField, 0.001f)
        assertEquals(0f, data.azimuth, 0.001f)
    }

    /**
     * Verifies custom array initialization and property access.
     */
    @Test
    fun sensorData_canBeInitializedWithValues() {
        val accel = floatArrayOf(1f, 2f, 3f)
        val gyro = floatArrayOf(4f, 5f, 6f)
        val mag = floatArrayOf(7f, 8f, 9f)
        val azimuth = 180f

        val data = SensorData(accel, gyro, mag, azimuth)
        assertArrayEquals(accel, data.accelerometer, 0.001f)
        assertArrayEquals(gyro, data.gyroscope, 0.001f)
        assertArrayEquals(mag, data.magneticField, 0.001f)
        assertEquals(azimuth, data.azimuth, 0.001f)
    }

    /**
     * Verifies equality comparisons across all branches of custom equals implementation.
     */
    @Test
    fun sensorData_equals_handlesAllBranches() {
        val base = SensorData(
            accelerometer = floatArrayOf(1f, 2f, 3f),
            gyroscope = floatArrayOf(4f, 5f, 6f),
            magneticField = floatArrayOf(7f, 8f, 9f),
            azimuth = 45f
        )
        val identical = SensorData(
            accelerometer = floatArrayOf(1f, 2f, 3f),
            gyroscope = floatArrayOf(4f, 5f, 6f),
            magneticField = floatArrayOf(7f, 8f, 9f),
            azimuth = 45f
        )
        val diffAccel = base.copy(accelerometer = floatArrayOf(9f, 2f, 3f))
        val diffGyro = base.copy(gyroscope = floatArrayOf(4f, 9f, 6f))
        val diffMag = base.copy(magneticField = floatArrayOf(7f, 8f, 1f))
        val diffAzimuth = base.copy(azimuth = 90f)

        // Reflexive
        org.junit.Assert.assertTrue(base.equals(base))
        // Identical values
        org.junit.Assert.assertTrue(base == identical)
        // Equal hashCodes
        assertEquals(base.hashCode(), identical.hashCode())

        // Non-equal cases
        org.junit.Assert.assertFalse(base == diffAccel)
        org.junit.Assert.assertFalse(base == diffGyro)
        org.junit.Assert.assertFalse(base == diffMag)
        org.junit.Assert.assertFalse(base == diffAzimuth)

        // Null and different class checks
        org.junit.Assert.assertFalse(base.equals(null))
        org.junit.Assert.assertFalse(base.equals("OtherClass"))
    }
}

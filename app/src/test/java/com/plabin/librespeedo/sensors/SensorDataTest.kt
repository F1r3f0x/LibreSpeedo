package com.plabin.librespeedo.sensors

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SensorDataTest {

    @Test
    fun defaultSensorData_hasExpectedDefaults() {
        val data = SensorData()
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), data.accelerometer, 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), data.gyroscope, 0.001f)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f), data.magneticField, 0.001f)
        assertEquals(0f, data.azimuth, 0.001f)
    }

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
}

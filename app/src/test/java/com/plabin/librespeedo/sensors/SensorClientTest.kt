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

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import org.robolectric.shadows.ShadowSensorManager

/**
 * Unit tests verifying hardware sensor registration, event processing, and filtering in [SensorClient].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SensorClientTest {

    private lateinit var context: Context
    private lateinit var sensorManager: SensorManager
    private lateinit var shadowSensorManager: ShadowSensorManager
    private lateinit var sensorClient: SensorClient
    private lateinit var accelSensor: Sensor
    private lateinit var gyroSensor: Sensor
    private lateinit var magSensor: Sensor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shadowSensorManager = shadowOf(sensorManager)

        val shadowAccel = ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER)
        val shadowGyro = ShadowSensor.newInstance(Sensor.TYPE_GYROSCOPE)
        val shadowMag = ShadowSensor.newInstance(Sensor.TYPE_MAGNETIC_FIELD)

        shadowSensorManager.addSensor(shadowAccel)
        shadowSensorManager.addSensor(shadowGyro)
        shadowSensorManager.addSensor(shadowMag)

        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)!!
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!

        sensorClient = SensorClient(context)
    }

    private fun createSensorEvent(sensor: Sensor, values: FloatArray): SensorEvent {
        val constructor = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
        constructor.isAccessible = true
        val event = constructor.newInstance(values.size)
        val sensorField = SensorEvent::class.java.getField("sensor")
        sensorField.isAccessible = true
        sensorField.set(event, sensor)
        System.arraycopy(values, 0, event.values, 0, values.size)
        return event
    }

    @Test
    fun getSensorUpdates_subscribesAndHandlesSensorEvents() = runTest(UnconfinedTestDispatcher()) {
        val emissions = mutableListOf<SensorData>()
        val job = launch {
            sensorClient.getSensorUpdates().collect {
                emissions.add(it)
            }
        }

        val listeners = shadowSensorManager.listeners
        assertNotNull(listeners)

        for (listener in listeners) {
            listener.onAccuracyChanged(accelSensor, 0)
            listener.onSensorChanged(null)

            // Test accelerometer event (flat: Y < Z)
            val accelEventFlat = createSensorEvent(accelSensor, floatArrayOf(0.1f, 1.0f, 9.8f))
            listener.onSensorChanged(accelEventFlat)

            // Test gyroscope event
            val gyroEvent = createSensorEvent(gyroSensor, floatArrayOf(0.01f, 0.02f, 0.03f))
            listener.onSensorChanged(gyroEvent)

            // Test magnetic field event
            val magEvent = createSensorEvent(magSensor, floatArrayOf(20f, 0f, 40f))
            listener.onSensorChanged(magEvent)

            // Test accelerometer event (upright: Y > Z)
            val accelEventUpright = createSensorEvent(accelSensor, floatArrayOf(0.1f, 9.8f, 1.0f))
            listener.onSensorChanged(accelEventUpright)
        }

        assertTrue(emissions.isNotEmpty())
        job.cancel()
    }
}

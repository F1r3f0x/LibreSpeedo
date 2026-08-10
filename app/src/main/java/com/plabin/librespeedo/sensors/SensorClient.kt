package com.plabin.librespeedo.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SensorData(
    val accelerometer: FloatArray = floatArrayOf(0f, 0f, 0f),
    val gyroscope: FloatArray = floatArrayOf(0f, 0f, 0f),
    val magneticField: FloatArray = floatArrayOf(0f, 0f, 0f),
    val azimuth: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData

        if (!accelerometer.contentEquals(other.accelerometer)) return false
        if (!gyroscope.contentEquals(other.gyroscope)) return false
        if (!magneticField.contentEquals(other.magneticField)) return false
        if (azimuth != other.azimuth) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accelerometer.contentHashCode()
        result = 31 * result + gyroscope.contentHashCode()
        result = 31 * result + magneticField.contentHashCode()
        result = 31 * result + azimuth.hashCode()
        return result
    }
}

class SensorClient(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun getSensorUpdates(): Flow<SensorData> = callbackFlow {
        val currentAccel = floatArrayOf(0f, 0f, 0f)
        val currentGyro = floatArrayOf(0f, 0f, 0f)
        val currentMag = floatArrayOf(0f, 0f, 0f)
        var currentAzimuth = 0f
        
        // Smoothing factor for the low-pass filter (0.0 to 1.0)
        // Lower values mean smoother, less sensitive output. This will work for now
        // TODO: Add a message for our current status
        val ALPHA = 0.15f

        fun applyLowPassFilter(input: FloatArray, output: FloatArray) {
            if (output[0] == 0f && output[1] == 0f && output[2] == 0f) {
                input.copyInto(output)
                return
            }
            for (i in input.indices) {
                output[i] = output[i] + ALPHA * (input[i] - output[i])
            }
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                
                var changed = false
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    applyLowPassFilter(event.values, currentAccel)
                    changed = true
                } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    event.values.copyInto(currentGyro)
                    changed = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    applyLowPassFilter(event.values, currentMag)
                    changed = true
                }

                if (changed) {
                    // Calculate azimuth (compass heading)
                    val rotationMatrix = FloatArray(9)
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, currentAccel, currentMag)) {
                        val remappedMatrix = FloatArray(9)
                        // If Y gravity is greater than Z gravity, the phone is held more upright
                        if (Math.abs(currentAccel[1]) > Math.abs(currentAccel[2])) {
                            SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)
                        } else {
                            rotationMatrix.copyInto(remappedMatrix)
                        }

                        val orientationAngles = FloatArray(3)
                        SensorManager.getOrientation(remappedMatrix, orientationAngles)
                        // orientationAngles[0] is azimuth in radians
                        currentAzimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                        if (currentAzimuth < 0) {
                            currentAzimuth += 360f
                        }
                    }
                    trySend(SensorData(currentAccel, currentGyro, currentMag, currentAzimuth))
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Do nothing
            }
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        gyroscope?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        magneticField?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}

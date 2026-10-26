package com.plabin.librespeedo.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

enum class MotionSource { LINEAR_ACCELERATION, ACCELEROMETER_MINUS_GRAVITY }

data class MotionAvailability(
    val linearAcceleration: Boolean,
    val accelerometerFallback: Boolean,
    val rotationVector: Boolean
) {
    val available: Boolean get() = linearAcceleration || accelerometerFallback
}

data class MotionSample(
    val elapsedRealtimeNanos: Long,
    val xMetersPerSecondSquared: Float,
    val yMetersPerSecondSquared: Float,
    val zMetersPerSecondSquared: Float,
    val horizontalMetersPerSecondSquared: Float?,
    val verticalMetersPerSecondSquared: Float?,
    val totalDynamicMetersPerSecondSquared: Float,
    val accuracy: Int,
    val source: MotionSource,
    val orientationValid: Boolean,
    val qualityFlags: Int
)

/** Timestamped driving motion stream, independent of compass snapshots. */
class MotionClient(context: Context) {
    private val appContext = context.applicationContext
    private val manager by lazy { appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    private val linearSensor by lazy { manager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) }
    private val accelSensor by lazy { manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    private val gravitySensor by lazy { manager.getDefaultSensor(Sensor.TYPE_GRAVITY) }
    private val rotationSensor by lazy { manager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }

    fun availability() = MotionAvailability(
        linearAcceleration = linearSensor != null,
        accelerometerFallback = linearSensor == null && accelSensor != null,
        rotationVector = rotationSensor != null
    )

    fun samples(): Flow<MotionSample> = callbackFlow {
        val linear = linearSensor
        val accelerometer = accelSensor
        if (linear == null && accelerometer == null) {
            close()
            return@callbackFlow
        }
        val rotationMatrix = FloatArray(9)
        var rotationNanos = 0L
        var orientationJumpUntilNanos = 0L
        val gravity = floatArrayOf(0f, 0f, SensorManager.GRAVITY_EARTH)
        var gravityNanos = 0L
        val estimatedGravity = floatArrayOf(0f, 0f, SensorManager.GRAVITY_EARTH)
        val filtered = FloatArray(3)
        var hasFiltered = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        val nextMatrix = FloatArray(9)
                        SensorManager.getRotationMatrixFromVector(nextMatrix, event.values)
                        if (rotationNanos > 0L && event.timestamp - rotationNanos in 1L..100_000_000L) {
                            val dot = rotationMatrix.indices.sumOf { index -> (rotationMatrix[index] * nextMatrix[index]).toDouble() }
                            val angle = Math.toDegrees(acos(((dot - 1.0) / 2.0).coerceIn(-1.0, 1.0)))
                            if (angle > 45.0) orientationJumpUntilNanos = event.timestamp + 500_000_000L
                        }
                        nextMatrix.copyInto(rotationMatrix)
                        rotationNanos = event.timestamp
                    }
                    Sensor.TYPE_GRAVITY -> {
                        event.values.copyInto(gravity)
                        gravityNanos = event.timestamp
                    }
                    Sensor.TYPE_ACCELEROMETER -> if (linear == null) {
                        val dynamic = FloatArray(3)
                        for (axis in 0..2) {
                            estimatedGravity[axis] = 0.9f * estimatedGravity[axis] + 0.1f * event.values[axis]
                            val reference = if (gravityNanos > 0L && abs(event.timestamp - gravityNanos) < 250_000_000L) gravity[axis] else estimatedGravity[axis]
                            dynamic[axis] = event.values[axis] - reference
                        }
                        emit(event.timestamp, dynamic, event.accuracy, MotionSource.ACCELEROMETER_MINUS_GRAVITY)
                    }
                    Sensor.TYPE_LINEAR_ACCELERATION -> if (linear != null) {
                        emit(event.timestamp, event.values, event.accuracy, MotionSource.LINEAR_ACCELERATION)
                    }
                }
            }

            private fun emit(timestamp: Long, vector: FloatArray, accuracy: Int, source: MotionSource) {
                val dt = if (hasFiltered) ((timestamp - lastTimestamp).coerceAtLeast(1L) / 1e9).toFloat() else 0f
                val alpha = if (hasFiltered) (dt / (0.20f + dt)).coerceIn(0.02f, 1f) else 1f
                for (axis in 0..2) filtered[axis] = if (hasFiltered) filtered[axis] + alpha * (vector[axis] - filtered[axis]) else vector[axis]
                hasFiltered = true
                lastTimestamp = timestamp
                val orientationFresh = rotationNanos > 0L && abs(timestamp - rotationNanos) <= 100_000_000L
                val x: Float
                val y: Float
                val z: Float
                if (orientationFresh) {
                    x = rotationMatrix[0] * filtered[0] + rotationMatrix[1] * filtered[1] + rotationMatrix[2] * filtered[2]
                    y = rotationMatrix[3] * filtered[0] + rotationMatrix[4] * filtered[1] + rotationMatrix[5] * filtered[2]
                    z = rotationMatrix[6] * filtered[0] + rotationMatrix[7] * filtered[1] + rotationMatrix[8] * filtered[2]
                } else {
                    x = filtered[0]; y = filtered[1]; z = filtered[2]
                }
                val total = sqrt(x * x + y * y + z * z)
                val clipped = (0..2).any { abs(vector[it]) >= 39.2f }
                val flags = (if (!orientationFresh) FLAG_ORIENTATION_UNAVAILABLE else 0) or
                    (if (clipped) FLAG_CLIPPED else 0) or
                    (if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) FLAG_UNRELIABLE else 0) or
                    (if (timestamp <= orientationJumpUntilNanos) FLAG_ORIENTATION_JUMP else 0)
                val horizontal = if (orientationFresh) hypot(x, y) else null
                trySend(
                    MotionSample(
                        elapsedRealtimeNanos = timestamp,
                        xMetersPerSecondSquared = x,
                        yMetersPerSecondSquared = y,
                        zMetersPerSecondSquared = z,
                        horizontalMetersPerSecondSquared = horizontal,
                        verticalMetersPerSecondSquared = if (orientationFresh) z else null,
                        totalDynamicMetersPerSecondSquared = total,
                        accuracy = accuracy,
                        source = source,
                        orientationValid = orientationFresh,
                        qualityFlags = flags
                    )
                )
            }

            private var lastTimestamp = 0L
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val periodUs = 20_000
        rotationSensor?.let { manager.registerListener(listener, it, periodUs) }
        if (linear != null) manager.registerListener(listener, linear, periodUs)
        else {
            gravitySensor?.let { manager.registerListener(listener, it, periodUs) }
            accelerometer?.let { manager.registerListener(listener, it, periodUs) }
        }
        awaitClose { manager.unregisterListener(listener) }
    }

    companion object {
        const val FLAG_ORIENTATION_UNAVAILABLE = 1
        const val FLAG_CLIPPED = 1 shl 1
        const val FLAG_UNRELIABLE = 1 shl 2
        const val FLAG_ORIENTATION_JUMP = 1 shl 3
    }
}

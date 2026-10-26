package com.plabin.librespeedo.trips

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

const val STANDARD_GRAVITY = 9.80665

data class MotionObservation(
    val elapsedRealtimeNanos: Long,
    val horizontalAccelerationMetersPerSecondSquared: Double?,
    val verticalAccelerationMetersPerSecondSquared: Double?,
    val totalDynamicAccelerationMetersPerSecondSquared: Double,
    val forwardAccelerationMetersPerSecondSquared: Double? = null,
    val lateralAccelerationMetersPerSecondSquared: Double? = null,
    val accuracyReliable: Boolean,
    val qualityFlags: Int,
    val moving: Boolean?
)

data class MotionMetrics(
    val maximumHorizontalG: Double? = null,
    val averageHorizontalG: Double? = null,
    val movingAverageHorizontalG: Double? = null,
    val maximumVerticalG: Double? = null,
    val maximumTotalDynamicG: Double? = null,
    val rawDiagnosticPeakG: Double? = null,
    val horizontalGIntegralSeconds: Double = 0.0,
    val movingHorizontalGIntegralSeconds: Double = 0.0,
    val validGDurationNanos: Long = 0L,
    val movingValidGDurationNanos: Long = 0L,
    val totalDynamicGDurationNanos: Long = 0L,
    val rmsHorizontalG: Double? = null,
    val p95HorizontalG: Double? = null,
    val exposureAbove015GSeconds: Double = 0.0,
    val exposureAbove025GSeconds: Double = 0.0,
    val maximumForwardG: Double? = null,
    val maximumBrakingG: Double? = null,
    val maximumLateralG: Double? = null,
    val maximumJerkGPerSecond: Double? = null
)

/** Time-weighted, timestamp-gated G-force metrics. Axes are never averaged with signs. */
class MotionMetricsAccumulator(private val maxGapNanos: Long = 250_000_000L) {
    private var previous: MotionObservation? = null
    private var previousFilteredHorizontalG: Double? = null
    private var previousMoving: Boolean? = null
    private var filteredHorizontalG: Double? = null
    private var filteredVerticalG: Double? = null
    private var filteredTotalG: Double? = null
    private var maxHorizontal: Double? = null
    private var maxVertical: Double? = null
    private var maxTotal: Double? = null
    private var rawPeak: Double? = null
    private var integral = 0.0
    private var movingIntegral = 0.0
    private var validDuration = 0L
    private var movingDuration = 0L
    private var totalDuration = 0L
    private var horizontalSquaredIntegral = 0.0
    private var exposure015 = 0.0
    private var exposure025 = 0.0
    private val horizontalHistogram = IntArray(501)
    private var horizontalCount = 0
    private var filteredForward: Double? = null
    private var filteredLateral: Double? = null
    private var previousForward: Double? = null
    private var previousLateral: Double? = null
    private var maximumForward: Double? = null
    private var maximumBraking: Double? = null
    private var maximumLateral: Double? = null
    private var maximumJerk: Double? = null

    fun add(observation: MotionObservation): Boolean {
        val old = previous
        if (old != null && observation.elapsedRealtimeNanos <= old.elapsedRealtimeNanos) return false
        val rawTotal = observation.totalDynamicAccelerationMetersPerSecondSquared / STANDARD_GRAVITY
        if (rawTotal.isFinite()) rawPeak = max(rawPeak ?: 0.0, rawTotal)
        val dtNanos = old?.let { observation.elapsedRealtimeNanos - it.elapsedRealtimeNanos } ?: 0L
        val dt = dtNanos / 1e9
        val alpha = if (dt > 0) dt / (0.25 + dt) else 1.0
        val horizontal = observation.horizontalAccelerationMetersPerSecondSquared
            ?.takeIf { it.isFinite() && observation.accuracyReliable && observation.qualityFlags == 0 }
            ?.let { hypot(it, 0.0) / STANDARD_GRAVITY }
        val vertical = observation.verticalAccelerationMetersPerSecondSquared
            ?.takeIf { it.isFinite() && observation.accuracyReliable && observation.qualityFlags == 0 }
            ?.let { abs(it) / STANDARD_GRAVITY }
        val total = rawTotal.takeIf { it.isFinite() && observation.accuracyReliable && observation.qualityFlags and QUALITY_CLIPPED == 0 }
        val forward = observation.forwardAccelerationMetersPerSecondSquared
            ?.takeIf { it.isFinite() && observation.accuracyReliable && observation.qualityFlags == 0 }
            ?.div(STANDARD_GRAVITY)
        val lateral = observation.lateralAccelerationMetersPerSecondSquared
            ?.takeIf { it.isFinite() && observation.accuracyReliable && observation.qualityFlags == 0 }
            ?.div(STANDARD_GRAVITY)
        filteredHorizontalG = smooth(filteredHorizontalG, horizontal, alpha)
        filteredVerticalG = smooth(filteredVerticalG, vertical, alpha)
        filteredTotalG = smooth(filteredTotalG, total, alpha)
        filteredForward = smooth(filteredForward, forward, alpha)
        filteredLateral = smooth(filteredLateral, lateral, alpha)
        filteredHorizontalG?.let { maxHorizontal = max(maxHorizontal ?: 0.0, it) }
        filteredVerticalG?.let { maxVertical = max(maxVertical ?: 0.0, it) }
        filteredTotalG?.let { maxTotal = max(maxTotal ?: 0.0, it) }
        filteredForward?.let {
            if (forward != null) maximumForward = max(maximumForward ?: 0.0, it)
            if (forward != null && it < 0) maximumBraking = max(maximumBraking ?: 0.0, abs(it))
        }
        filteredLateral?.let { if (lateral != null) maximumLateral = max(maximumLateral ?: 0.0, abs(it)) }
        if (dtNanos in 1..maxGapNanos && filteredForward != null && filteredLateral != null && previousForward != null && previousLateral != null) {
            val jerk = hypot(filteredForward!! - previousForward!!, filteredLateral!! - previousLateral!!) / dt
            if (jerk.isFinite()) maximumJerk = max(maximumJerk ?: 0.0, jerk)
        }

        val acceptedFilteredHorizontal = filteredHorizontalG.takeIf { horizontal != null }
        if (old != null && dtNanos in 1..maxGapNanos) {
            val previousMagnitude = previousFilteredHorizontalG
            if (acceptedFilteredHorizontal != null && previousMagnitude != null && old.accuracyReliable) {
                integral += (previousMagnitude + acceptedFilteredHorizontal) * 0.5 * dt
                horizontalSquaredIntegral += (previousMagnitude * previousMagnitude + acceptedFilteredHorizontal * acceptedFilteredHorizontal) * 0.5 * dt
                validDuration += dtNanos
                val meanG = (previousMagnitude + acceptedFilteredHorizontal) * 0.5
                if (meanG > 0.15) exposure015 += dt
                if (meanG > 0.25) exposure025 += dt
                val bin = (acceptedFilteredHorizontal * 100).toInt().coerceIn(0, horizontalHistogram.lastIndex)
                horizontalHistogram[bin]++
                horizontalCount++
                if (observation.moving == true && previousMoving == true) {
                    movingIntegral += (previousMagnitude + acceptedFilteredHorizontal) * 0.5 * dt
                    movingDuration += dtNanos
                }
            }
            if (total != null && old.totalDynamicAccelerationMetersPerSecondSquared.isFinite()) totalDuration += dtNanos
        }
        previousFilteredHorizontalG = acceptedFilteredHorizontal
        previousMoving = observation.moving
        previousForward = filteredForward.takeIf { forward != null }
        previousLateral = filteredLateral.takeIf { lateral != null }
        previous = observation
        return true
    }

    fun snapshot(): MotionMetrics {
        var p95: Double? = null
        if (horizontalCount > 0) {
            val target = (horizontalCount * 0.95).toInt().coerceAtLeast(1)
            var sum = 0
            for (index in horizontalHistogram.indices) {
                sum += horizontalHistogram[index]
                if (sum >= target) { p95 = index / 100.0; break }
            }
        }
        return MotionMetrics(
        maximumHorizontalG = maxHorizontal,
        averageHorizontalG = if (validDuration > 0L) integral / (validDuration / 1e9) else null,
        movingAverageHorizontalG = if (movingDuration > 0L) movingIntegral / (movingDuration / 1e9) else null,
        maximumVerticalG = maxVertical,
        maximumTotalDynamicG = maxTotal,
        rawDiagnosticPeakG = rawPeak,
        horizontalGIntegralSeconds = integral,
        movingHorizontalGIntegralSeconds = movingIntegral,
        validGDurationNanos = validDuration,
        movingValidGDurationNanos = movingDuration,
        totalDynamicGDurationNanos = totalDuration,
        rmsHorizontalG = if (validDuration > 0L) sqrt(horizontalSquaredIntegral / (validDuration / 1e9)) else null,
        p95HorizontalG = p95,
        exposureAbove015GSeconds = exposure015,
        exposureAbove025GSeconds = exposure025,
        maximumForwardG = maximumForward,
        maximumBrakingG = maximumBraking,
        maximumLateralG = maximumLateral,
        maximumJerkGPerSecond = maximumJerk
        )
    }

    private fun smooth(previous: Double?, current: Double?, alpha: Double): Double? = when {
        current == null -> previous
        previous == null -> current
        else -> previous + alpha * (current - previous)
    }

    companion object {
        const val QUALITY_CLIPPED = 1 shl 1
    }
}

package com.plabin.librespeedo.trips

import kotlin.math.abs
import kotlin.math.max

enum class DrivingEventType { HARD_ACCELERATION, HARSH_BRAKING, HARD_CORNERING, HIGH_JERK }

data class DrivingEventFeature(
    val elapsedRealtimeNanos: Long,
    val forwardG: Double?,
    val lateralG: Double?,
    val speedMetersPerSecond: Double?,
    val confidence: Double,
    val eligible: Boolean
)

data class DetectedDrivingEvent(
    val type: DrivingEventType,
    val startElapsedRealtimeNanos: Long,
    val endElapsedRealtimeNanos: Long,
    val peakValue: Double,
    val speedContextMetersPerSecond: Double?,
    val confidence: Double,
    val detectorVersion: Int = 1
) {
    val durationNanos get() = (endElapsedRealtimeNanos - startElapsedRealtimeNanos).coerceAtLeast(0L)
}

data class DrivingDetectionConfig(
    val accelerationEntryG: Double = 0.25,
    val accelerationExitG: Double = 0.16,
    val brakingEntryG: Double = 0.25,
    val brakingExitG: Double = 0.16,
    val corneringEntryG: Double = 0.25,
    val corneringExitG: Double = 0.16,
    val jerkEntryGPerSecond: Double = 2.0,
    val jerkExitGPerSecond: Double = 1.25,
    val minimumDurationNanos: Long = 600_000_000L,
    val jerkMinimumDurationNanos: Long = 160_000_000L,
    val cooldownNanos: Long = 4_000_000_000L
)

/** Sustained, quality-gated maneuver detector. It reports evidence and does not score trips. */
class DrivingEventDetector(private val config: DrivingDetectionConfig = DrivingDetectionConfig()) {
    private data class Active(
        val type: DrivingEventType,
        val start: Long,
        var lastAbove: Long,
        var peak: Double,
        var confidenceSum: Double,
        var confidenceCount: Int,
        var speedSum: Double,
        var speedCount: Int
    )

    private val active = mutableMapOf<DrivingEventType, Active>()
    private val cooldownUntil = mutableMapOf<DrivingEventType, Long>()
    private var prior: DrivingEventFeature? = null

    fun add(feature: DrivingEventFeature): List<DetectedDrivingEvent> {
        val jerk = prior?.let { old ->
            val dt = (feature.elapsedRealtimeNanos - old.elapsedRealtimeNanos) / 1e9
            if (dt > 0.0 && dt <= 0.25 && feature.eligible && old.eligible && feature.forwardG != null && old.forwardG != null && feature.lateralG != null && old.lateralG != null) {
                kotlin.math.hypot(feature.forwardG - old.forwardG, feature.lateralG - old.lateralG) / dt
            } else null
        }
        val values = mapOf(
            DrivingEventType.HARD_ACCELERATION to feature.forwardG?.coerceAtLeast(0.0),
            DrivingEventType.HARSH_BRAKING to feature.forwardG?.let { (-it).coerceAtLeast(0.0) },
            DrivingEventType.HARD_CORNERING to feature.lateralG?.let(::abs),
            DrivingEventType.HIGH_JERK to jerk
        )
        val output = mutableListOf<DetectedDrivingEvent>()
        values.forEach { (type, value) ->
            val activeEvent = active[type]
            val (entry, exit) = thresholds(type)
            if (!feature.eligible || value == null) {
                if (activeEvent != null && feature.elapsedRealtimeNanos - activeEvent.lastAbove > 250_000_000L) {
                    finalize(type, activeEvent.lastAbove, output)
                }
            } else if (value >= entry) {
                val event = activeEvent ?: if (feature.elapsedRealtimeNanos >= (cooldownUntil[type] ?: 0L)) {
                    Active(type, feature.elapsedRealtimeNanos, feature.elapsedRealtimeNanos, value, 0.0, 0, 0.0, 0)
                        .also { active[type] = it }
                } else null
                event?.let {
                    it.lastAbove = feature.elapsedRealtimeNanos
                    it.peak = max(it.peak, value)
                    it.confidenceSum += feature.confidence
                    it.confidenceCount++
                    feature.speedMetersPerSecond?.let { speed -> it.speedSum += speed; it.speedCount++ }
                }
            } else if (activeEvent != null) {
                if (value >= exit) {
                    activeEvent.lastAbove = feature.elapsedRealtimeNanos
                } else if (feature.elapsedRealtimeNanos - activeEvent.lastAbove > 250_000_000L) {
                    finalize(type, activeEvent.lastAbove, output)
                }
            }
        }
        prior = feature
        return output
    }

    fun flush(atElapsedRealtimeNanos: Long): List<DetectedDrivingEvent> = buildList {
        active.toMap().forEach { (type, event) -> finalize(type, minOf(event.lastAbove, atElapsedRealtimeNanos), this) }
    }

    private fun finalize(type: DrivingEventType, end: Long, output: MutableList<DetectedDrivingEvent>) {
        val event = active.remove(type) ?: return
        val duration = (end - event.start).coerceAtLeast(0L)
        val minimum = if (type == DrivingEventType.HIGH_JERK) config.jerkMinimumDurationNanos else config.minimumDurationNanos
        if (duration >= minimum && event.confidenceCount > 0) {
            output += DetectedDrivingEvent(
                type = type,
                startElapsedRealtimeNanos = event.start,
                endElapsedRealtimeNanos = end,
                peakValue = event.peak,
                speedContextMetersPerSecond = event.speedSum.takeIf { event.speedCount > 0 }?.div(event.speedCount),
                confidence = (event.confidenceSum / event.confidenceCount).coerceIn(0.0, 1.0)
            )
            cooldownUntil[type] = end + config.cooldownNanos
        }
    }

    private fun thresholds(type: DrivingEventType): Pair<Double, Double> = when (type) {
        DrivingEventType.HARD_ACCELERATION -> config.accelerationEntryG to config.accelerationExitG
        DrivingEventType.HARSH_BRAKING -> config.brakingEntryG to config.brakingExitG
        DrivingEventType.HARD_CORNERING -> config.corneringEntryG to config.corneringExitG
        DrivingEventType.HIGH_JERK -> config.jerkEntryGPerSecond to config.jerkExitGPerSecond
    }

    companion object { const val VERSION = 1 }
}

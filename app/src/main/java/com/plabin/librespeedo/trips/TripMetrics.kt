package com.plabin.librespeedo.trips

import kotlin.math.max

enum class MovementState { MOVING, STOPPED, UNKNOWN }
enum class TripStatus { RECORDING, FINISHED, INTERRUPTED }
enum class TripEndReason { MANUAL, DISCONNECT_GRACE, AUTOMATION_DISABLED, BACKGROUND, PERMISSION_LOST, RECOVERED }

/** Timestamped, immutable input to trip calculations. Times use Android elapsed realtime. */
data class SpeedObservation(
    val elapsedRealtimeNanos: Long,
    val wallTimeUtcMillis: Long,
    val speedMetersPerSecond: Double?,
    val accuracyMetersPerSecond: Double? = null,
    val accepted: Boolean = true,
    val provider: String? = null,
    val bearingDegrees: Double? = null
)

data class TripMetrics(
    val topSpeedMetersPerSecond: Double? = null,
    val distanceMeters: Double = 0.0,
    val speedIntegralMeters: Double = 0.0,
    val speedCoveredNanos: Long = 0L,
    val movingNanos: Long = 0L,
    val stoppedNanos: Long = 0L,
    val unknownNanos: Long = 0L,
    val totalNanos: Long = 0L,
    val speedCoverage: Double = 0.0,
    val movementCoverage: Double = 0.0
) {
    val averageSpeedMetersPerSecond: Double?
        get() = speedIntegralMeters.takeIf { speedCoveredNanos > 0L }?.div(speedCoveredNanos / 1e9)
}

/** Pure, replayable accumulator. Gaps and rejected points are never integrated. */
class TripMetricsAccumulator(private val maxSpeedGapNanos: Long = 3_000_000_000L) {
    private var previous: SpeedObservation? = null
    private var speedSum = 0.0
    private var distance = 0.0
    private var covered = 0L
    private var top: Double? = null
    private var startNanos: Long? = null
    private var lastNanos: Long? = null
    private var lastObservedNanos: Long? = null
    private var moving = 0L
    private var stopped = 0L
    private var unknown = 0L
    var lastObservationAccepted: Boolean = false
        private set
    val latestTimestampNanos: Long? get() = lastObservedNanos
    val currentMovementState: MovementState get() = movement
    private var movement = MovementState.UNKNOWN
    private var pendingMovement: MovementState? = null
    private var pendingMovementNanos = 0L
    private var ambiguousNanos = 0L

    fun start(atElapsedRealtimeNanos: Long) {
        if (startNanos == null) startNanos = atElapsedRealtimeNanos
        lastObservedNanos = atElapsedRealtimeNanos
        lastNanos = atElapsedRealtimeNanos
    }

    /** Closes the final interval as unknown when no fresh location covered it. */
    fun finish(atElapsedRealtimeNanos: Long) {
        val last = lastObservedNanos ?: startNanos ?: return
        if (atElapsedRealtimeNanos > last) {
            unknown += atElapsedRealtimeNanos - last
            lastObservedNanos = atElapsedRealtimeNanos
            lastNanos = atElapsedRealtimeNanos
        }
    }

    fun add(observation: SpeedObservation): Boolean {
        val priorTimestamp = lastObservedNanos
        if (priorTimestamp != null && observation.elapsedRealtimeNanos <= priorTimestamp) return false
        val speed = observation.speedMetersPerSecond
        val p = previous
        val isSpike = speed != null && speed.isFinite() && speed >= 0.0 && observation.accepted &&
            p?.speedMetersPerSecond?.let { priorSpeed ->
                val dt = (observation.elapsedRealtimeNanos - p.elapsedRealtimeNanos).coerceAtLeast(1L) / 1e9
                dt <= 2.0 && kotlin.math.abs(speed - priorSpeed) > 25.0 && speed > 35.0
            } == true
        lastObservationAccepted = observation.accepted && !isSpike
        if (speed != null && speed.isFinite() && speed >= 0.0 && lastObservationAccepted) top = max(top ?: 0.0, speed)
        val priorTime = lastObservedNanos
        if (priorTime != null && observation.elapsedRealtimeNanos > priorTime) {
            val dt = observation.elapsedRealtimeNanos - priorTime
            if (dt <= maxSpeedGapNanos && p != null && p.accepted && lastObservationAccepted &&
                p.speedMetersPerSecond != null && speed != null &&
                p.speedMetersPerSecond.isFinite() && speed.isFinite() && p.speedMetersPerSecond >= 0 && speed >= 0
            ) {
                val intervalDistance = (p.speedMetersPerSecond + speed) * 0.5 * dt / 1e9
                distance += intervalDistance
                speedSum += intervalDistance
                covered += dt
            }
            val start = startNanos ?: priorTime.also { startNanos = it }
            val intervalStart = max(priorTime, start)
            val interval = (observation.elapsedRealtimeNanos - intervalStart).coerceAtLeast(0L)
            if (p == null || dt > maxSpeedGapNanos || speed == null || !lastObservationAccepted) {
                unknown += interval
                movement = MovementState.UNKNOWN
                pendingMovement = null
                pendingMovementNanos = 0L
                ambiguousNanos = 0L
            } else {
                val candidate = when {
                    speed < (1.0 / 3.6) -> MovementState.STOPPED
                    speed > (3.0 / 3.6) -> MovementState.MOVING
                    else -> null
                }
                when (candidate) {
                    movement -> {
                        addMovementDuration(movement, interval)
                        pendingMovement = null
                        pendingMovementNanos = 0L
                        ambiguousNanos = 0L
                    }
                    null -> {
                        if (movement != MovementState.UNKNOWN && ambiguousNanos + interval <= 3_000_000_000L) {
                            addMovementDuration(movement, interval)
                            ambiguousNanos += interval
                        } else {
                            unknown += interval
                            movement = MovementState.UNKNOWN
                            ambiguousNanos = 0L
                        }
                    }
                    else -> {
                        unknown += interval
                        if (pendingMovement != candidate) {
                            pendingMovement = candidate
                            pendingMovementNanos = 0L
                        }
                        pendingMovementNanos += interval
                        val required = if (candidate == MovementState.STOPPED) 5_000_000_000L else 2_000_000_000L
                        if (pendingMovementNanos >= required) {
                            unknown -= pendingMovementNanos
                            addMovementDuration(candidate, pendingMovementNanos)
                            movement = candidate
                            pendingMovement = null
                            pendingMovementNanos = 0L
                            ambiguousNanos = 0L
                        }
                    }
                }
            }
            lastNanos = observation.elapsedRealtimeNanos
        }
        if (startNanos == null) startNanos = observation.elapsedRealtimeNanos
        if (priorTime == null || observation.elapsedRealtimeNanos > priorTime) {
            previous = observation.copy(accepted = lastObservationAccepted)
            lastObservedNanos = observation.elapsedRealtimeNanos
        }
        return true
    }

    private fun addMovementDuration(state: MovementState, duration: Long) {
        when (state) {
            MovementState.MOVING -> moving += duration
            MovementState.STOPPED -> stopped += duration
            MovementState.UNKNOWN -> unknown += duration
        }
    }

    fun snapshot(): TripMetrics {
        val total = ((lastNanos ?: startNanos ?: 0L) - (startNanos ?: 0L)).coerceAtLeast(0L)
        return TripMetrics(
            topSpeedMetersPerSecond = top, distanceMeters = distance, speedIntegralMeters = speedSum,
            speedCoveredNanos = covered, movingNanos = moving, stoppedNanos = stopped,
            unknownNanos = unknown, totalNanos = total,
            speedCoverage = if (total == 0L) 0.0 else (covered.toDouble() / total).coerceIn(0.0, 1.0),
            movementCoverage = if (total == 0L) 0.0 else ((moving + stopped).toDouble() / total).coerceIn(0.0, 1.0)
        )
    }
}

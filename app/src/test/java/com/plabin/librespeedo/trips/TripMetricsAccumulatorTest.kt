package com.plabin.librespeedo.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMetricsAccumulatorTest {
    @Test
    fun integratesUnequalSpacingAndBackdatesConfirmedMovementChanges() {
        val metrics = TripMetricsAccumulator().apply {
            start(0L)
            add(sample(1, 10.0))
            add(sample(2, 10.0))
            add(sample(3, 10.0))
            add(sample(4, 0.0))
            add(sample(5, 0.0))
            add(sample(6, 0.0))
            add(sample(7, 0.0))
            add(sample(8, 0.0))
        }.snapshot()

        assertEquals(25.0, metrics.distanceMeters, 1e-9)
        assertEquals(25.0 / 7.0, metrics.averageSpeedMetersPerSecond!!, 1e-9)
        assertEquals(10.0, metrics.topSpeedMetersPerSecond!!, 1e-9)
        assertEquals(7_000_000_000L, metrics.speedCoveredNanos)
        assertEquals(2_000_000_000L, metrics.movingNanos)
        assertEquals(5_000_000_000L, metrics.stoppedNanos)
        assertEquals(1_000_000_000L, metrics.unknownNanos)
        assertEquals(metrics.totalNanos, metrics.movingNanos + metrics.stoppedNanos + metrics.unknownNanos)
    }

    @Test
    fun missingFixesBecomeUnknownAndDoNotBridgeDistanceOrSpeed() {
        val accumulator = TripMetricsAccumulator().apply {
            start(0L)
            add(sample(1, 5.0))
            add(sample(2, 5.0))
            add(sample(8, null, accepted = false))
        }
        val metrics = accumulator.snapshot()
        assertEquals(1_000_000_000L, metrics.speedCoveredNanos)
        assertEquals(5.0, metrics.distanceMeters, 0.0)
        assertEquals(8_000_000_000L, metrics.unknownNanos)
        assertEquals(8_000_000_000L, metrics.totalNanos)
        assertEquals(0.0, metrics.movementCoverage, 0.0)
    }

    @Test
    fun rejectsAnIsolatedSpeedSpikeAndOutOfOrderInput() {
        val accumulator = TripMetricsAccumulator().apply {
            start(0L)
            add(sample(1, 10.0))
            add(sample(2, 45.0))
            assertFalse(add(sample(2, 11.0)))
            add(sample(3, 10.0))
            add(sample(4, 10.0))
        }
        val metrics = accumulator.snapshot()
        assertEquals(10.0, metrics.topSpeedMetersPerSecond!!, 1e-9)
        assertEquals(10.0, metrics.distanceMeters, 1e-9)
        assertEquals(1_000_000_000L, metrics.speedCoveredNanos)
        assertTrue(accumulator.lastObservationAccepted)
    }

    @Test
    fun finishCountsUncoveredTailAsUnknownAndNullWhenNoSpeedCoverage() {
        val accumulator = TripMetricsAccumulator().apply {
            start(10L)
            finish(2_000_000_010L)
        }
        val metrics = accumulator.snapshot()
        assertEquals(2_000_000_000L, metrics.unknownNanos)
        assertEquals(2_000_000_000L, metrics.totalNanos)
        assertNull(metrics.averageSpeedMetersPerSecond)
        assertTrue(metrics.speedCoverage == 0.0)
    }

    private fun sample(seconds: Long, speed: Double?, accepted: Boolean = true) = SpeedObservation(
        elapsedRealtimeNanos = seconds * 1_000_000_000L,
        wallTimeUtcMillis = seconds * 1000L,
        speedMetersPerSecond = speed,
        accepted = accepted
    )
}

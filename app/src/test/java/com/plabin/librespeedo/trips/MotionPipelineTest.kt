package com.plabin.librespeedo.trips

import com.plabin.librespeedo.sensors.MotionSample
import com.plabin.librespeedo.sensors.MotionSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionPipelineTest {
    @Test
    fun stationaryPhoneHasZeroDynamicGNotOneGravity() {
        val accumulator = MotionMetricsAccumulator()
        listOf(0L, 20_000_000L, 40_000_000L).forEach { time ->
            assertTrue(accumulator.add(motion(time, horizontal = 0.0, vertical = 0.0, total = 0.0)))
        }
        val metrics = accumulator.snapshot()
        assertEquals(0.0, metrics.maximumHorizontalG!!, 1e-12)
        assertEquals(0.0, metrics.averageHorizontalG!!, 1e-12)
        assertEquals(0.0, metrics.maximumTotalDynamicG!!, 1e-12)
        assertEquals(0.0, metrics.rawDiagnosticPeakG!!, 1e-12)
    }

    @Test
    fun orientationUnavailableLeavesHorizontalMetricsNull() {
        val accumulator = MotionMetricsAccumulator()
        accumulator.add(motion(0L, horizontal = null, vertical = null, total = 0.1, flags = 1))
        accumulator.add(motion(20_000_000L, horizontal = null, vertical = null, total = 0.1, flags = 1))
        assertNull(accumulator.snapshot().maximumHorizontalG)
        assertNull(accumulator.snapshot().averageHorizontalG)
        assertEquals(0.1 / STANDARD_GRAVITY, accumulator.snapshot().maximumTotalDynamicG!!, 1e-5)
    }

    @Test
    fun outOfOrderMotionIsRejectedAndGapsDoNotBridgeIntegrals() {
        val accumulator = MotionMetricsAccumulator()
        assertTrue(accumulator.add(motion(0L, horizontal = 1.0, vertical = 0.0, total = 1.0)))
        assertFalse(accumulator.add(motion(0L, horizontal = 5.0, vertical = 0.0, total = 5.0)))
        assertTrue(accumulator.add(motion(300_000_000L, horizontal = 3.0, vertical = 0.0, total = 3.0)))
        assertEquals(0L, accumulator.snapshot().validGDurationNanos)
        assertEquals(1.0 + 2.0 * (0.3 / 0.55), accumulator.snapshot().maximumHorizontalG!! * STANDARD_GRAVITY, 1e-6)
    }

    @Test
    fun sustainedHighConfidenceAccelerationProducesOneManeuverEvent() {
        val detector = DrivingEventDetector()
        val output = mutableListOf<DetectedDrivingEvent>()
        for (tick in 0..10) {
            output += detector.add(feature(tick * 100_000_000L, forwardG = 0.3))
        }
        for (tick in 11..14) {
            output += detector.add(feature(tick * 100_000_000L, forwardG = 0.0))
        }
        assertEquals(1, output.count { it.type == DrivingEventType.HARD_ACCELERATION })
        val event = output.single { it.type == DrivingEventType.HARD_ACCELERATION }
        assertTrue(event.durationNanos >= 600_000_000L)
        assertEquals(0.3, event.peakValue, 1e-9)
        assertEquals(0.9, event.confidence, 1e-9)
    }

    @Test
    fun shortAccelerationDoesNotBecomeDrivingEvent() {
        val detector = DrivingEventDetector()
        val output = mutableListOf<DetectedDrivingEvent>()
        for (tick in 0..3) output += detector.add(feature(tick * 100_000_000L, 0.35))
        for (tick in 4..8) output += detector.add(feature(tick * 100_000_000L, 0.0))
        output += detector.flush(900_000_000L)
        assertTrue(output.none { it.type == DrivingEventType.HARD_ACCELERATION })
    }

    @Test
    fun compressedMotionChunksRoundTripAllChannels() {
        val original = listOf(
            MotionSample(100L, 1f, 2f, 3f, 2.2f, 3f, 3.7f, 3, MotionSource.LINEAR_ACCELERATION, true, 0),
            MotionSample(120L, -1f, -2f, -3f, null, null, 3.1f, 2, MotionSource.ACCELEROMETER_MINUS_GRAVITY, false, 1)
        )
        val decoded = MotionChunkCodec.decode(MotionChunkCodec.encode(original))
        assertEquals(original, decoded)
    }

    private fun motion(
        elapsedNanos: Long,
        horizontal: Double?,
        vertical: Double?,
        total: Double,
        flags: Int = 0
    ) = MotionObservation(
        elapsedRealtimeNanos = elapsedNanos,
        horizontalAccelerationMetersPerSecondSquared = horizontal,
        verticalAccelerationMetersPerSecondSquared = vertical,
        totalDynamicAccelerationMetersPerSecondSquared = total,
        accuracyReliable = true,
        qualityFlags = flags,
        moving = true
    )

    private fun feature(time: Long, forwardG: Double) = DrivingEventFeature(
        elapsedRealtimeNanos = time,
        forwardG = forwardG,
        lateralG = 0.0,
        speedMetersPerSecond = 15.0,
        confidence = 0.9,
        eligible = true
    )
}

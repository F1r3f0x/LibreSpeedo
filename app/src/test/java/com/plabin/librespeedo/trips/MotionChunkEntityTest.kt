package com.plabin.librespeedo.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MotionChunkEntityTest {
    @Test
    fun equalPayloadsDeduplicateInHashCollections() {
        val chunk = MotionChunkEntity(
            tripId = "trip",
            startElapsedRealtimeNanos = 0L,
            endElapsedRealtimeNanos = 20_000_000L,
            sampleCount = 2,
            source = "LINEAR_ACCELERATION",
            compressedSamples = byteArrayOf(1, 2, 3)
        )
        val restored = chunk.copy(compressedSamples = chunk.compressedSamples.copyOf())

        assertEquals(chunk, restored)
        assertEquals(chunk.hashCode(), restored.hashCode())
        assertEquals(1, setOf(chunk, restored).size)
        assertNotEquals(chunk, chunk.copy(compressedSamples = byteArrayOf(1, 2, 4)))
        assertNotEquals(chunk, chunk.copy(tripId = "another-trip"))
        assertNotEquals(chunk, chunk.copy(encodingVersion = 2))
    }
}

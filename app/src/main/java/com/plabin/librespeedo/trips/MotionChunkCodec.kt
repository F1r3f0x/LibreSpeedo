package com.plabin.librespeedo.trips

import com.plabin.librespeedo.sensors.MotionSample
import com.plabin.librespeedo.sensors.MotionSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Versioned compact GZIP encoding for timestamped IMU samples. */
object MotionChunkCodec {
    fun encode(samples: List<MotionSample>): ByteArray {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { zipped ->
            DataOutputStream(zipped).use { out ->
                out.writeInt(1)
                out.writeInt(samples.size)
                var priorTimestamp = 0L
                samples.forEachIndexed { index, sample ->
                    out.writeLong(if (index == 0) sample.elapsedRealtimeNanos else sample.elapsedRealtimeNanos - priorTimestamp)
                    priorTimestamp = sample.elapsedRealtimeNanos
                    out.writeFloat(sample.xMetersPerSecondSquared)
                    out.writeFloat(sample.yMetersPerSecondSquared)
                    out.writeFloat(sample.zMetersPerSecondSquared)
                    out.writeFloat(sample.horizontalMetersPerSecondSquared ?: Float.NaN)
                    out.writeFloat(sample.verticalMetersPerSecondSquared ?: Float.NaN)
                    out.writeFloat(sample.totalDynamicMetersPerSecondSquared)
                    out.writeInt(sample.accuracy)
                    out.writeByte(sample.source.ordinal)
                    out.writeBoolean(sample.orientationValid)
                    out.writeInt(sample.qualityFlags)
                }
            }
        }
        return bytes.toByteArray()
    }

    fun decode(compressed: ByteArray): List<MotionSample> {
        GZIPInputStream(ByteArrayInputStream(compressed)).use { zipped ->
            DataInputStream(zipped).use { input ->
                require(input.readInt() == 1) { "Unsupported motion chunk version" }
                val count = input.readInt()
                require(count in 0..100_000) { "Invalid motion chunk sample count" }
                var timestamp = 0L
                return List(count) { index ->
                    val delta = input.readLong()
                    timestamp = if (index == 0) delta else timestamp + delta
                    val x = input.readFloat()
                    val y = input.readFloat()
                    val z = input.readFloat()
                    val horizontal = input.readFloat().takeUnless { it.isNaN() }
                    val vertical = input.readFloat().takeUnless { it.isNaN() }
                    val total = input.readFloat()
                    val accuracy = input.readInt()
                    val source = MotionSource.entries[input.readUnsignedByte()]
                    val orientation = input.readBoolean()
                    val flags = input.readInt()
                    MotionSample(timestamp, x, y, z, horizontal, vertical, total, accuracy, source, orientation, flags)
                }
            }
        }
    }
}

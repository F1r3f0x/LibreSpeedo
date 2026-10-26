package com.plabin.librespeedo.trips

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TripDatabaseCheckpointTest {
    @Test
    fun checkpointRecoveryAndFinalizationPreserveTripOwnedRows() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Application>(),
            TripDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            val dao = database.tripDao()
            val trip = TripEntity(
                id = "trip-1",
                startUtcMillis = 100L,
                startElapsedNanos = 1_000L,
                bootSession = "boot-1",
                origin = "MANUAL",
                status = TripStatus.RECORDING.name,
                lastCheckpointElapsedNanos = 2_000L
            )
            dao.checkpointTrip(
                trip,
                pending = listOf(
                    TripSampleEntity(
                        tripId = trip.id,
                        elapsedRealtimeNanos = 1_500L,
                        wallTimeUtcMillis = 150L,
                        speedMetersPerSecond = 2.0,
                        accuracyMetersPerSecond = 0.5,
                        provider = "gps",
                        accepted = true
                    )
                ),
                chunks = listOf(
                    MotionChunkEntity(
                        tripId = trip.id,
                        startElapsedRealtimeNanos = 1_000L,
                        endElapsedRealtimeNanos = 2_000L,
                        sampleCount = 1,
                        source = "LINEAR_ACCELERATION",
                        compressedSamples = byteArrayOf(1, 2, 3)
                    )
                ),
                events = listOf(
                    DrivingEventEntity(
                        tripId = trip.id,
                        type = "HARD_ACCELERATION",
                        startElapsedRealtimeNanos = 1_000L,
                        endElapsedRealtimeNanos = 2_000L,
                        peakValue = 0.3,
                        durationNanos = 1_000L,
                        speedContextMetersPerSecond = 10.0,
                        confidence = 0.9,
                        detectorVersion = 1
                    )
                )
            )
            dao.putRating(
                TripRatingEntity(
                    tripId = trip.id,
                    scoringVersion = 1,
                    calculatedUtcMillis = 200L,
                    totalScore = null,
                    accelerationScore = null,
                    brakingScore = null,
                    corneringScore = null,
                    eligibility = "NOT_RATED",
                    explanation = "Insufficient calibration"
                )
            )

            dao.checkpointTrip(trip.copy(distanceMeters = 12.0), emptyList(), emptyList(), emptyList())
            dao.putTrip(trip.copy(status = TripStatus.INTERRUPTED.name))
            dao.finishAtBoundary(trip.copy(status = TripStatus.FINISHED.name), 2_000L)

            assertEquals(1, dao.getSamples(trip.id).size)
            assertEquals(1L, rowCount(database, "motion_chunks", trip.id))
            assertEquals(1L, rowCount(database, "driving_events", trip.id))
            assertEquals(1L, rowCount(database, "trip_ratings", trip.id))
        } finally {
            database.close()
        }
    }

    private fun rowCount(database: TripDatabase, table: String, tripId: String): Long =
        database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM $table WHERE tripId = ?", arrayOf(tripId))
            .use { cursor ->
                check(cursor.moveToFirst())
                cursor.getLong(0)
            }
}

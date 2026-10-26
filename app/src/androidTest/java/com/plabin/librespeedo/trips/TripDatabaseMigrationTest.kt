package com.plabin.librespeedo.trips

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TripDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migratesVersion2TripsAndKeepsCarAndSummaryData() {
        val databaseName = "trip-migration-v2"
        helper.createDatabase(databaseName, 2).apply {
            execSQL(
                """INSERT INTO trips (
                    id, startUtcMillis, startElapsedNanos, bootSession, origin, carAddress, status,
                    lastCheckpointElapsedNanos, distanceMeters, speedIntegralMeters, speedCoveredNanos,
                    movingNanos, stoppedNanos, unknownNanos, speedCoverage, movementCoverage,
                    algorithmVersion, routeStorageEnabled
                ) VALUES ('legacy-trip', 1000, 500, 'boot-a', 'AUTOMATIC', '11:22:33:44:55:66',
                    'FINISHED', 2000500, 120.0, 120.0, 2000000000, 1500000000, 300000000,
                    200000000, 0.9, 0.95, 1, 0)"""
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 5, true, *TripDatabase.ALL_MIGRATIONS)
        migrated.query(
            "SELECT carAddress, maximumHorizontalG, validGDurationNanos, mountingConfirmed FROM trips WHERE id = 'legacy-trip'"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("11:22:33:44:55:66", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertEquals(0L, cursor.getLong(2))
            assertEquals(0, cursor.getInt(3))
        }
        migrated.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        migrated.close()
    }

    @Test
    fun migratesMotionSchemaAndCreatesEventAndRatingTables() {
        val databaseName = "trip-migration-v3"
        helper.createDatabase(databaseName, 3).close()
        val migrated = helper.runMigrationsAndValidate(databaseName, 5, true, *TripDatabase.ALL_MIGRATIONS)
        listOf("motion_chunks", "driving_events", "trip_ratings").forEach { table ->
            migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = '$table'").use {
                assertTrue("Missing $table", it.moveToFirst())
            }
        }
        migrated.close()
    }
}

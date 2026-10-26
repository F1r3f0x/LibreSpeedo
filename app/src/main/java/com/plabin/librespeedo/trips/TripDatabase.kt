package com.plabin.librespeedo.trips

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.ColumnInfo
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val startUtcMillis: Long,
    val endUtcMillis: Long? = null,
    val startElapsedNanos: Long,
    val endElapsedNanos: Long? = null,
    val bootSession: String,
    val origin: String,
    val carAddress: String? = null,
    val status: String,
    val endReason: String? = null,
    val lastCheckpointElapsedNanos: Long,
    val topSpeedMetersPerSecond: Double? = null,
    val distanceMeters: Double = 0.0,
    val speedIntegralMeters: Double = 0.0,
    val speedCoveredNanos: Long = 0L,
    val movingNanos: Long = 0L,
    val stoppedNanos: Long = 0L,
    val unknownNanos: Long = 0L,
    val speedCoverage: Double = 0.0,
    val movementCoverage: Double = 0.0,
    val maximumHorizontalG: Double? = null,
    val averageHorizontalG: Double? = null,
    val movingAverageHorizontalG: Double? = null,
    val maximumVerticalG: Double? = null,
    val maximumTotalDynamicG: Double? = null,
    val rawDiagnosticPeakG: Double? = null,
    @ColumnInfo(defaultValue = "0") val horizontalGIntegralSeconds: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val movingHorizontalGIntegralSeconds: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val validGDurationNanos: Long = 0L,
    @ColumnInfo(defaultValue = "0") val movingValidGDurationNanos: Long = 0L,
    @ColumnInfo(defaultValue = "0") val totalDynamicGDurationNanos: Long = 0L,
    @ColumnInfo(defaultValue = "1") val motionAlgorithmVersion: Int = 1,
    val rmsHorizontalG: Double? = null,
    val p95HorizontalG: Double? = null,
    @ColumnInfo(defaultValue = "0") val exposureAbove015GSeconds: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val exposureAbove025GSeconds: Double = 0.0,
    val maximumForwardG: Double? = null,
    val maximumBrakingG: Double? = null,
    val maximumLateralG: Double? = null,
    val maximumJerkGPerSecond: Double? = null,
    @ColumnInfo(defaultValue = "0") val mountingConfirmed: Boolean = false,
    val algorithmVersion: Int = 1,
    val routeStorageEnabled: Boolean = false
)

@Entity(
    tableName = "trip_samples",
    foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId"), Index(value = ["tripId", "elapsedRealtimeNanos"], unique = true)]
)
data class TripSampleEntity(
    @PrimaryKey(autoGenerate = true) val sampleId: Long = 0L,
    val tripId: String,
    val elapsedRealtimeNanos: Long,
    val wallTimeUtcMillis: Long,
    val speedMetersPerSecond: Double?,
    val accuracyMetersPerSecond: Double?,
    val provider: String?,
    val accepted: Boolean,
    val movementState: String = MovementState.UNKNOWN.name,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val horizontalG: Double? = null,
    val verticalG: Double? = null,
    val totalDynamicG: Double? = null,
    val motionSource: String? = null,
    @ColumnInfo(defaultValue = "0") val qualityFlags: Int = 0
)

@Entity(
    tableName = "motion_chunks",
    foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId"), Index("endElapsedRealtimeNanos")]
)
data class MotionChunkEntity(
    @PrimaryKey(autoGenerate = true) val chunkId: Long = 0L,
    val tripId: String,
    val startElapsedRealtimeNanos: Long,
    val endElapsedRealtimeNanos: Long,
    val sampleCount: Int,
    val source: String,
    @ColumnInfo(defaultValue = "1") val encodingVersion: Int = 1,
    val compressedSamples: ByteArray
)

@Entity(
    tableName = "driving_events",
    foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId")]
)
data class DrivingEventEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Long = 0L,
    val tripId: String,
    val type: String,
    val startElapsedRealtimeNanos: Long,
    val endElapsedRealtimeNanos: Long,
    val peakValue: Double,
    val durationNanos: Long,
    val speedContextMetersPerSecond: Double?,
    val confidence: Double,
    val detectorVersion: Int,
    val supportingChunkIds: String? = null
)

@Entity(
    tableName = "trip_ratings",
    foreignKeys = [ForeignKey(entity = TripEntity::class, parentColumns = ["id"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("tripId")]
)
data class TripRatingEntity(
    @PrimaryKey(autoGenerate = true) val ratingId: Long = 0L,
    val tripId: String,
    val scoringVersion: Int,
    val calculatedUtcMillis: Long,
    val totalScore: Double?,
    val accelerationScore: Double?,
    val brakingScore: Double?,
    val corneringScore: Double?,
    val eligibility: String,
    val explanation: String
)

@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertTripIfAbsent(trip: TripEntity): Long
    @Update suspend fun updateTrip(trip: TripEntity)
    @Transaction
    suspend fun putTrip(trip: TripEntity) {
        if (insertTripIfAbsent(trip) == -1L) updateTrip(trip)
    }
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSample(sample: TripSampleEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMotionChunk(chunk: MotionChunkEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMotionChunks(chunks: List<MotionChunkEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDrivingEvents(events: List<DrivingEventEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putRating(rating: TripRatingEntity)
    @Query("SELECT * FROM trips WHERE status != 'RECORDING' ORDER BY startUtcMillis DESC") fun observeTrips(): Flow<List<TripEntity>>
    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1") suspend fun getTrip(id: String): TripEntity?
    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1") fun observeTrip(id: String): Flow<TripEntity?>
    @Query("SELECT * FROM trip_samples WHERE tripId = :id ORDER BY elapsedRealtimeNanos") fun observeSamples(id: String): Flow<List<TripSampleEntity>>
    @Query("SELECT * FROM trip_samples WHERE tripId = :id ORDER BY elapsedRealtimeNanos") suspend fun getSamples(id: String): List<TripSampleEntity>
    @Query("SELECT * FROM driving_events WHERE tripId = :id ORDER BY startElapsedRealtimeNanos") suspend fun getDrivingEvents(id: String): List<DrivingEventEntity>
    @Query("SELECT * FROM trips WHERE status = 'RECORDING' ORDER BY startUtcMillis DESC LIMIT 1") suspend fun unfinishedTrip(): TripEntity?
    @Query("DELETE FROM trips WHERE id = :id") suspend fun deleteTrip(id: String)
    @Query("DELETE FROM trips WHERE endUtcMillis IS NOT NULL AND endUtcMillis < :cutoffUtcMillis") suspend fun deleteTripsBefore(cutoffUtcMillis: Long)
    @Query("DELETE FROM trip_samples WHERE tripId = :id") suspend fun deleteSamples(id: String)
    @Query("DELETE FROM trip_samples WHERE tripId = :id AND elapsedRealtimeNanos > :boundaryNanos") suspend fun deleteSamplesAfter(id: String, boundaryNanos: Long)
    @Query("DELETE FROM motion_chunks WHERE tripId = :id AND endElapsedRealtimeNanos > :boundaryNanos") suspend fun deleteMotionChunksAfter(id: String, boundaryNanos: Long)
    @Query("DELETE FROM driving_events WHERE tripId = :id AND (startElapsedRealtimeNanos > :boundaryNanos OR endElapsedRealtimeNanos > :boundaryNanos)") suspend fun deleteDrivingEventsAfter(id: String, boundaryNanos: Long)
    @Query("DELETE FROM motion_chunks WHERE endElapsedRealtimeNanos < :cutoffNanos") suspend fun deleteMotionChunksBefore(cutoffNanos: Long)
    @Transaction
    suspend fun finishAndStore(trip: TripEntity, pending: List<TripSampleEntity>) {
        putTrip(trip)
        pending.forEach { insertSample(it) }
    }
    @Transaction
    suspend fun checkpointTrip(
        trip: TripEntity,
        pending: List<TripSampleEntity>,
        chunks: List<MotionChunkEntity>,
        events: List<DrivingEventEntity>
    ) {
        putTrip(trip)
        pending.forEach { insertSample(it) }
        insertMotionChunks(chunks)
        insertDrivingEvents(events)
    }
    @Transaction
    suspend fun finishAtBoundary(trip: TripEntity, boundaryNanos: Long) {
        deleteSamplesAfter(trip.id, boundaryNanos)
        deleteMotionChunksAfter(trip.id, boundaryNanos)
        deleteDrivingEventsAfter(trip.id, boundaryNanos)
        putTrip(trip)
    }
}

@Database(entities = [TripEntity::class, TripSampleEntity::class, MotionChunkEntity::class, DrivingEventEntity::class, TripRatingEntity::class], version = 5, exportSchema = true)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile private var instance: TripDatabase? = null
        fun get(context: Context): TripDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TripDatabase::class.java, "trips.db")
                .addMigrations(*ALL_MIGRATIONS)
                .build()
                .also { instance = it }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN carAddress TEXT")
            }
        }
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumHorizontalG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN averageHorizontalG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN movingAverageHorizontalG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumVerticalG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumTotalDynamicG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN rawDiagnosticPeakG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN horizontalGIntegralSeconds REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN movingHorizontalGIntegralSeconds REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN validGDurationNanos INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN movingValidGDurationNanos INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN totalDynamicGDurationNanos INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN motionAlgorithmVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE trip_samples ADD COLUMN horizontalG REAL")
                db.execSQL("ALTER TABLE trip_samples ADD COLUMN verticalG REAL")
                db.execSQL("ALTER TABLE trip_samples ADD COLUMN totalDynamicG REAL")
                db.execSQL("ALTER TABLE trip_samples ADD COLUMN motionSource TEXT")
                db.execSQL("ALTER TABLE trip_samples ADD COLUMN qualityFlags INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS motion_chunks (chunkId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tripId TEXT NOT NULL, startElapsedRealtimeNanos INTEGER NOT NULL, endElapsedRealtimeNanos INTEGER NOT NULL, sampleCount INTEGER NOT NULL, source TEXT NOT NULL, encodingVersion INTEGER NOT NULL DEFAULT 1, compressedSamples BLOB NOT NULL, FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_motion_chunks_tripId ON motion_chunks(tripId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_motion_chunks_endElapsedRealtimeNanos ON motion_chunks(endElapsedRealtimeNanos)")
                db.execSQL("CREATE TABLE IF NOT EXISTS driving_events (eventId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tripId TEXT NOT NULL, type TEXT NOT NULL, startElapsedRealtimeNanos INTEGER NOT NULL, endElapsedRealtimeNanos INTEGER NOT NULL, peakValue REAL NOT NULL, durationNanos INTEGER NOT NULL, speedContextMetersPerSecond REAL, confidence REAL NOT NULL, detectorVersion INTEGER NOT NULL, supportingChunkIds TEXT, FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_driving_events_tripId ON driving_events(tripId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS trip_ratings (ratingId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, tripId TEXT NOT NULL, scoringVersion INTEGER NOT NULL, calculatedUtcMillis INTEGER NOT NULL, totalScore REAL, accelerationScore REAL, brakingScore REAL, corneringScore REAL, eligibility TEXT NOT NULL, explanation TEXT NOT NULL, FOREIGN KEY(tripId) REFERENCES trips(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trip_ratings_tripId ON trip_ratings(tripId)")
            }
        }
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN rmsHorizontalG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN p95HorizontalG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN exposureAbove015GSeconds REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN exposureAbove025GSeconds REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumForwardG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumBrakingG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumLateralG REAL")
                db.execSQL("ALTER TABLE trips ADD COLUMN maximumJerkGPerSecond REAL")
            }
        }
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN mountingConfirmed INTEGER NOT NULL DEFAULT 0")
            }
        }
        val ALL_MIGRATIONS: Array<androidx.room.migration.Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )
    }
}

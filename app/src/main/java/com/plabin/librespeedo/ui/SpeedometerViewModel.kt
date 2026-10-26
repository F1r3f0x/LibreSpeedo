/*
 * Copyright (C) 2026 Patricio Labin Correa (f1r3f0x)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plabin.librespeedo.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.plabin.librespeedo.data.SettingsRepository
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.location.LocationClient
import com.plabin.librespeedo.sensors.SensorClient
import com.plabin.librespeedo.sensors.MotionClient
import com.plabin.librespeedo.sensors.MotionSample
import com.plabin.librespeedo.utils.SpeedConverter
import com.plabin.librespeedo.trips.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Defines the available modular widgets for the main dashboard screen.
 */
enum class WidgetType {
    /** Primary speedometer display showing numerical speed and unit. */
    SPEEDOMETER,
    /** Analog gauge speedometer with sweeping needle, track arc, and speed readout. */
    ANALOG_SPEEDOMETER,
    /** Real-time digital compass showing current heading and North pointer. */
    COMPASS,
    /** Animated analog compass dial with cardinal points and animated needle. */
    ANALOG_COMPASS,
    /** Stopwatch / chronometer widget for timing trips, laps, and durations. */
    CHRONOMETER,
    /** Displays current GPS coordinates (latitude and longitude). */
    POSITION,
    /** Diagnostic readout of location provider, raw sensor metrics, and accuracy. */
    DEBUG,
    /** Demonstration widget displaying placeholder greeting. */
    HELLO_WORLD
}

/**
 * Represents the immutable UI state of the speedometer dashboard.
 *
 * @property speedKmh Current speed in kilometers per hour (km/h).
 * @property latitude Current latitude in decimal degrees.
 * @property longitude Current longitude in decimal degrees.
 * @property heading Active fused heading in degrees (GPS bearing if moving > 3 km/h, else compass azimuth).
 * @property altitude Current altitude above sea level in meters.
 * @property accuracy Estimated horizontal accuracy radius in meters.
 * @property provider Active location provider name (e.g., "fused", "gps", "network").
 * @property accelX Filtered accelerometer X-axis acceleration in m/s².
 * @property accelY Filtered accelerometer Y-axis acceleration in m/s².
 * @property accelZ Filtered accelerometer Z-axis acceleration in m/s².
 * @property gyroX Raw gyroscope X-axis angular velocity in rad/s.
 * @property gyroY Raw gyroscope Y-axis angular velocity in rad/s.
 * @property gyroZ Raw gyroscope Z-axis angular velocity in rad/s.
 * @property magX Filtered geomagnetic field X-axis flux density in μT.
 * @property magY Filtered geomagnetic field Y-axis flux density in μT.
 * @property magZ Filtered geomagnetic field Z-axis flux density in μT.
 * @property compassAzimuth Hardware sensor compass heading in degrees [0, 360).
 * @property gpsBearing GNSS location bearing in degrees [0, 360).
 * @property isLocationFresh True while the last location fix is less than five seconds old.
 * @property isSpeedAvailable True if the fresh location fix includes a speed measurement.
 * @property isTracking True if active location/sensor collection is running.
 * @property error Error message if location updates failed, or null if healthy.
 */
data class SpeedometerUiState(
    val speedKmh: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Float = 0f,
    val altitude: Double = 0.0,
    val accuracy: Double = 0.0,
    val provider: String = "None",
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val compassAzimuth: Float = 0f,
    val gpsBearing: Float = 0f,
    val isLocationFresh: Boolean = false,
    val isSpeedAvailable: Boolean = false,
    val hasLocationAccuracy: Boolean = false,
    val isTracking: Boolean = false,
    val error: String? = null
)

data class TripLoggerUiState(
    val trip: TripEntity? = null,
    val trips: List<TripEntity> = emptyList(),
    val isReady: Boolean = false,
    val pairedCars: List<PairedCar> = emptyList(),
    val carConnection: CarConnection = CarConnection.UNKNOWN,
    val motionAvailable: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel responsible for managing the GPS and sensor tracking lifecycles,
 * computing smart fused bearings, and managing dashboard widget configuration.
 *
 * Uses an [AndroidViewModel] to inject the application context into [LocationClient],
 * [SensorClient], and [SettingsRepository].
 *
 * @param application The application instance.
 * @param locationClient Optional client for location updates (defaults to a new [LocationClient]).
 * @param sensorClient Optional client for sensor updates (defaults to a new [SensorClient]).
 * @param settingsRepository Optional repository for settings (defaults to a new [SettingsRepository]).
 * @param coroutineScope Optional coroutine scope override for background streams (defaults to [viewModelScope]).
 */
class SpeedometerViewModel @JvmOverloads constructor(
    application: Application,
    private val locationClient: LocationClient = LocationClient(application),
    private val sensorClient: SensorClient = SensorClient(application),
    private val settingsRepository: SettingsRepository = SettingsRepository(application),
    private val coroutineScope: CoroutineScope? = null,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime
) : AndroidViewModel(application) {

    private val tripDao = TripDatabase.get(application).tripDao()
    private val bluetoothClient = BluetoothConnectionClient(application)
    private val motionClient = MotionClient(application)
    private val tripMutex = Mutex()
    private var metricAccumulator = TripMetricsAccumulator()
    private var pendingTripSamples = mutableListOf<TripSampleEntity>()
    private var activeTrip: TripEntity? = null
    private var lastCheckpointMs = 0L
    private var bluetoothJob: Job? = null
    private var motionJob: Job? = null
    private var autoStartCandidate = mutableListOf<Pair<SpeedObservation, TripSampleEntity>>()
    private var preTripMotionBuffer = mutableListOf<MotionSample>()
    private var pendingMotionSamples = mutableListOf<MotionSample>()
    private var latestMotionSample: MotionSample? = null
    private var motionAccumulator = MotionMetricsAccumulator()
    private var eventDetector = DrivingEventDetector()
    private var pendingDrivingEvents = mutableListOf<DetectedDrivingEvent>()
    private var lastMotionChunkFlushNanos = 0L
    private val appVisible = MutableStateFlow(false)
    private var graceTripCheckpoint: TripEntity? = null
    private var graceBoundaryUtcMillis: Long? = null
    private val _tripLogger = MutableStateFlow(TripLoggerUiState())
    val tripLogger: StateFlow<TripLoggerUiState> = _tripLogger.asStateFlow()

    private val scope: CoroutineScope
        get() = coroutineScope ?: viewModelScope

    private val _uiState = MutableStateFlow(SpeedometerUiState())
    /** StateFlow emitting current location metrics, sensor readings, and tracking status. */
    val uiState: StateFlow<SpeedometerUiState> = _uiState.asStateFlow()

    val chronometer = ChronometerState(elapsedRealtime)
    private var locationJob: Job? = null
    private var sensorJob: Job? = null
    private var expiryJob: Job? = null
    private var hasGpsBearing = false
    private var latestLocationFixElapsedRealtimeNanos: Long? = null

    private fun invalidateLocation() {
        _uiState.value = _uiState.value.copy(
            speedKmh = 0f,
            isLocationFresh = false,
            isSpeedAvailable = false,
            hasLocationAccuracy = false,
            heading = _uiState.value.compassAzimuth
        )
    }

    private suspend fun markLocationExpired(fixElapsedRealtimeNanos: Long, expiredAtNanos: Long) {
        tripMutex.withLock {
            if (latestLocationFixElapsedRealtimeNanos != fixElapsedRealtimeNanos) return@withLock
            val trip = activeTrip ?: return@withLock
            val lastObservedNanos = metricAccumulator.latestTimestampNanos ?: return@withLock
            if (lastObservedNanos > fixElapsedRealtimeNanos || expiredAtNanos <= lastObservedNanos) return@withLock

            val expiredObservation = SpeedObservation(
                elapsedRealtimeNanos = expiredAtNanos,
                wallTimeUtcMillis = System.currentTimeMillis(),
                speedMetersPerSecond = null,
                accepted = false,
                provider = "location-expired"
            )
            if (!metricAccumulator.add(expiredObservation)) return@withLock
            pendingTripSamples += TripSampleEntity(
                tripId = trip.id,
                elapsedRealtimeNanos = expiredObservation.elapsedRealtimeNanos,
                wallTimeUtcMillis = expiredObservation.wallTimeUtcMillis,
                speedMetersPerSecond = null,
                accuracyMetersPerSecond = null,
                provider = expiredObservation.provider,
                accepted = false,
                movementState = MovementState.UNKNOWN.name
            )
        }
    }

    private fun useGpsHeading() = _uiState.value.isLocationFresh &&
        _uiState.value.isSpeedAvailable && _uiState.value.speedKmh > 3f && hasGpsBearing

    private val defaultWidgets = listOf(
        WidgetType.SPEEDOMETER,
        WidgetType.COMPASS,
        WidgetType.POSITION,
        WidgetType.DEBUG
    )

    init {
        scope.launch {
            _tripLogger.value = _tripLogger.value.copy(trips = tripDao.observeTrips().first())
            val unfinished = withContext(Dispatchers.IO) { tripDao.unfinishedTrip() }
            if (unfinished != null) {
                val interrupted = unfinished.copy(
                    status = TripStatus.INTERRUPTED.name,
                    endReason = TripEndReason.RECOVERED.name,
                    endUtcMillis = System.currentTimeMillis(),
                    endElapsedNanos = unfinished.lastCheckpointElapsedNanos
                )
                tripMutex.withLock { withContext(Dispatchers.IO) { tripDao.putTrip(interrupted) } }
            }
            _tripLogger.value = _tripLogger.value.copy(isReady = true)
            tripDao.observeTrips().collect { trips -> _tripLogger.value = _tripLogger.value.copy(trips = trips) }
        }
        scope.launch {
            combine(settingsRepository.automaticTripLogging, settingsRepository.selectedCarAddress, appVisible) { enabled, address, visible -> Triple(enabled, address, visible) }
                .collectLatest { (enabled, address, visible) ->
                    bluetoothJob?.cancel()
                    bluetoothJob = null
                    if (activeTrip?.origin == "AUTOMATIC" && address != null && activeTrip?.carAddress != address) {
                        finishTrip(TripEndReason.AUTOMATION_DISABLED)
                    }
                    if (!enabled || address == null || !visible) {
                        _tripLogger.value = _tripLogger.value.copy(carConnection = CarConnection.UNKNOWN)
                        if ((!enabled || address == null) && activeTrip?.origin == "AUTOMATIC") {
                            finishTrip(TripEndReason.AUTOMATION_DISABLED)
                        }
                    } else {
                        bluetoothJob = scope.launch {
                            bluetoothClient.observe(address).collect { connection ->
                                onCarConnectionChanged(address, connection)
                            }
                        }
                    }
                }
        }
        scope.launch {
            settingsRepository.tripRetentionDays.collectLatest { days ->
                if (days > 0) {
                    val cutoff = System.currentTimeMillis() - days * 86_400_000L
                    tripMutex.withLock { withContext(Dispatchers.IO) { tripDao.deleteTripsBefore(cutoff) } }
                }
            }
        }
    }

    fun setAppVisible(visible: Boolean) {
        appVisible.value = visible
        if (visible) {
            refreshPairedCars()
            _tripLogger.value = _tripLogger.value.copy(motionAvailable = motionClient.availability().available)
            if (motionJob?.isActive != true) {
                motionJob = scope.launch { motionClient.samples().collect { recordMotionSample(it) } }
            }
        } else {
            motionJob?.cancel()
            motionJob = null
        }
    }

    fun refreshPairedCars() {
        scope.launch {
            val cars = withContext(Dispatchers.IO) { bluetoothClient.pairedCars() }
            _tripLogger.value = _tripLogger.value.copy(pairedCars = cars)
        }
    }

    private fun onCarConnectionChanged(address: String, connection: CarConnection) {
        if (settingsRepository.selectedCarAddress.value != address) return
        _tripLogger.value = _tripLogger.value.copy(carConnection = connection)
        if (connection == CarConnection.CONNECTED) {
            // Reconnecting the selected car cancels its pending disconnect grace timer.
            disconnectGraceJob?.cancel()
            disconnectGraceJob = null
            graceTripCheckpoint = null
            graceBoundaryUtcMillis = null
        } else if (connection == CarConnection.DISCONNECTED) {
            autoStartCandidate.clear()
            if (activeTrip?.origin == "AUTOMATIC" && activeTrip?.carAddress == address) prepareDisconnectGrace(address)
        }
    }

    private var disconnectGraceJob: Job? = null

    private fun beginDisconnectGrace(address: String) {
        if (_tripLogger.value.carConnection != CarConnection.DISCONNECTED || disconnectGraceJob?.isActive == true) return
        disconnectGraceJob = scope.launch {
            delay(60.seconds)
            if (_tripLogger.value.carConnection == CarConnection.DISCONNECTED && activeTrip?.carAddress == address) {
                finishAtDisconnectBoundary(address)
            }
        }
    }

    private fun prepareDisconnectGrace(address: String) {
        if (disconnectGraceJob?.isActive == true) return
        scope.launch {
            tripMutex.withLock {
                val trip = activeTrip ?: return@withLock
                if (graceTripCheckpoint != null) return@withLock
                if (trip.origin != "AUTOMATIC" || trip.carAddress != address) return@withLock
                val boundaryNanos = SystemClock.elapsedRealtimeNanos()
                val boundaryUtc = System.currentTimeMillis()
                val boundaryObservation = SpeedObservation(
                    elapsedRealtimeNanos = boundaryNanos,
                    wallTimeUtcMillis = boundaryUtc,
                    speedMetersPerSecond = null,
                    accepted = false,
                    provider = "bluetooth-disconnect"
                )
                val sample = TripSampleEntity(
                    tripId = trip.id,
                    elapsedRealtimeNanos = boundaryNanos,
                    wallTimeUtcMillis = boundaryUtc,
                    speedMetersPerSecond = null,
                    accuracyMetersPerSecond = null,
                    provider = "bluetooth-disconnect",
                    accepted = false,
                    movementState = MovementState.UNKNOWN.name
                )
                if (!metricAccumulator.add(boundaryObservation)) return@withLock
                val metrics = metricAccumulator.snapshot()
                pendingDrivingEvents += eventDetector.flush(boundaryNanos)
                eventDetector = DrivingEventDetector()
                val checkpoint = trip.copy(
                    lastCheckpointElapsedNanos = boundaryNanos,
                    topSpeedMetersPerSecond = metrics.topSpeedMetersPerSecond,
                    distanceMeters = metrics.distanceMeters,
                    speedIntegralMeters = metrics.speedIntegralMeters,
                    speedCoveredNanos = metrics.speedCoveredNanos,
                    movingNanos = metrics.movingNanos,
                    stoppedNanos = metrics.stoppedNanos,
                    unknownNanos = metrics.unknownNanos,
                    speedCoverage = metrics.speedCoverage,
                    movementCoverage = metrics.movementCoverage
                ).withMetrics(metrics, motionAccumulator.snapshot())
                val chunks = takeMotionChunk(trip.id)
                val events = pendingDrivingEvents.map { it.toEntity(trip.id) }
                try {
                    withContext(Dispatchers.IO) { tripDao.checkpointTrip(checkpoint, pendingTripSamples + sample, chunks, events) }
                    pendingTripSamples.clear()
                    pendingMotionSamples.clear()
                    pendingDrivingEvents.clear()
                    activeTrip = checkpoint
                    graceTripCheckpoint = checkpoint
                    graceBoundaryUtcMillis = boundaryUtc
                    lastCheckpointMs = elapsedRealtime()
                    lastMotionChunkFlushNanos = boundaryNanos
                    _tripLogger.value = _tripLogger.value.copy(trip = checkpoint, error = null)
                } catch (e: Exception) {
                    activeTrip = null
                    pendingTripSamples.clear()
                    pendingDrivingEvents.clear()
                    _tripLogger.value = _tripLogger.value.copy(trip = null, error = "Trip checkpoint failed: ${e.message ?: "storage error"}")
                }
            }
            if (activeTrip?.carAddress == address) beginDisconnectGrace(address)
        }
    }

    private fun finishAtDisconnectBoundary(address: String) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { tripMutex.withLock {
                val checkpoint = graceTripCheckpoint ?: return@withLock
                if (_tripLogger.value.carConnection != CarConnection.DISCONNECTED ||
                    activeTrip?.id != checkpoint.id || checkpoint.carAddress != address
                ) return@withLock
                val ended = checkpoint.copy(
                    endUtcMillis = graceBoundaryUtcMillis,
                    endElapsedNanos = checkpoint.lastCheckpointElapsedNanos,
                    status = TripStatus.FINISHED.name,
                    endReason = TripEndReason.DISCONNECT_GRACE.name
                )
                try {
                    withContext(Dispatchers.IO) {
                        tripDao.finishAtBoundary(ended, checkpoint.lastCheckpointElapsedNanos)
                    }
                    activeTrip = null
                    pendingTripSamples.clear()
                    graceTripCheckpoint = null
                    graceBoundaryUtcMillis = null
                    _tripLogger.value = _tripLogger.value.copy(trip = null, error = null)
                } catch (e: Exception) {
                    activeTrip = null
                    _tripLogger.value = _tripLogger.value.copy(trip = null, error = "Trip could not be finalized: ${e.message ?: "storage error"}")
                }
            } }
        }
    }

    fun startManualTrip() {
        scope.launch {
            tripMutex.withLock {
                if (activeTrip != null) return@withLock
                val nowElapsedNanos = SystemClock.elapsedRealtimeNanos()
                val trip = TripEntity(
                    id = UUID.randomUUID().toString(), startUtcMillis = System.currentTimeMillis(),
                    startElapsedNanos = nowElapsedNanos, lastCheckpointElapsedNanos = nowElapsedNanos,
                    bootSession = "${android.os.Build.FINGERPRINT}:${System.currentTimeMillis() - SystemClock.elapsedRealtime()}",
                    origin = "MANUAL", status = TripStatus.RECORDING.name,
                    routeStorageEnabled = settingsRepository.storeTripRoute.value,
                    mountingConfirmed = settingsRepository.stableMountConfirmed.value
                )
                try {
                    withContext(Dispatchers.IO) { tripDao.putTrip(trip) }
                    activeTrip = trip
                    autoStartCandidate.clear()
                    pendingTripSamples.clear()
                    pendingMotionSamples.clear()
                    metricAccumulator = TripMetricsAccumulator()
                    metricAccumulator.start(nowElapsedNanos)
                    eventDetector = DrivingEventDetector()
                    pendingDrivingEvents.clear()
                    preTripMotionBuffer.clear()
                    motionAccumulator = MotionMetricsAccumulator()
                    lastMotionChunkFlushNanos = nowElapsedNanos
                    lastCheckpointMs = elapsedRealtime()
                    _tripLogger.value = _tripLogger.value.copy(trip = trip, error = null)
                } catch (e: Exception) {
                    _tripLogger.value = _tripLogger.value.copy(error = "Trip could not be started: ${e.message ?: "storage error"}")
                }
            }
        }
    }

    fun finishTrip(reason: TripEndReason = TripEndReason.MANUAL) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { withContext(NonCancellable) { finishTripInternal(reason) } }
    }

    private suspend fun finishTripInternal(reason: TripEndReason) = tripMutex.withLock {
        val trip = activeTrip ?: return@withLock
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        metricAccumulator.finish(nowNanos)
        pendingDrivingEvents += eventDetector.flush(nowNanos)
        eventDetector = DrivingEventDetector()
        val metrics = metricAccumulator.snapshot()
        val ended = trip.withMetrics(metrics, motionAccumulator.snapshot()).copy(
            endUtcMillis = System.currentTimeMillis(), endElapsedNanos = nowNanos,
            lastCheckpointElapsedNanos = nowNanos,
            status = if (reason == TripEndReason.MANUAL || reason == TripEndReason.DISCONNECT_GRACE || reason == TripEndReason.AUTOMATION_DISABLED) TripStatus.FINISHED.name else TripStatus.INTERRUPTED.name,
            endReason = reason.name, topSpeedMetersPerSecond = metrics.topSpeedMetersPerSecond,
            distanceMeters = metrics.distanceMeters, speedIntegralMeters = metrics.speedIntegralMeters,
            speedCoveredNanos = metrics.speedCoveredNanos, movingNanos = metrics.movingNanos,
            stoppedNanos = metrics.stoppedNanos, unknownNanos = metrics.unknownNanos,
            speedCoverage = metrics.speedCoverage, movementCoverage = metrics.movementCoverage
        )
        val chunks = takeMotionChunk(trip.id)
        val events = pendingDrivingEvents.map { it.toEntity(trip.id) }
        try {
            withContext(Dispatchers.IO) { tripDao.checkpointTrip(ended, pendingTripSamples.toList(), chunks, events) }
            activeTrip = null
            pendingTripSamples.clear()
            pendingMotionSamples.clear()
            autoStartCandidate.clear()
            pendingDrivingEvents.clear()
            disconnectGraceJob?.cancel()
            disconnectGraceJob = null
            graceTripCheckpoint = null
            graceBoundaryUtcMillis = null
            _tripLogger.value = _tripLogger.value.copy(trip = null, error = null)
        } catch (e: Exception) {
            _tripLogger.value = _tripLogger.value.copy(error = "Trip could not be saved: ${e.message ?: "storage error"}")
            activeTrip = null
            pendingTripSamples.clear()
            autoStartCandidate.clear()
            pendingDrivingEvents.clear()
            _tripLogger.value = _tripLogger.value.copy(trip = null)
        }
    }

    fun deleteTrip(id: String) {
        scope.launch {
            tripMutex.withLock { withContext(Dispatchers.IO) { tripDao.deleteTrip(id) } }
        }
    }

    suspend fun exportTripDatabase(destination: Uri) {
        tripMutex.withLock {
            activeTrip?.let { trip ->
                val checkpoint = trip.withMetrics(metricAccumulator.snapshot(), motionAccumulator.snapshot()).copy(
                    lastCheckpointElapsedNanos = maxOf(
                        trip.lastCheckpointElapsedNanos,
                        metricAccumulator.latestTimestampNanos ?: trip.lastCheckpointElapsedNanos
                    )
                )
                val chunks = takeMotionChunk(trip.id)
                val events = pendingDrivingEvents.map { it.toEntity(trip.id) }
                withContext(Dispatchers.IO) {
                    tripDao.checkpointTrip(checkpoint, pendingTripSamples.toList(), chunks, events)
                }
                pendingTripSamples.clear()
                pendingMotionSamples.lastOrNull()?.let {
                    lastMotionChunkFlushNanos = it.elapsedRealtimeNanos
                }
                pendingMotionSamples.clear()
                pendingDrivingEvents.clear()
                activeTrip = checkpoint
                _tripLogger.value = _tripLogger.value.copy(trip = checkpoint, error = null)
            }
            TripDatabaseExporter.export(getApplication(), destination)
        }
    }

    suspend fun loadTripSamples(id: String): List<TripSampleEntity> = withContext(Dispatchers.IO) { tripDao.getSamples(id) }

    suspend fun loadDrivingEvents(id: String): List<DrivingEventEntity> = withContext(Dispatchers.IO) { tripDao.getDrivingEvents(id) }

    private suspend fun recordTripLocation(location: android.location.Location, speedMs: Double?) {
        if (activeTrip == null && (!settingsRepository.automaticTripLogging.value ||
                settingsRepository.selectedCarAddress.value == null ||
                _tripLogger.value.carConnection != CarConnection.CONNECTED)
        ) return
        tripMutex.withLock {
            val selectedAddress = settingsRepository.selectedCarAddress.value
            val elapsedNanos = location.elapsedRealtimeNanos.takeIf { it > 0L }
                ?: SystemClock.elapsedRealtimeNanos()
            val speedAccuracy = if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond.toDouble() else null
            val obs = SpeedObservation(
                elapsedRealtimeNanos = elapsedNanos,
                wallTimeUtcMillis = location.time,
                speedMetersPerSecond = speedMs,
                accuracyMetersPerSecond = speedAccuracy,
                accepted = speedMs != null && ((speedAccuracy != null && speedAccuracy <= 3.0) || (location.hasAccuracy() && location.accuracy <= 25f)),
                provider = location.provider,
                bearingDegrees = if (speedMs != null && speedMs > 3.0 / 3.6 && location.hasBearing()) location.bearing.toDouble() else null
            )
            val trip = activeTrip
            if (trip == null) {
                if (_tripLogger.value.isReady && settingsRepository.automaticTripLogging.value && selectedAddress != null &&
                    _tripLogger.value.carConnection == CarConnection.CONNECTED
                ) considerAutomaticStartLocked(location, obs, selectedAddress)
                return@withLock
            }
            if (elapsedNanos <= trip.startElapsedNanos) return@withLock
            if (!metricAccumulator.add(obs)) return@withLock
            val nearbyMotion = latestMotionSample?.takeIf { abs(it.elapsedRealtimeNanos - elapsedNanos) <= 250_000_000L }
            pendingTripSamples += TripSampleEntity(
                tripId = trip.id, elapsedRealtimeNanos = elapsedNanos, wallTimeUtcMillis = location.time,
                speedMetersPerSecond = speedMs, accuracyMetersPerSecond = obs.accuracyMetersPerSecond,
                provider = location.provider, accepted = metricAccumulator.lastObservationAccepted,
                latitude = if (trip.routeStorageEnabled) location.latitude else null,
                longitude = if (trip.routeStorageEnabled) location.longitude else null,
                horizontalG = nearbyMotion?.horizontalMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                verticalG = nearbyMotion?.verticalMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                totalDynamicG = nearbyMotion?.totalDynamicMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                motionSource = nearbyMotion?.source?.name,
                qualityFlags = nearbyMotion?.qualityFlags ?: 0,
                movementState = metricAccumulator.currentMovementState.name
            )
            val now = elapsedRealtime()
            _tripLogger.value = _tripLogger.value.copy(
                trip = trip.withMetrics(metricAccumulator.snapshot(), motionAccumulator.snapshot())
            )
            if (now - lastCheckpointMs >= 5_000L) {
                val metrics = metricAccumulator.snapshot()
                val checkpoint = trip.withMetrics(metrics, motionAccumulator.snapshot()).copy(
                    lastCheckpointElapsedNanos = elapsedNanos,
                    speedCoverage = metrics.speedCoverage, movementCoverage = metrics.movementCoverage
                )
                val batch = pendingTripSamples.toList()
                val chunks = takeMotionChunk(trip.id)
                val events = pendingDrivingEvents.map { it.toEntity(trip.id) }
                try {
                    withContext(Dispatchers.IO) { tripDao.checkpointTrip(checkpoint, batch, chunks, events) }
                    pendingTripSamples.clear()
                    pendingMotionSamples.clear()
                    pendingDrivingEvents.clear()
                    activeTrip = checkpoint
                    lastCheckpointMs = now
                    lastMotionChunkFlushNanos = SystemClock.elapsedRealtimeNanos()
                    _tripLogger.value = _tripLogger.value.copy(trip = checkpoint, error = null)
                } catch (e: Exception) {
                    _tripLogger.value = _tripLogger.value.copy(error = "Trip could not be saved: ${e.message ?: "storage error"}")
                    activeTrip = null
                    pendingTripSamples.clear()
                    pendingDrivingEvents.clear()
                    _tripLogger.value = _tripLogger.value.copy(trip = null)
                }
            }
        }
    }

    private suspend fun recordMotionSample(sample: MotionSample) {
        latestMotionSample = sample
        if (!_tripLogger.value.motionAvailable) {
            _tripLogger.value = _tripLogger.value.copy(motionAvailable = true)
        }
        tripMutex.withLock {
            val trip = activeTrip
            if (trip == null) {
                if (settingsRepository.automaticTripLogging.value && _tripLogger.value.carConnection == CarConnection.CONNECTED) {
                    preTripMotionBuffer += sample
                    val cutoff = sample.elapsedRealtimeNanos - 8_000_000_000L
                    preTripMotionBuffer.removeAll { it.elapsedRealtimeNanos < cutoff }
                }
                return@withLock
            }
            if (sample.elapsedRealtimeNanos < trip.startElapsedNanos) return@withLock
            val moving = when (metricAccumulator.currentMovementState) {
                MovementState.MOVING -> true
                MovementState.STOPPED -> false
                MovementState.UNKNOWN -> null
            }
            val speedState = _uiState.value
            val course = speedState.gpsBearing.toDouble().takeIf {
                trip.mountingConfirmed && speedState.isLocationFresh && speedState.isSpeedAvailable &&
                    speedState.hasLocationAccuracy && speedState.accuracy <= 25.0 && speedState.speedKmh > 3f && hasGpsBearing
            }
            val observation = toMotionObservation(sample, moving, trip.mountingConfirmed, course)
            if (!motionAccumulator.add(observation)) return@withLock
            val eventEligible = course != null && speedState.speedKmh > 10f && speedState.accuracy <= 15.0 &&
                sample.elapsedRealtimeNanos - trip.startElapsedNanos >= 2_000_000_000L &&
                sample.accuracy != android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE && sample.qualityFlags == 0
            val confidence = when {
                !eventEligible -> 0.0
                sample.accuracy >= android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> 0.95
                speedState.accuracy <= 8.0 -> 0.85
                else -> 0.7
            }
            pendingDrivingEvents += eventDetector.add(
                DrivingEventFeature(
                    elapsedRealtimeNanos = sample.elapsedRealtimeNanos,
                    forwardG = observation.forwardAccelerationMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                    lateralG = observation.lateralAccelerationMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                    speedMetersPerSecond = speedState.speedKmh.toDouble() / 3.6,
                    confidence = confidence,
                    eligible = eventEligible
                )
            )
            pendingMotionSamples += sample
            if (sample.elapsedRealtimeNanos - lastMotionChunkFlushNanos < 5_000_000_000L) return@withLock
            val speedMetrics = metricAccumulator.snapshot()
            val checkpoint = trip.withMetrics(speedMetrics, motionAccumulator.snapshot()).copy(
                lastCheckpointElapsedNanos = maxOf(trip.lastCheckpointElapsedNanos, metricAccumulator.latestTimestampNanos ?: sample.elapsedRealtimeNanos)
            )
            val chunks = takeMotionChunk(trip.id)
            val events = pendingDrivingEvents.map { it.toEntity(trip.id) }
            try {
                withContext(Dispatchers.IO) { tripDao.checkpointTrip(checkpoint, pendingTripSamples.toList(), chunks, events) }
                pendingTripSamples.clear()
                pendingMotionSamples.clear()
                pendingDrivingEvents.clear()
                activeTrip = checkpoint
                lastMotionChunkFlushNanos = sample.elapsedRealtimeNanos
                _tripLogger.value = _tripLogger.value.copy(trip = checkpoint, error = null)
            } catch (e: Exception) {
                activeTrip = null
                pendingTripSamples.clear()
                pendingMotionSamples.clear()
                pendingDrivingEvents.clear()
                _tripLogger.value = _tripLogger.value.copy(trip = null, error = "Motion data could not be saved: ${e.message ?: "storage error"}")
            }
        }
    }

    private fun takeMotionChunk(tripId: String): List<MotionChunkEntity> {
        if (pendingMotionSamples.isEmpty()) return emptyList()
        val samples = pendingMotionSamples.toList()
        return listOf(
            MotionChunkEntity(
                tripId = tripId,
                startElapsedRealtimeNanos = samples.first().elapsedRealtimeNanos,
                endElapsedRealtimeNanos = samples.last().elapsedRealtimeNanos,
                sampleCount = samples.size,
                source = samples.map { it.source.name }.distinct().joinToString(","),
                compressedSamples = MotionChunkCodec.encode(samples)
            )
        )
    }

    private fun toMotionObservation(
        sample: MotionSample,
        moving: Boolean?,
        mountingConfirmed: Boolean,
        courseDegrees: Double?
    ): MotionObservation {
        val radians = courseDegrees?.takeIf { sample.orientationValid && mountingConfirmed }?.let { Math.toRadians(it) }
        val forward = radians?.let { sample.xMetersPerSecondSquared * kotlin.math.sin(it) + sample.yMetersPerSecondSquared * kotlin.math.cos(it) }
        val lateral = radians?.let { sample.xMetersPerSecondSquared * kotlin.math.cos(it) - sample.yMetersPerSecondSquared * kotlin.math.sin(it) }
        return MotionObservation(
            elapsedRealtimeNanos = sample.elapsedRealtimeNanos,
            horizontalAccelerationMetersPerSecondSquared = sample.horizontalMetersPerSecondSquared?.toDouble(),
            verticalAccelerationMetersPerSecondSquared = sample.verticalMetersPerSecondSquared?.toDouble(),
            totalDynamicAccelerationMetersPerSecondSquared = sample.totalDynamicMetersPerSecondSquared.toDouble(),
            forwardAccelerationMetersPerSecondSquared = forward,
            lateralAccelerationMetersPerSecondSquared = lateral,
            accuracyReliable = sample.accuracy != android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE,
            qualityFlags = sample.qualityFlags,
            moving = moving
        )
    }

    private fun TripEntity.withMetrics(speed: TripMetrics, motion: MotionMetrics): TripEntity = copy(
        topSpeedMetersPerSecond = speed.topSpeedMetersPerSecond,
        distanceMeters = speed.distanceMeters,
        speedIntegralMeters = speed.speedIntegralMeters,
        speedCoveredNanos = speed.speedCoveredNanos,
        movingNanos = speed.movingNanos,
        stoppedNanos = speed.stoppedNanos,
        unknownNanos = speed.unknownNanos,
        speedCoverage = speed.speedCoverage,
        movementCoverage = speed.movementCoverage,
        maximumHorizontalG = motion.maximumHorizontalG,
        averageHorizontalG = motion.averageHorizontalG,
        movingAverageHorizontalG = motion.movingAverageHorizontalG,
        maximumVerticalG = motion.maximumVerticalG,
        maximumTotalDynamicG = motion.maximumTotalDynamicG,
        rawDiagnosticPeakG = motion.rawDiagnosticPeakG,
        horizontalGIntegralSeconds = motion.horizontalGIntegralSeconds,
        movingHorizontalGIntegralSeconds = motion.movingHorizontalGIntegralSeconds,
        validGDurationNanos = motion.validGDurationNanos,
        movingValidGDurationNanos = motion.movingValidGDurationNanos,
        totalDynamicGDurationNanos = motion.totalDynamicGDurationNanos,
        rmsHorizontalG = motion.rmsHorizontalG,
        p95HorizontalG = motion.p95HorizontalG,
        exposureAbove015GSeconds = motion.exposureAbove015GSeconds,
        exposureAbove025GSeconds = motion.exposureAbove025GSeconds,
        maximumForwardG = motion.maximumForwardG,
        maximumBrakingG = motion.maximumBrakingG,
        maximumLateralG = motion.maximumLateralG,
        maximumJerkGPerSecond = motion.maximumJerkGPerSecond
    )

    private fun DetectedDrivingEvent.toEntity(tripId: String) = DrivingEventEntity(
        tripId = tripId,
        type = type.name,
        startElapsedRealtimeNanos = startElapsedRealtimeNanos,
        endElapsedRealtimeNanos = endElapsedRealtimeNanos,
        peakValue = peakValue,
        durationNanos = durationNanos,
        speedContextMetersPerSecond = speedContextMetersPerSecond,
        confidence = confidence,
        detectorVersion = detectorVersion
    )

    private suspend fun considerAutomaticStartLocked(
        location: android.location.Location,
        observation: SpeedObservation,
        carAddress: String
    ) {
        val speed = observation.speedMetersPerSecond
        val accurateEnough = observation.accepted && speed != null &&
            location.provider != "network" &&
            ((observation.accuracyMetersPerSecond != null && observation.accuracyMetersPerSecond <= 2.5) ||
                (location.hasAccuracy() && location.accuracy <= 25f))
        if (!accurateEnough || speed <= 8.0 / 3.6) {
            autoStartCandidate.clear()
            return
        }
        val previous = autoStartCandidate.lastOrNull()?.first
        if (previous != null && observation.elapsedRealtimeNanos - previous.elapsedRealtimeNanos !in 1L..1_500_000_000L) {
            if (observation.elapsedRealtimeNanos <= previous.elapsedRealtimeNanos) return
            autoStartCandidate.clear()
        }
        val sample = TripSampleEntity(
            tripId = "", elapsedRealtimeNanos = observation.elapsedRealtimeNanos,
            wallTimeUtcMillis = observation.wallTimeUtcMillis,
            speedMetersPerSecond = speed, accuracyMetersPerSecond = observation.accuracyMetersPerSecond,
            provider = observation.provider, accepted = true
        )
        autoStartCandidate += observation to sample
        val first = autoStartCandidate.first().first
        if (observation.elapsedRealtimeNanos - first.elapsedRealtimeNanos < 5_000_000_000L) return
        val trip = TripEntity(
            id = UUID.randomUUID().toString(), startUtcMillis = first.wallTimeUtcMillis,
            startElapsedNanos = first.elapsedRealtimeNanos,
            lastCheckpointElapsedNanos = observation.elapsedRealtimeNanos,
            bootSession = "${android.os.Build.FINGERPRINT}:${System.currentTimeMillis() - SystemClock.elapsedRealtime()}",
            origin = "AUTOMATIC", carAddress = carAddress, status = TripStatus.RECORDING.name,
            routeStorageEnabled = settingsRepository.storeTripRoute.value,
            mountingConfirmed = settingsRepository.stableMountConfirmed.value
        )
        val buffered = autoStartCandidate.map { (input, row) ->
            input to row.copy(tripId = trip.id, movementState = MovementState.MOVING.name)
        }
        val accumulator = TripMetricsAccumulator().apply { start(first.elapsedRealtimeNanos) }
        val qualityAcceptedRows = buffered.map { (input, row) ->
            accumulator.add(input)
            input to row.copy(
                accepted = accumulator.lastObservationAccepted,
                movementState = accumulator.currentMovementState.name
            )
        }
        val metrics = accumulator.snapshot()
        val initialMotion = preTripMotionBuffer.filter {
            it.elapsedRealtimeNanos in first.elapsedRealtimeNanos..observation.elapsedRealtimeNanos
        }
        val gAccumulator = MotionMetricsAccumulator()
        val initialDetector = DrivingEventDetector()
        val initialEvents = mutableListOf<DetectedDrivingEvent>()
        initialMotion.forEach { sample ->
            val nearestSpeed = autoStartCandidate.minByOrNull { (_, row) -> abs(row.elapsedRealtimeNanos - sample.elapsedRealtimeNanos) }
            val course = nearestSpeed?.first?.takeIf {
                abs(it.elapsedRealtimeNanos - sample.elapsedRealtimeNanos) <= 1_000_000_000L
            }?.bearingDegrees
            val motionObservation = toMotionObservation(sample, true, trip.mountingConfirmed, course)
            gAccumulator.add(motionObservation)
            val nearestObservation = nearestSpeed?.first
            val eligible = trip.mountingConfirmed && course != null && nearestObservation?.speedMetersPerSecond?.let { it > 10.0 / 3.6 } == true &&
                (nearestObservation.accuracyMetersPerSecond == null || nearestObservation.accuracyMetersPerSecond <= 2.5) &&
                sample.elapsedRealtimeNanos - first.elapsedRealtimeNanos >= 2_000_000_000L &&
                sample.accuracy != android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE && sample.qualityFlags == 0
            initialEvents += initialDetector.add(
                DrivingEventFeature(
                    sample.elapsedRealtimeNanos,
                    motionObservation.forwardAccelerationMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                    motionObservation.lateralAccelerationMetersPerSecondSquared?.div(STANDARD_GRAVITY),
                    nearestObservation?.speedMetersPerSecond,
                    if (eligible) 0.8 else 0.0,
                    eligible
                )
            )
        }
        initialEvents += initialDetector.flush(observation.elapsedRealtimeNanos)
        val checkpoint = trip.withMetrics(metrics, gAccumulator.snapshot())
        try {
            val initialChunks = if (initialMotion.isEmpty()) emptyList() else listOf(
                MotionChunkEntity(
                    tripId = trip.id,
                    startElapsedRealtimeNanos = initialMotion.first().elapsedRealtimeNanos,
                    endElapsedRealtimeNanos = initialMotion.last().elapsedRealtimeNanos,
                    sampleCount = initialMotion.size,
                    source = initialMotion.map { it.source.name }.distinct().joinToString(","),
                    compressedSamples = MotionChunkCodec.encode(initialMotion)
                )
            )
            withContext(Dispatchers.IO) { tripDao.checkpointTrip(checkpoint, qualityAcceptedRows.map { it.second }, initialChunks, initialEvents.map { it.toEntity(trip.id) }) }
            activeTrip = checkpoint
            metricAccumulator = accumulator
            motionAccumulator = gAccumulator
            eventDetector = DrivingEventDetector()
            pendingTripSamples.clear()
            pendingMotionSamples.clear()
            pendingDrivingEvents.clear()
            preTripMotionBuffer.clear()
            lastCheckpointMs = elapsedRealtime()
            lastMotionChunkFlushNanos = observation.elapsedRealtimeNanos
            autoStartCandidate.clear()
            _tripLogger.value = _tripLogger.value.copy(trip = checkpoint, error = null)
        } catch (e: Exception) {
            autoStartCandidate.clear()
            _tripLogger.value = _tripLogger.value.copy(error = "Automatic trip could not be saved: ${e.message ?: "storage error"}")
        }
    }

    private val _activeWidgets = MutableStateFlow(
        settingsRepository.getActiveWidgets(defaultWidgets.map { it.name })
            .mapNotNull { name ->
                try {
                    WidgetType.valueOf(name)
                } catch (_: Exception) {
                    null
                }
            }
            .takeIf { it.isNotEmpty() } ?: defaultWidgets
    )
    /** StateFlow emitting the ordered list of active dashboard widgets. */
    val activeWidgets: StateFlow<List<WidgetType>> = _activeWidgets.asStateFlow()
    /** StateFlow emitting the custom grid spans configured for dashboard widgets. */
    val widgetSpans: StateFlow<Map<String, WidgetSpan>> = settingsRepository.widgetSpans

    /**
     * Updates and persists the grid span size for a widget.
     *
     * @param widget The [WidgetType] being updated.
     * @param span The new [WidgetSpan] size (HALF, FULL_WIDTH, or LARGE).
     */
    fun setWidgetSpan(widget: WidgetType, span: WidgetSpan) = settingsRepository.setWidgetSpan(widget.name, span)

    private fun saveWidgets() {
        settingsRepository.setActiveWidgets(_activeWidgets.value.map { it.name })
    }

    /**
     * Moves a widget from [from] index to [to] index in the active list and persists the new order.
     *
     * @param from The initial item index.
     * @param to The target destination index.
     */
    fun reorderWidget(from: Int, to: Int) {
        val list = _activeWidgets.value.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
            _activeWidgets.value = list
            saveWidgets()
        }
    }

    /**
     * Appends a new widget to the active dashboard and persists the change.
     *
     * @param widget The [WidgetType] to add.
     */
    fun addWidget(widget: WidgetType) {
        if (!_activeWidgets.value.contains(widget)) {
            _activeWidgets.value += widget
            saveWidgets()
        }
    }

    /**
     * Removes a widget from the active dashboard and persists the change.
     *
     * @param widget The [WidgetType] to remove.
     */
    fun removeWidget(widget: WidgetType) {
        _activeWidgets.value = _activeWidgets.value.filter { it != widget }
        saveWidgets()
    }

    /**
     * Resets the dashboard back to the factory default widgets and clears custom span sizes.
     */
    fun resetLayout() {
        _activeWidgets.value = defaultWidgets
        settingsRepository.clearLayout()
    }

    /**
     * Begins collecting location updates from the [LocationClient].
     *
     * If tracking is already active, this method does nothing. It parses the raw meters-per-second
     * speed into kilometers-per-hour (km/h) and updates the internal [MutableStateFlow].
     */
    fun startTracking() {
        if (locationJob?.isActive == true) return
        
        _uiState.value = _uiState.value.copy(isTracking = true, error = null)

        locationJob = locationClient.getLocationUpdates(500L)
            .catch { e ->
                expiryJob?.cancel()
                invalidateLocation()
                if (e is SecurityException) finishTrip(TripEndReason.PERMISSION_LOST)
                _uiState.value = _uiState.value.copy(
                    isTracking = false,
                    error = e.message ?: "Unknown error"
                )
            }
            .onEach { location ->
                expiryJob?.cancel()
                val fixElapsedRealtimeNanos = location.elapsedRealtimeNanos.takeIf { it > 0L }
                    ?: (elapsedRealtime() * 1_000_000L)
                val fixTime = fixElapsedRealtimeNanos / 1_000_000L
                val previousFix = latestLocationFixElapsedRealtimeNanos
                if (previousFix == null || fixElapsedRealtimeNanos >= previousFix) {
                    latestLocationFixElapsedRealtimeNanos = fixElapsedRealtimeNanos
                }
                val remainingMs = 5_000L - (elapsedRealtime() - fixTime).coerceAtLeast(0L)
                if (remainingMs <= 0L) {
                    invalidateLocation()
                    markLocationExpired(
                        fixElapsedRealtimeNanos,
                        maxOf(fixElapsedRealtimeNanos + 5_000_000_000L, (elapsedRealtime() * 1_000_000L))
                    )
                    return@onEach
                }
                val speedMs = if (location.hasSpeed()) location.speed else 0f
                val speedKmh = SpeedConverter.msToKmh(speedMs)
                recordTripLocation(location, if (location.hasSpeed()) speedMs.toDouble() else null)
                
                val newGpsBearing = if (location.hasBearing()) location.bearing else _uiState.value.gpsBearing
                hasGpsBearing = location.hasBearing()
                val activeHeading = if (speedKmh > 3.0f && hasGpsBearing) newGpsBearing else _uiState.value.compassAzimuth

                _uiState.value = _uiState.value.copy(
                    speedKmh = speedKmh,
                    isLocationFresh = true,
                    isSpeedAvailable = location.hasSpeed(),
                    hasLocationAccuracy = location.hasAccuracy(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    gpsBearing = newGpsBearing,
                    heading = activeHeading,
                    altitude = if (location.hasAltitude()) location.altitude else 0.0,
                    accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else 0.0,
                    provider = location.provider ?: "Unknown"
                )
                expiryJob = scope.launch {
                    delay(remainingMs.milliseconds)
                    if (latestLocationFixElapsedRealtimeNanos == fixElapsedRealtimeNanos) {
                        invalidateLocation()
                        markLocationExpired(fixElapsedRealtimeNanos, fixElapsedRealtimeNanos + 5_000_000_000L)
                    }
                }
            }
            .launchIn(scope)

        if (sensorJob?.isActive == true) return
        sensorJob = sensorClient.getSensorUpdates()
            .onEach { sensorData ->
                val activeHeading = if (useGpsHeading()) _uiState.value.gpsBearing else sensorData.azimuth
                
                _uiState.value = _uiState.value.copy(
                    accelX = sensorData.accelerometer[0],
                    accelY = sensorData.accelerometer[1],
                    accelZ = sensorData.accelerometer[2],
                    gyroX = sensorData.gyroscope[0],
                    gyroY = sensorData.gyroscope[1],
                    gyroZ = sensorData.gyroscope[2],
                    magX = sensorData.magneticField[0],
                    magY = sensorData.magneticField[1],
                    magZ = sensorData.magneticField[2],
                    compassAzimuth = sensorData.azimuth,
                    heading = activeHeading
                )
            }
            .launchIn(scope)
    }

    fun stopTracking() {
        locationJob?.cancel()
        locationJob = null
        sensorJob?.cancel()
        sensorJob = null
        expiryJob?.cancel()
        expiryJob = null
        invalidateLocation()
        _uiState.value = _uiState.value.copy(isTracking = false)
    }
}

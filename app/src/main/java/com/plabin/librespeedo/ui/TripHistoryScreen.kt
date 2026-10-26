package com.plabin.librespeedo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.plabin.librespeedo.trips.TripEntity
import com.plabin.librespeedo.trips.TripSampleEntity
import com.plabin.librespeedo.trips.DrivingEventEntity
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripHistoryScreen(
    trips: List<TripEntity>,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    loadSamples: suspend (String) -> List<TripSampleEntity>,
    loadEvents: suspend (String) -> List<DrivingEventEntity>
) {
    var selected by remember { mutableStateOf<TripEntity?>(null) }
    var samples by remember { mutableStateOf(emptyList<TripSampleEntity>()) }
    var events by remember { mutableStateOf(emptyList<DrivingEventEntity>()) }
    LaunchedEffect(selected?.id) {
        samples = emptyList()
        events = emptyList()
        selected?.let {
            samples = loadSamples(it.id)
            events = loadEvents(it.id)
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(selected?.let { "Trip details" } ?: "Trip history") },
            navigationIcon = { IconButton(onClick = { if (selected != null) selected = null else onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
        )
    }) { padding ->
        if (selected != null) {
            TripDetails(selected!!, samples, events, Modifier.padding(padding))
        } else if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("No trips saved yet") }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(trips, key = { it.id }) { trip ->
                    Card(Modifier.fillMaxWidth().clickable { selected = trip }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(trip.startUtcMillis)), style = MaterialTheme.typography.titleSmall)
                                Text("${trip.status.lowercase().replaceFirstChar { it.uppercase() }} · ${formatDuration(trip.endElapsedNanos?.minus(trip.startElapsedNanos) ?: (trip.lastCheckpointElapsedNanos - trip.startElapsedNanos))}", style = MaterialTheme.typography.bodySmall)
                                Text("${formatDistance(trip.distanceMeters)} · ${trip.endReason?.lowercase()?.replace('_', ' ') ?: trip.origin.lowercase()}", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDelete(trip.id) }) { Icon(Icons.Default.Delete, "Delete trip") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripDetails(
    trip: TripEntity,
    samples: List<TripSampleEntity>,
    events: List<DrivingEventEntity>,
    modifier: Modifier = Modifier
) {
    val duration = (trip.endElapsedNanos?.minus(trip.startElapsedNanos)
        ?: trip.lastCheckpointElapsedNanos - trip.startElapsedNanos).coerceAtLeast(0L)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT).format(Date(trip.startUtcMillis)), style = MaterialTheme.typography.titleMedium)
        Text("${trip.status} · ${trip.endReason ?: trip.origin}", style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
        MetricRow("Top speed", trip.topSpeedMetersPerSecond?.let { "${"%.1f".format(it * 3.6)} km/h" } ?: "Unavailable")
        MetricRow("Average speed", if (trip.speedCoveredNanos > 0) "${"%.1f".format(trip.speedIntegralMeters / (trip.speedCoveredNanos / 1e9) * 3.6)} km/h" else "Unavailable")
        MetricRow("Distance", formatDistance(trip.distanceMeters))
        MetricRow("Trip time", formatDuration(duration))
        MetricRow("Time stopped", formatDuration(trip.stoppedNanos))
        MetricRow("Maximum horizontal G", trip.maximumHorizontalG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable · no valid orientation samples")
        MetricRow("Average horizontal G", trip.averageHorizontalG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable · insufficient sensor coverage")
        MetricRow("Moving average G", trip.movingAverageHorizontalG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("Maximum vertical G", trip.maximumVerticalG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("Maximum total dynamic G", trip.maximumTotalDynamicG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("Forward acceleration peak", trip.maximumForwardG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable · confirm a stable mount and course")
        MetricRow("Braking peak", trip.maximumBrakingG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("Lateral peak", trip.maximumLateralG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("95th percentile G", trip.p95HorizontalG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("RMS G", trip.rmsHorizontalG?.let { "${"%.2f".format(it)} G" } ?: "Unavailable")
        MetricRow("Exposure above 0.15 G", "${"%.1f".format(trip.exposureAbove015GSeconds)} s")
        MetricRow("Peak jerk", trip.maximumJerkGPerSecond?.let { "${"%.2f".format(it)} G/s" } ?: "Unavailable")
        MetricRow("Speed coverage", "${(trip.speedCoverage * 100).toInt()}%")
        MetricRow("Movement coverage", "${(trip.movementCoverage * 100).toInt()}%")
        MetricRow("Motion coverage", "${if (duration > 0L) (trip.validGDurationNanos.toDouble() / duration * 100).toInt().coerceIn(0, 100) else 0}%")
        MetricRow("Phone mount", if (trip.mountingConfirmed) "Confirmed" else "Not confirmed")
        if (trip.routeStorageEnabled) MetricRow("Route storage", "Enabled for this trip")
        if (samples.isNotEmpty()) {
            HorizontalDivider()
            Text("Speed profile", style = MaterialTheme.typography.titleSmall)
            TripSparkline(samples, "km/h", value = { it.speedMetersPerSecond?.times(3.6) }, accepted = { it.accepted && it.speedMetersPerSecond != null }, maxGapNanos = 3_000_000_000L)
            Text("Horizontal G profile", style = MaterialTheme.typography.titleSmall)
            TripSparkline(samples, "G", value = { it.horizontalG }, accepted = { it.horizontalG != null }, maxGapNanos = 3_000_000_000L)
        }
        HorizontalDivider()
        Text("Detected maneuvers", style = MaterialTheme.typography.titleSmall)
        if (events.isEmpty()) Text("No sustained, high-confidence maneuver events were detected.", style = MaterialTheme.typography.bodySmall)
        events.forEach { event ->
            val title = when (event.type) {
                "HARD_ACCELERATION" -> "Harsh acceleration"
                "HARSH_BRAKING" -> "Harsh braking"
                "HARD_CORNERING" -> "Hard cornering"
                "HIGH_JERK" -> "High jerk"
                else -> event.type.lowercase().replace('_', ' ')
            }
            val unit = if (event.type == "HIGH_JERK") "G/s" else "G"
            Text("$title · ${"%.2f".format(event.peakValue)} $unit · ${"%.1f".format(event.durationNanos / 1e9)} s · ${"%.0f".format(event.confidence * 100)}% confidence")
        }
        if (trip.status == "INTERRUPTED") Text("This trip ended unexpectedly. Its duration stops at the last saved checkpoint.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun TripSparkline(
    samples: List<TripSampleEntity>,
    unit: String,
    value: (TripSampleEntity) -> Double?,
    accepted: (TripSampleEntity) -> Boolean,
    maxGapNanos: Long
) {
    val points = samples.map(value).filterNotNull().filter { it.isFinite() }
    val low = points.minOrNull() ?: 0.0
    val high = points.maxOrNull() ?: 0.0
    val lineColor = MaterialTheme.colorScheme.primary
    Text(if (points.isEmpty()) "No valid $unit samples" else "${"%.1f".format(low)}–${"%.1f".format(high)} $unit", style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        if (samples.size < 2 || points.isEmpty()) return@Canvas
        val span = (high - low).takeIf { it > 1e-9 } ?: 1.0
        val path = Path()
        var hasPoint = false
        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val previousValue = value(previous)
            val currentValue = value(current)
            val valid = accepted(previous) && accepted(current) && previousValue != null && currentValue != null &&
                current.elapsedRealtimeNanos - previous.elapsedRealtimeNanos in 1L..maxGapNanos
            if (!valid) { hasPoint = false; continue }
            val x0 = (index - 1).toFloat() / (samples.size - 1) * size.width
            val x1 = index.toFloat() / (samples.size - 1) * size.width
            val y0 = size.height - (((previousValue!! - low) / span).toFloat() * size.height)
            val y1 = size.height - (((currentValue!! - low) / span).toFloat() * size.height)
            if (!hasPoint) path.moveTo(x0, y0) else path.lineTo(x0, y0)
            path.lineTo(x1, y1)
            hasPoint = true
        }
        drawPath(path, lineColor, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun formatDuration(nanos: Long): String {
    val seconds = (nanos.coerceAtLeast(0L) / 1_000_000_000L)
    return "%02d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}

private fun formatDistance(meters: Double): String = if (meters < 1000) "${"%.0f".format(meters)} m" else "${"%.2f".format(meters / 1000)} km"

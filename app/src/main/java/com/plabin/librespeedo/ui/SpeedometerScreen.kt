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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme

/**
 * The main UI screen displaying the speedometer, compass, and GPS debug panel.
 * 
 * @param uiState The current data state emitted by the ViewModel.
 * @param modifier An optional [Modifier] to configure the layout of the root column.
 */
@Composable
fun SpeedometerScreen(
    uiState: SpeedometerUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Text(text = "LibreSpeedo (Proto)", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(32.dp))

        // Compass Prototype
        CompassView(heading = uiState.heading)

        // Speed Display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%.1f", uiState.speedKmh),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "km/h",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // Current GPS Position
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Position",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
            Text(
                text = String.format("Lat: %.5f", uiState.latitude),
                fontSize = 16.sp
            )
            Text(
                text = String.format("Lng: %.5f", uiState.longitude),
                fontSize = 16.sp
            )
        }

        // Debug Panel
        DebugPanel(
            provider = uiState.provider,
            speed = uiState.speedKmh,
            latitude = uiState.latitude,
            longitude = uiState.longitude,
            heading = uiState.heading,
            altitude = uiState.altitude,
            accuracy = uiState.accuracy,
            error = uiState.error
        )
    }
}

/**
 * A simple compass visualizer that rotates an arrow icon based on the current heading.
 *
 * @param heading The current heading in degrees (0 = North).
 */
@Composable
fun CompassView(heading: Float) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↑",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(heading)
        )
        // North indicator for the debug prototype
        Text(
            text = "N",
            modifier = Modifier.align(Alignment.TopCenter).padding(4.dp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * A debug card displaying raw metrics from the hardware sensors.
 * Useful for validating the active location provider and sensor accuracy.
 */
@Composable
fun DebugPanel(
    provider: String,
    speed: Float,
    latitude: Double,
    longitude: Double,
    heading: Float,
    altitude: Double,
    accuracy: Double,
    error: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Debug Info", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (error != null) {
                Text(text = "Error: $error", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            DebugRow("Provider", provider)
            DebugRow("Raw Speed", "$speed km/h")
            DebugRow("Coordinates", "$latitude, $longitude")
            DebugRow("Heading", "$heading°")
            DebugRow("Altitude", "$altitude m")
            DebugRow("Accuracy", "±$accuracy m")
        }
    }
}

@Composable
fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun SpeedometerScreenPreview() {
    LibreSpeedoTheme {
        SpeedometerScreen(
            uiState = SpeedometerUiState(
                speedKmh = 42.5f,
                latitude = 40.7128,
                longitude = -74.0060,
                heading = 45f,
                altitude = 10.5,
                accuracy = 3.2,
                provider = "Preview"
            )
        )
    }
}

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
package com.plabin.librespeedo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.SpeedometerUiState

/**
 * Renders live diagnostics including hardware sensor vectors, GNSS accuracy, altitude, and provider.
 *
 * Automatically enables vertical scrolling when rendered inside compact card spans.
 *
 * @param uiState Current sensor and location metrics.
 * @param span Current card span sizing.
 * @param modifier Optional layout modifier.
 */
@Composable
fun DebugComponent(
    uiState: SpeedometerUiState,
    span: WidgetSpan = WidgetSpan.LARGE,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (span != WidgetSpan.LARGE) Modifier.verticalScroll(rememberScrollState()) else Modifier)
    ) {
        Text(
            text = "Debug Info",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = if (span == WidgetSpan.HALF) 12.sp else 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (uiState.error != null) {
            Text(
                text = "Error: ${uiState.error}",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
        DebugRow("Provider", uiState.provider, span)
        DebugRow("Raw Speed", "${uiState.speedKmh} km/h", span)
        DebugRow("Coordinates", "${uiState.latitude}, ${uiState.longitude}", span)
        DebugRow("Heading", "${uiState.heading}°", span)
        DebugRow("Altitude", "${uiState.altitude} m", span)
        DebugRow("Accuracy", "±${uiState.accuracy} m", span)
        DebugRow(
            "Accelerometer",
            String.format(
                LocalLocale.current.platformLocale,
                "%.2f, %.2f, %.2f",
                uiState.accelX,
                uiState.accelY,
                uiState.accelZ
            ),
            span
        )
        DebugRow(
            "Gyroscope",
            String.format(
                LocalLocale.current.platformLocale,
                "%.2f, %.2f, %.2f",
                uiState.gyroX,
                uiState.gyroY,
                uiState.gyroZ
            ),
            span
        )
        DebugRow(
            "Magnetic Field",
            String.format(
                LocalLocale.current.platformLocale,
                "%.2f, %.2f, %.2f",
                uiState.magX,
                uiState.magY,
                uiState.magZ
            ),
            span
        )
    }
}

/**
 * A single row within the [DebugComponent] showing a label and value pair.
 *
 * @param label The metric name.
 * @param value The formatted metric readout.
 * @param span Current card span sizing for font sizing.
 */
@Composable
fun DebugRow(label: String, value: String, span: WidgetSpan = WidgetSpan.LARGE) {
    val fontSize = if (span == WidgetSpan.HALF) 10.sp else 12.sp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = fontSize,
            maxLines = 1
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

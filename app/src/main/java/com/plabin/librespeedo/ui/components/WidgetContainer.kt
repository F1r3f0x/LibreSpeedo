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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.SpeedometerUiState
import com.plabin.librespeedo.ui.WidgetType

/**
 * Container card for an individual dashboard widget.
 *
 * Provides elevated Material surface, edit-mode action controls (resize button, unit settings,
 * remove button), and active span badge.
 *
 * @param widget The [WidgetType] to display.
 * @param span The active [WidgetSpan] size of the widget.
 * @param uiState Current sensor and location metrics.
 * @param isEditMode True if editing actions (remove, resize, config) should be visible.
 * @param speedUnit Active speed measurement unit.
 * @param onRemove Callback to remove this widget from the active dashboard.
 * @param onToggleSpan Callback to cycle this widget to the next span size.
 * @param onSpeedUnitChange Callback to update the speedometer's unit preference.
 * @param elevation Dynamic elevation applied to the card (e.g. higher when dragged).
 * @param modifier Optional layout modifier.
 */
@Composable
fun WidgetContainer(
    widget: WidgetType,
    span: WidgetSpan,
    uiState: SpeedometerUiState,
    isEditMode: Boolean,
    speedUnit: SpeedUnit,
    onRemove: () -> Unit,
    onToggleSpan: () -> Unit,
    onSpeedUnitChange: (SpeedUnit) -> Unit,
    elevation: Dp,
    modifier: Modifier = Modifier
) {
    var showSpeedSettings by remember { mutableStateOf(false) }

    if (showSpeedSettings && widget == WidgetType.SPEEDOMETER) {
        AlertDialog(
            onDismissRequest = { showSpeedSettings = false },
            title = { Text("Speedometer Settings") },
            text = {
                Column {
                    Text("Select Unit:")
                    Spacer(modifier = Modifier.height(8.dp))
                    SpeedUnit.entries.forEach { unit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = speedUnit == unit,
                                onClick = { onSpeedUnitChange(unit) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = unit.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedSettings = false }) {
                    Text("Done")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isEditMode) {
                Surface(
                    shape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = span.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleSpan) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Resize Widget (${span.label})"
                        )
                    }
                    if (widget == WidgetType.SPEEDOMETER) {
                        IconButton(onClick = { showSpeedSettings = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Widget Settings"
                            )
                        }
                    }
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Widget"
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isEditMode) PaddingValues(top = 28.dp, start = 12.dp, end = 12.dp, bottom = 12.dp) else PaddingValues(16.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (widget) {
                    WidgetType.SPEEDOMETER -> SpeedComponent(uiState, speedUnit, span)
                    WidgetType.COMPASS -> CompassComponent(uiState.heading, span)
                    WidgetType.ANALOG_COMPASS -> AnalogCompassComponent(uiState.heading, span)
                    WidgetType.CHRONOMETER -> ChronometerComponent(span)
                    WidgetType.POSITION -> PositionComponent(uiState, span)
                    WidgetType.DEBUG -> DebugComponent(uiState, span)
                    WidgetType.HELLO_WORLD -> HelloWorldComponent()
                }
            }
        }
    }
}

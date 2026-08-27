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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.WidgetSpan
import kotlinx.coroutines.delay

/**
 * Formats a duration in milliseconds into a readable stopwatch format `HH:MM:SS.SS` or `MM:SS.SS`.
 */
fun formatChronometerTime(totalMillis: Long): String {
    val millis = (totalMillis % 1000) / 10
    val totalSeconds = totalMillis / 1000
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60

    return if (hours > 0) {
        String.format(java.util.Locale.US, "%02d:%02d:%02d.%02d", hours, minutes, seconds, millis)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
    }
}

/**
 * A full-featured digital stopwatch and chronometer dashboard component.
 *
 * Supports starting, pausing, resetting, and recording split lap times with responsive typography.
 *
 * @param span Active widget layout span for adaptive sizing.
 * @param modifier Optional layout modifier.
 */
@Composable
fun ChronometerComponent(
    span: WidgetSpan = WidgetSpan.FULL_WIDTH,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var laps by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedMillis
            while (isRunning) {
                elapsedMillis = System.currentTimeMillis() - startTime
                delay(30L)
            }
        }
    }

    val timeFontSize = when (span) {
        WidgetSpan.HALF -> 26.sp
        WidgetSpan.FULL_WIDTH -> 38.sp
        WidgetSpan.LARGE -> 48.sp
    }

    val formattedTime = formatChronometerTime(elapsedMillis)

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chronometer",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = formattedTime,
            fontSize = timeFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )

        if (laps.isNotEmpty() && span != WidgetSpan.HALF) {
            Text(
                text = "Last Lap: ${formatChronometerTime(laps.last())} (${laps.size} laps)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start / Pause Button
            IconButton(
                onClick = { isRunning = !isRunning },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause Chronometer" else "Start Chronometer",
                    tint = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Lap Button
            if (isRunning && span != WidgetSpan.HALF) {
                IconButton(
                    onClick = { laps = laps + elapsedMillis },
                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Record Lap"
                    )
                }
            }

            // Reset Button
            IconButton(
                onClick = {
                    isRunning = false
                    elapsedMillis = 0L
                    laps = emptyList()
                },
                enabled = elapsedMillis > 0L || isRunning,
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Chronometer"
                )
            }
        }
    }
}

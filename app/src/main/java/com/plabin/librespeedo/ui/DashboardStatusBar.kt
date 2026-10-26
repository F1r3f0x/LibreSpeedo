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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.plabin.librespeedo.R

/** Live dashboard indicators; movement requires a fresh speed fix above 3 km/h. */
@Composable
internal fun DashboardStatusBar(
    uiState: SpeedometerUiState,
    isRecording: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val hasSpeed = uiState.isLocationFresh && uiState.isSpeedAvailable
    val isMoving = hasSpeed && uiState.speedKmh > 3f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape)) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.app_icon_description),
                    modifier = Modifier.matchParentSize()
                )
            }
            StatusIndicator(
                icon = if (uiState.isLocationFresh) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                description = stringResource(if (uiState.isLocationFresh) R.string.status_gps_connected else R.string.status_gps_disconnected),
                active = uiState.isLocationFresh,
                tint = colors.primary,
                container = colors.primaryContainer
            )
            StatusIndicator(
                icon = when {
                    isMoving -> Icons.Default.Navigation
                    hasSpeed -> Icons.Default.Pause
                    else -> Icons.AutoMirrored.Filled.HelpOutline
                },
                description = stringResource(when {
                    isMoving -> R.string.status_moving
                    hasSpeed -> R.string.status_stationary
                    else -> R.string.status_movement_unknown
                }),
                active = isMoving,
                tint = colors.tertiary,
                container = colors.tertiaryContainer
            )
            StatusIndicator(
                icon = if (isRecording) Icons.Default.FiberManualRecord else Icons.Default.RadioButtonUnchecked,
                description = stringResource(if (isRecording) R.string.status_recording else R.string.status_not_recording),
                active = isRecording,
                tint = colors.error,
                container = colors.errorContainer
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusIndicator(
    icon: ImageVector,
    description: String,
    active: Boolean,
    tint: Color,
    container: Color
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(description) } },
        state = rememberTooltipState()
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = if (active) container else MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp))
            }
        }
    }
}

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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.DigitalSpeedometerStyle
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.SpeedometerUiState
import com.plabin.librespeedo.utils.SpeedConverter

/**
 * Renders the high-contrast digital speedometer readout.
 *
 * Automatically converts raw speed to the selected [SpeedUnit] and dynamically scales
 * typography according to the active [WidgetSpan].
 *
 * @param uiState Current speedometer UI state with raw speed metrics.
 * @param speedUnit Active unit (KMH, MPH, MS).
 * @param span Current card span sizing.
 * @param modifier Optional modifier for layout customization.
 * @param style Saved appearance of the numerical readout.
 */
@Composable
fun SpeedComponent(
    uiState: SpeedometerUiState,
    speedUnit: SpeedUnit,
    modifier: Modifier = Modifier,
    span: WidgetSpan = WidgetSpan.FULL_WIDTH,
    style: DigitalSpeedometerStyle = DigitalSpeedometerStyle.STANDARD
) {
    val speedRawMs = uiState.speedKmh / 3.6f

    val displaySpeed = when (speedUnit) {
        SpeedUnit.KMH -> uiState.speedKmh
        SpeedUnit.MPH -> SpeedConverter.msToMph(speedRawMs)
        SpeedUnit.MS -> speedRawMs
    }

    val displayUnitStr = when (speedUnit) {
        SpeedUnit.KMH -> "km/h"
        SpeedUnit.MPH -> "mph"
        SpeedUnit.MS -> "m/s"
    }

    val speedFontSize = when (span) {
        WidgetSpan.HALF -> 44.sp
        WidgetSpan.FULL_WIDTH -> 72.sp
        WidgetSpan.LARGE -> 84.sp
    }

    val unitFontSize = when (span) {
        WidgetSpan.HALF -> 16.sp
        WidgetSpan.FULL_WIDTH -> 22.sp
        WidgetSpan.LARGE -> 26.sp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val reading = if (uiState.isLocationFresh && uiState.isSpeedAvailable) {
            String.format(LocalLocale.current.platformLocale, "%.1f", displaySpeed)
        } else "—"
        if (style == DigitalSpeedometerStyle.SEVEN_SEGMENT) {
            SevenSegmentReadout(reading, Modifier.fillMaxWidth().height(speedFontSize.value.dp))
            Spacer(Modifier.height(4.dp))
        } else Text(
            text = reading,
            fontSize = speedFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
        Text(
            text = displayUnitStr,
            fontSize = unitFontSize,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

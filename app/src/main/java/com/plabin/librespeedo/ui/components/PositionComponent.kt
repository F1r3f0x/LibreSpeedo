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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.SpeedometerUiState

/**
 * Renders current geographic position (latitude and longitude).
 *
 * @param uiState State containing current latitude and longitude.
 * @param span Current card span sizing for responsive font adjustments.
 * @param modifier Optional layout modifier.
 */
@Composable
fun PositionComponent(
    uiState: SpeedometerUiState,
    span: WidgetSpan = WidgetSpan.HALF,
    modifier: Modifier = Modifier
) {
    val titleSize = if (span == WidgetSpan.HALF) 13.sp else 16.sp
    val coordSize = if (span == WidgetSpan.HALF) 12.sp else 15.sp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Current Position",
            fontWeight = FontWeight.SemiBold,
            fontSize = titleSize,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(LocalLocale.current.platformLocale, "Lat: %.5f", uiState.latitude),
            fontSize = coordSize,
            maxLines = 1
        )
        Text(
            text = String.format(LocalLocale.current.platformLocale, "Lng: %.5f", uiState.longitude),
            fontSize = coordSize,
            maxLines = 1
        )
    }
}

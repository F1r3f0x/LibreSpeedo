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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.WidgetSpan

/**
 * Renders a high-contrast digital compass needle indicating current heading.
 *
 * @param heading Heading in degrees where 0 is North.
 * @param span Current card span sizing.
 * @param modifier Optional modifier for layout customization.
 */
@Composable
fun CompassComponent(
    heading: Float,
    span: WidgetSpan = WidgetSpan.HALF,
    modifier: Modifier = Modifier
) {
    val dialSize = when (span) {
        WidgetSpan.HALF -> 95.dp
        WidgetSpan.FULL_WIDTH -> 115.dp
        WidgetSpan.LARGE -> 140.dp
    }
    val arrowSize = when (span) {
        WidgetSpan.HALF -> 36.sp
        WidgetSpan.FULL_WIDTH -> 44.sp
        WidgetSpan.LARGE -> 54.sp
    }

    Box(
        modifier = modifier
            .size(dialSize)
            .background(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "↑",
            fontSize = arrowSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(-heading)
        )
        Text(
            text = "N",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(4.dp),
            fontWeight = FontWeight.Bold,
            fontSize = if (span == WidgetSpan.HALF) 11.sp else 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

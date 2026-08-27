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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.WidgetSpan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Returns the cardinal/intercardinal direction name for a given degree heading.
 */
fun getCardinalDirection(degrees: Float): String {
    val normalized = ((degrees % 360f) + 360f) % 360f
    return when (normalized) {
        in 337.5..360.0, in 0.0..<22.5 -> "N"
        in 22.5..<67.5 -> "NE"
        in 67.5..<112.5 -> "E"
        in 112.5..<157.5 -> "SE"
        in 157.5..<202.5 -> "S"
        in 202.5..<247.5 -> "SW"
        in 247.5..<292.5 -> "W"
        in 292.5..<337.5 -> "NW"
        else -> "N"
    }
}

/**
 * Renders an animated analog compass instrument dial with realistic compass rose,
 * tick markers, cardinal indicators, and a dual-tone needle.
 *
 * @param heading Current fused bearing/azimuth in degrees (0 = North).
 * @param span Active widget layout span for adaptive sizing.
 * @param modifier Optional layout modifier.
 */
@Composable
fun AnalogCompassComponent(
    heading: Float,
    span: WidgetSpan = WidgetSpan.FULL_WIDTH,
    modifier: Modifier = Modifier
) {
    var accumulatedAngle by remember { mutableFloatStateOf(heading) }

    LaunchedEffect(heading) {
        val diff = (heading - accumulatedAngle) % 360f
        val shortestDiff = when {
            diff > 180f -> diff - 360f
            diff < -180f -> diff + 360f
            else -> diff
        }
        accumulatedAngle += shortestDiff
    }

    val animatedHeading by animateFloatAsState(
        targetValue = accumulatedAngle,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "AnalogCompassAnimation"
    )

    val dialSize = when (span) {
        WidgetSpan.HALF -> 110.dp
        WidgetSpan.FULL_WIDTH -> 150.dp
        WidgetSpan.LARGE -> 200.dp
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant
    val northNeedleColor = Color(0xFFE53935)
    val southNeedleColor = Color(0xFFB0BEC5)

    val normalizedHeading = ((heading % 360f) + 360f) % 360f
    val cardinalText = getCardinalDirection(normalizedHeading)

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 6.dp.toPx()

                // Draw outer dial ring
                drawCircle(
                    color = surfaceColor,
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = onSurfaceColor.copy(alpha = 0.3f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Draw tick marks and cardinal marks
                for (deg in 0 until 360 step 15) {
                    val angleRad = Math.toRadians((deg - 90).toDouble())
                    val isMajor = deg % 90 == 0
                    val isSemiMajor = deg % 45 == 0

                    val tickLength = when {
                        isMajor -> 10.dp.toPx()
                        isSemiMajor -> 6.dp.toPx()
                        else -> 3.dp.toPx()
                    }

                    val tickColor = if (isMajor) primaryColor else onSurfaceColor.copy(alpha = 0.6f)
                    val tickWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                    val startX = (center.x + (radius - tickLength) * cos(angleRad)).toFloat()
                    val startY = (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                    val endX = (center.x + radius * cos(angleRad)).toFloat()
                    val endY = (center.y + radius * sin(angleRad)).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth
                    )
                }

                // Draw rotating compass needle
                rotate(degrees = -animatedHeading, pivot = center) {
                    val needleLength = radius * 0.72f
                    val needleWidth = radius * 0.15f

                    // North Needle (Red pointed triangle)
                    val northPath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x - needleWidth, center.y)
                        lineTo(center.x + needleWidth, center.y)
                        close()
                    }
                    drawPath(path = northPath, color = northNeedleColor)

                    // South Needle (Silver/Grey pointed triangle)
                    val southPath = Path().apply {
                        moveTo(center.x, center.y + needleLength)
                        lineTo(center.x - needleWidth, center.y)
                        lineTo(center.x + needleWidth, center.y)
                        close()
                    }
                    drawPath(path = southPath, color = southNeedleColor)

                    // Center Pivot Pin
                    drawCircle(
                        color = Color.White,
                        radius = 5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 3.dp.toPx(),
                        center = center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${normalizedHeading.roundToInt()}°",
                fontWeight = FontWeight.Bold,
                fontSize = if (span == WidgetSpan.HALF) 13.sp else 16.sp,
                color = primaryColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = cardinalText,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (span == WidgetSpan.HALF) 12.sp else 15.sp,
                color = secondaryColor
            )
        }
    }
}

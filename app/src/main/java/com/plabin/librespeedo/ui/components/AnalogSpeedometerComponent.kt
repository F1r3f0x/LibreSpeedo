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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.AnalogSpeedometerStyle
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.SpeedometerUiState
import com.plabin.librespeedo.utils.SpeedConverter
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.sin

/**
 * Returns the gauge maximum speed limit configuration for a given [SpeedUnit] and current speed.
 */
fun getSpeedometerMaxScale(speedUnit: SpeedUnit, currentSpeed: Float): Float {
    val defaultMax = when (speedUnit) {
        SpeedUnit.KMH -> 240f
        SpeedUnit.MPH -> 140f
        SpeedUnit.MS -> 60f
    }
    return if (currentSpeed > defaultMax) {
        val step = when (speedUnit) {
            SpeedUnit.KMH -> 40f
            SpeedUnit.MPH -> 20f
            SpeedUnit.MS -> 10f
        }
        (defaultMax + ((currentSpeed - defaultMax) / step + 1).toInt() * step)
    } else {
        defaultMax
    }
}

/**
 * Renders an animated analog automotive speedometer gauge with sweeping needle,
 * graduated tick marks, progress arc, and digital speed readout.
 *
 * @param uiState Current speedometer UI state with raw speed metrics.
 * @param speedUnit Active unit (KMH, MPH, MS).
 * @param modifier Optional layout modifier.
 * @param span Active widget layout span for adaptive sizing.
 * @param style Saved visual preset for the gauge.
 */
@Composable
fun AnalogSpeedometerComponent(
    uiState: SpeedometerUiState,
    speedUnit: SpeedUnit,
    modifier: Modifier = Modifier,
    span: WidgetSpan = WidgetSpan.FULL_WIDTH,
    style: AnalogSpeedometerStyle = AnalogSpeedometerStyle.CLASSIC
) {
    val speedAvailable = uiState.isLocationFresh && uiState.isSpeedAvailable
    val speedRawMs = uiState.speedKmh / 3.6f

    val displaySpeed = if (!speedAvailable) 0f else when (speedUnit) {
        SpeedUnit.KMH -> max(0f, uiState.speedKmh)
        SpeedUnit.MPH -> max(0f, SpeedConverter.msToMph(speedRawMs))
        SpeedUnit.MS -> max(0f, speedRawMs)
    }

    val displayUnitStr = when (speedUnit) {
        SpeedUnit.KMH -> "km/h"
        SpeedUnit.MPH -> "mph"
        SpeedUnit.MS -> "m/s"
    }

    val maxScale = getSpeedometerMaxScale(speedUnit, displaySpeed)
    val startAngle = 135f
    val sweepAngle = 270f
    val speedFraction = (displaySpeed / maxScale).coerceIn(0f, 1f)
    val targetNeedleAngle = startAngle + (speedFraction * sweepAngle)

    val animatedNeedleAngle by animateFloatAsState(
        targetValue = targetNeedleAngle,
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "AnalogSpeedometerNeedle"
    )

    val dialSize = when (span) {
        WidgetSpan.HALF -> 88.dp
        WidgetSpan.FULL_WIDTH -> 110.dp
        WidgetSpan.LARGE -> 175.dp
    }

    if (style == AnalogSpeedometerStyle.CAT) {
        CatSpeedometerDial(
            speedAvailable = speedAvailable,
            displaySpeed = displaySpeed,
            unit = displayUnitStr,
            maxScale = maxScale,
            animatedAngle = animatedNeedleAngle,
            dialSize = dialSize,
            modifier = modifier
        )
        return
    }

    val isSport = style == AnalogSpeedometerStyle.SPORT
    val isMinimal = style == AnalogSpeedometerStyle.MINIMAL
    val primaryColor = when {
        isSport -> Color(0xFFFFBE55)
        isMinimal -> Color(0xFFE4F3F5)
        else -> MaterialTheme.colorScheme.primary
    }
    val secondaryColor = when {
        isSport -> Color(0xFFFF8A50)
        isMinimal -> Color(0xFF94AEB4)
        else -> MaterialTheme.colorScheme.secondary
    }
    val tertiaryColor = if (isSport) Color(0xFFFF4D4D) else MaterialTheme.colorScheme.tertiary
    val trackBackgroundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val needleColor = if (isMinimal) primaryColor else Color(0xFFE53935)
    val hubColor = if (isSport) Color(0xFF151C26) else MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(dialSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().semantics {
                contentDescription = if (speedAvailable) "Speed gauge" else "Speed unavailable"
            }) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val strokeWidth = if (isMinimal) 1.dp.toPx() else when (span) {
                    WidgetSpan.HALF -> 4.dp.toPx()
                    WidgetSpan.FULL_WIDTH -> 6.dp.toPx()
                    WidgetSpan.LARGE -> 8.dp.toPx()
                }
                val arcRadius = (size.minDimension / 2f) - (strokeWidth / 2f) - 4.dp.toPx()
                val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2, arcRadius * 2)

                if (isSport) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFF26303D), Color(0xFF10151D)), center, arcRadius),
                        radius = arcRadius,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFF495361), radius = arcRadius, center = center,
                        style = Stroke(1.dp.toPx())
                    )
                }

                // Background track arc
                drawArc(
                    color = trackBackgroundColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (isSport) drawArc(
                    color = tertiaryColor,
                    startAngle = startAngle + sweepAngle * 0.8f,
                    sweepAngle = sweepAngle * 0.2f,
                    useCenter = false, topLeft = arcTopLeft, size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Butt)
                )

                // Active speed progress arc
                val activeSweep = (speedFraction * sweepAngle).coerceAtLeast(0.1f)
                val arcGradient = Brush.sweepGradient(
                    0.375f to primaryColor,
                    0.75f to secondaryColor,
                    1.0f to tertiaryColor,
                    center = center
                )
                if (speedAvailable && !isMinimal) drawArc(
                    brush = arcGradient,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Draw tick markers
                val majorStep = when (speedUnit) {
                    SpeedUnit.KMH -> 20f
                    SpeedUnit.MPH -> 20f
                    SpeedUnit.MS -> 10f
                }
                val numTicks = (maxScale / (majorStep / 2)).toInt()

                for (i in 0..numTicks) {
                    val tickFraction = i.toFloat() / numTicks
                    val angleDeg = startAngle + (tickFraction * sweepAngle)
                    val angleRad = Math.toRadians(angleDeg.toDouble())
                    val isMajor = i % 2 == 0

                    if (isMinimal && !isMajor) continue
                    val tickLength = when {
                        isMinimal -> 4.dp.toPx()
                        isMajor && span == WidgetSpan.LARGE -> 10.dp.toPx()
                        isMajor -> 7.dp.toPx()
                        span == WidgetSpan.LARGE -> 5.dp.toPx()
                        else -> 3.dp.toPx()
                    }
                    val tickWidth = if (isMinimal) 1.dp.toPx() else if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                    val tickColor = if (isSport) {
                        if (tickFraction >= 0.8f) tertiaryColor else Color(0xFFE6EAF0)
                    } else if (isMajor) {
                        if (speedAvailable && tickFraction <= speedFraction) primaryColor else inactiveTickColor
                    } else {
                        trackBackgroundColor
                    }

                    val innerR = arcRadius - (strokeWidth / 2f) - 2.dp.toPx() - tickLength
                    val outerR = arcRadius - (strokeWidth / 2f) - 2.dp.toPx()

                    val startX = (center.x + innerR * cos(angleRad)).toFloat()
                    val startY = (center.y + innerR * sin(angleRad)).toFloat()
                    val endX = (center.x + outerR * cos(angleRad)).toFloat()
                    val endY = (center.y + outerR * sin(angleRad)).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round
                    )

                    if (isSport && (i % 4 == 0 || i == numTicks)) {
                        val labelRadius = innerR - 5.dp.toPx()
                        val label = textMeasurer.measure(
                            text = (maxScale * tickFraction).roundToInt().toString(),
                            style = TextStyle(
                                color = Color(0xFFE6EAF0),
                                fontSize = if (span == WidgetSpan.LARGE) 10.sp else if (span == WidgetSpan.HALF) 6.sp else 7.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        drawText(label, topLeft = Offset(
                            center.x + labelRadius * cos(angleRad).toFloat() - label.size.width / 2f,
                            center.y + labelRadius * sin(angleRad).toFloat() - label.size.height / 2f
                        ))
                    }
                }

                // The path points up (-90°); add 90° to align it with the dial's angle.
                if (speedAvailable) rotate(degrees = animatedNeedleAngle + 90f, pivot = center) {
                    val needleLength = arcRadius * 0.85f
                    val needleBaseWidth = when (span) {
                        WidgetSpan.HALF -> 2.5.dp.toPx()
                        WidgetSpan.FULL_WIDTH -> 3.5.dp.toPx()
                        WidgetSpan.LARGE -> 5.dp.toPx()
                    }

                    val needlePath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x - needleBaseWidth, center.y)
                        lineTo(center.x, center.y + needleBaseWidth * 1.5f)
                        lineTo(center.x + needleBaseWidth, center.y)
                        close()
                    }
                    if (isMinimal) {
                        drawLine(
                            color = needleColor,
                            start = center,
                            end = Offset(center.x, center.y - needleLength),
                            strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round
                        )
                    } else {
                        drawPath(path = needlePath, color = needleColor)
                    }

                    // Center pivot hub
                    val hubRadius = if (isMinimal) 2.5.dp.toPx() else when (span) {
                        WidgetSpan.HALF -> 4.dp.toPx()
                        WidgetSpan.FULL_WIDTH -> 5.5.dp.toPx()
                        WidgetSpan.LARGE -> 8.dp.toPx()
                    }
                    drawCircle(
                        color = primaryColor,
                        radius = hubRadius + 1.5.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = hubColor,
                        radius = hubRadius,
                        center = center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Digital speed readout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (speedAvailable) String.format(LocalLocale.current.platformLocale, "%.1f", displaySpeed) else "—",
                fontWeight = FontWeight.Bold,
                fontSize = if (span == WidgetSpan.HALF) 13.sp else 16.sp,
                color = primaryColor
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = displayUnitStr,
                fontWeight = FontWeight.Medium,
                fontSize = if (span == WidgetSpan.HALF) 11.sp else 13.sp,
                color = secondaryColor
            )
        }
    }
}

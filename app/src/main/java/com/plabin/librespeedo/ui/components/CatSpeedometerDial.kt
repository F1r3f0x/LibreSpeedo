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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** A vector-drawn cat instrument with a paw accent and an animated marker on the rim. */
@Composable
internal fun CatSpeedometerDial(
    speedAvailable: Boolean,
    displaySpeed: Float,
    unit: String,
    maxScale: Float,
    animatedAngle: Float,
    dialSize: Dp,
    modifier: Modifier = Modifier
) {
    val white = Color(0xFFE8ECEE)
    val black = Color(0xFF080C10)
    val blue = Color(0xFF4096B5)
    val red = Color(0xFFB52C38)
    val textMeasurer = rememberTextMeasurer()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(dialSize).semantics {
            contentDescription = if (speedAvailable) "Speed gauge" else "Speed unavailable"
        }) {
            Canvas(Modifier.fillMaxSize()) {
                val d = size.minDimension
                val c = Offset(size.width / 2f, size.height / 2f)
                fun p(x: Float, y: Float) = c + Offset(x * d, y * d)
                fun oval(x: Float, y: Float, w: Float, h: Float, color: Color) {
                    drawOval(color, p(x, y), Size(w * d, h * d))
                }
                fun arc(radius: Float, start: Float, sweep: Float, color: Color, width: Float) {
                    drawArc(color, start, sweep, false, p(-radius, -radius),
                        Size(radius * 2f * d, radius * 2f * d), style = Stroke(width * d))
                }

                drawCircle(black, d * 0.43f, c)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF343A41), black), c, d * 0.43f), d * 0.425f, c)
                arc(0.46f, 135f, 270f, Color(0xFF122B38), 0.07f)
                arc(0.46f, 135f, 270f, blue, 0.045f)
                for (i in 0..24) {
                    val a = Math.toRadians((135f + i * 270f / 24f).toDouble())
                    val inner = if (i % 2 == 0) 0.435f else 0.445f
                    drawLine(Color(0xFF14232C),
                        p(cos(a).toFloat() * inner, sin(a).toFloat() * inner),
                        p(cos(a).toFloat() * 0.48f, sin(a).toFloat() * 0.48f),
                        strokeWidth = d * 0.009f)
                }
                arc(0.37f, 135f, 270f, Color(0xFF11171D), 0.04f)
                arc(0.37f, 135f, 210f, white, 0.012f)
                arc(0.37f, 305f, 45f, red, 0.035f)

                // The face and ears stay inside the inner instrument bezel.
                val faceClip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(p(-0.34f, -0.34f), Size(d * 0.68f, d * 0.68f))) }
                clipPath(faceClip) {
                    val face = Path().apply {
                        moveTo(p(-0.31f, -0.08f).x, p(-0.31f, -0.08f).y)
                        lineTo(p(-0.29f, -0.37f).x, p(-0.29f, -0.37f).y)
                        lineTo(p(-0.12f, -0.26f).x, p(-0.12f, -0.26f).y)
                        quadraticTo(p(0.02f, -0.31f).x, p(0.02f, -0.31f).y, p(0.12f, -0.22f).x, p(0.12f, -0.22f).y)
                        lineTo(p(0.28f, -0.29f).x, p(0.28f, -0.29f).y)
                        lineTo(p(0.18f, -0.04f).x, p(0.18f, -0.04f).y)
                        quadraticTo(p(0.25f, 0.27f).x, p(0.25f, 0.27f).y, p(-0.07f, 0.33f).x, p(-0.07f, 0.33f).y)
                        quadraticTo(p(-0.36f, 0.34f).x, p(-0.36f, 0.34f).y, p(-0.31f, -0.08f).x, p(-0.31f, -0.08f).y)
                        close()
                    }
                    drawPath(face, Brush.linearGradient(listOf(Color(0xFFF0F0EF), Color(0xFF8D9298)), p(-0.25f, -0.25f), p(0.17f, 0.30f)))
                    drawPath(face, black, style = Stroke(d * 0.015f))
                    val ear = Path().apply {
                        moveTo(p(-0.27f, -0.28f).x, p(-0.27f, -0.28f).y)
                        lineTo(p(-0.24f, -0.15f).x, p(-0.24f, -0.15f).y)
                        lineTo(p(-0.17f, -0.24f).x, p(-0.17f, -0.24f).y)
                        close()
                    }
                    drawPath(ear, Color(0xFF737B83))
                    oval(-0.235f, -0.085f, 0.065f, 0.095f, black)
                    oval(-0.22f, -0.077f, 0.022f, 0.032f, white)
                    oval(-0.012f, -0.025f, 0.078f, 0.11f, black)
                    oval(0.002f, -0.015f, 0.024f, 0.035f, white)
                    val brow = Path().apply {
                        moveTo(p(-0.015f, -0.13f).x, p(-0.015f, -0.13f).y)
                        quadraticTo(p(0.055f, -0.16f).x, p(0.055f, -0.16f).y, p(0.09f, -0.09f).x, p(0.09f, -0.09f).y)
                    }
                    drawPath(brow, black, style = Stroke(d * 0.015f, cap = StrokeCap.Round))
                    val nose = Path().apply {
                        moveTo(p(-0.155f, 0.09f).x, p(-0.155f, 0.09f).y)
                        lineTo(p(-0.075f, 0.105f).x, p(-0.075f, 0.105f).y)
                        lineTo(p(-0.12f, 0.145f).x, p(-0.12f, 0.145f).y)
                        close()
                    }
                    drawPath(nose, black)
                    val smile = Path().apply {
                        moveTo(p(-0.23f, 0.12f).x, p(-0.23f, 0.12f).y)
                        quadraticTo(p(-0.20f, 0.24f).x, p(-0.20f, 0.24f).y, p(-0.12f, 0.16f).x, p(-0.12f, 0.16f).y)
                        quadraticTo(p(-0.06f, 0.26f).x, p(-0.06f, 0.26f).y, p(-0.015f, 0.15f).x, p(-0.015f, 0.15f).y)
                    }
                    drawPath(smile, black, style = Stroke(d * 0.012f, cap = StrokeCap.Round))
                }

                // A dark speed window keeps the digits readable over the illustration.
                val window = Path().apply {
                    moveTo(p(0.09f, 0.04f).x, p(0.09f, 0.04f).y)
                    lineTo(p(0.36f, -0.015f).x, p(0.36f, -0.015f).y)
                    lineTo(p(0.36f, 0.27f).x, p(0.36f, 0.27f).y)
                    lineTo(p(0.12f, 0.31f).x, p(0.12f, 0.31f).y)
                    close()
                }
                drawPath(window, black)
                for (i in 0..8) {
                    val a = Math.toRadians((135f + i * 270f / 8f).toDouble())
                    val tick = Path().apply {
                        moveTo(p(cos(a).toFloat() * 0.345f, sin(a).toFloat() * 0.345f).x,
                            p(cos(a).toFloat() * 0.345f, sin(a).toFloat() * 0.345f).y)
                        val a1 = a - 0.035
                        val a2 = a + 0.035
                        lineTo(p(cos(a1).toFloat() * 0.37f, sin(a1).toFloat() * 0.37f).x,
                            p(cos(a1).toFloat() * 0.37f, sin(a1).toFloat() * 0.37f).y)
                        lineTo(p(cos(a2).toFloat() * 0.37f, sin(a2).toFloat() * 0.37f).x,
                            p(cos(a2).toFloat() * 0.37f, sin(a2).toFloat() * 0.37f).y)
                        close()
                    }
                    drawPath(tick, white)
                }

                // Separate paw-shaped mini dial, as in the reference.
                val pawCenter = p(-0.35f, 0.32f)
                drawCircle(black, d * 0.135f, pawCenter)
                drawCircle(Color(0xFF414A54), d * 0.12f, pawCenter, style = Stroke(d * 0.012f))
                oval(-0.425f, 0.315f, 0.14f, 0.09f, Color(0xFFB9BDC1))
                oval(-0.443f, 0.27f, 0.045f, 0.055f, white)
                oval(-0.395f, 0.225f, 0.05f, 0.065f, Color(0xFFCDD1D5))
                oval(-0.335f, 0.235f, 0.05f, 0.065f, Color(0xFFCDD1D5))
                oval(-0.287f, 0.28f, 0.04f, 0.055f, Color(0xFFCDD1D5))

                val topLabel = textMeasurer.measure((maxScale / 2f).roundToInt().toString(),
                    TextStyle(color = black, fontSize = (dialSize.value * 0.045f).sp,
                        fontWeight = FontWeight.Bold))
                drawText(topLabel, topLeft = Offset(c.x - topLabel.size.width / 2f, c.y - d * 0.475f))

                if (speedAvailable) rotate(animatedAngle + 90f, c) {
                    val marker = Path().apply {
                        moveTo(c.x, c.y - d * 0.405f)
                        lineTo(c.x - d * 0.025f, c.y - d * 0.345f)
                        lineTo(c.x + d * 0.025f, c.y - d * 0.345f)
                        close()
                    }
                    drawPath(marker, Color(0xFFFFD166))
                    drawPath(marker, black, style = Stroke(d * 0.006f))
                }
            }
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).offset(x = -dialSize * 0.11f, y = dialSize * 0.13f)
                    .width(dialSize * 0.30f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (speedAvailable) String.format(LocalLocale.current.platformLocale, "%03.0f", displaySpeed) else "—",
                    color = white, fontSize = (dialSize.value * 0.12f).sp,
                    fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic, maxLines = 1)
                Text(unit, color = white.copy(alpha = 0.8f), fontSize = (dialSize.value * 0.07f).coerceAtLeast(8f).sp,
                    fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import kotlin.math.min

private val digitMasks = intArrayOf(0x3f, 0x06, 0x5b, 0x4f, 0x66, 0x6d, 0x7d, 0x07, 0x7f, 0x6f)

/** Scalable LED-like numerals; semantics retain the exact localized speed reading. */
@Composable
internal fun SevenSegmentReadout(value: String, modifier: Modifier = Modifier) {
    val active = MaterialTheme.colorScheme.secondary
    val inactive = active.copy(alpha = 0.08f)
    val characters = if (value == "—") "---" else value
    Canvas(modifier.semantics { text = AnnotatedString(value) }) {
        fun isDecimal(char: Char) = char == '.' || char == ',' || char == '\u066b'
        fun width(char: Char) = if (isDecimal(char)) 0.18f else 0.60f
        val naturalWidth = characters.sumOf { width(it).toDouble() }.toFloat() +
            (characters.length - 1).coerceAtLeast(0) * 0.10f
        val scale = min(size.height, size.width / naturalWidth.coerceAtLeast(0.1f))
        var left = (size.width - naturalWidth * scale) / 2f
        val top = (size.height - scale) / 2f
        characters.forEach { char ->
            if (isDecimal(char)) {
                drawCircle(active, radius = scale * 0.055f,
                    center = Offset(left + scale * 0.09f, top + scale * 0.93f))
            } else {
                val digit = Character.digit(char, 10)
                val mask = if (digit >= 0) digitMasks[digit] else if (char == '-') 0x40 else 0
                fun segment(index: Int, points: List<Pair<Float, Float>>) {
                    val path = Path().apply {
                        points.forEachIndexed { i, (x, y) ->
                            if (i == 0) moveTo(left + x * scale, top + y * scale)
                            else lineTo(left + x * scale, top + y * scale)
                        }
                        close()
                    }
                    drawPath(path, if (mask and (1 shl index) != 0) active else inactive)
                }
                fun horizontal(index: Int, y: Float) = segment(index, listOf(
                    0.10f to y, 0.16f to y - 0.045f, 0.44f to y - 0.045f,
                    0.50f to y, 0.44f to y + 0.045f, 0.16f to y + 0.045f
                ))
                fun vertical(index: Int, x: Float, y: Float) = segment(index, listOf(
                    x to y, x + 0.045f to y + 0.06f, x + 0.045f to y + 0.32f,
                    x to y + 0.38f, x - 0.045f to y + 0.32f, x - 0.045f to y + 0.06f
                ))
                horizontal(0, 0.05f)
                vertical(1, 0.55f, 0.08f)
                vertical(2, 0.55f, 0.54f)
                horizontal(3, 0.95f)
                vertical(4, 0.05f, 0.54f)
                vertical(5, 0.05f, 0.08f)
                horizontal(6, 0.50f)
            }
            left += (width(char) + 0.10f) * scale
        }
    }
}

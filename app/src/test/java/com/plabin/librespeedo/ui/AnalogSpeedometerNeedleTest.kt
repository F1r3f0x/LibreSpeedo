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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.plabin.librespeedo.data.AnalogSpeedometerStyle
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.components.AnalogSpeedometerComponent
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AnalogSpeedometerNeedleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun needlePointsAlongScale_atZeroHalfAndFullSpeed() {
        val state = mutableStateOf(SpeedometerUiState(
            isLocationFresh = true, isSpeedAvailable = true
        ))
        val style = mutableStateOf(AnalogSpeedometerStyle.CLASSIC)
        compose.setContent {
            LibreSpeedoTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    AnalogSpeedometerComponent(state.value, SpeedUnit.KMH, span = WidgetSpan.LARGE, style = style.value)
                }
            }
        }
        // Expected directions: lower-left at zero, up at half, lower-right at full scale.
        for (preset in AnalogSpeedometerStyle.entries) {
            compose.runOnIdle { style.value = preset }
            for ((speed, direction) in listOf(
                0f to Pair(-1.0, 1.0),
                120f to Pair(0.0, -1.0),
                240f to Pair(1.0, 1.0)
            )) {
                compose.runOnIdle { state.value = state.value.copy(speedKmh = speed) }
                compose.mainClock.advanceTimeBy(5_000L)
                val pixels = compose.onNodeWithContentDescription("Speed gauge").captureToImage().toPixelMap()
                var sumX = 0.0
                var sumY = 0.0
                var count = 0
                for (y in 0 until pixels.height) {
                    for (x in 0 until pixels.width) {
                        val color = pixels[x, y]
                        // Inspect the inner dial so the Sport red zone and outer ticks
                        // cannot be mistaken for the needle.
                        val isNeedle = if (preset == AnalogSpeedometerStyle.MINIMAL) {
                            color.red > 0.75f && color.green > 0.75f && color.blue > 0.75f
                        } else color.red > 0.75f && color.green < 0.4f && color.blue < 0.4f
                        val innerDial = hypot(x + 0.5 - pixels.width / 2.0,
                            y + 0.5 - pixels.height / 2.0) < pixels.width * 0.28
                        if (isNeedle && innerDial) {
                            sumX += x + 0.5
                            sumY += y + 0.5
                            count++
                        }
                    }
                }
                assertTrue("$preset needle should be visible at $speed km/h", count > 0)
                val dx = sumX / count - pixels.width / 2.0
                val dy = sumY / count - pixels.height / 2.0
                val alignment = (dx * direction.first + dy * direction.second) /
                    (hypot(dx, dy) * hypot(direction.first, direction.second))
                assertTrue("$preset needle points the wrong way at $speed km/h: ($dx, $dy)", alignment > 0.98)
            }
        }
    }
}

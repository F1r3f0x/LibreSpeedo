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
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.components.AnalogSpeedometerComponent
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
        compose.setContent {
            AnalogSpeedometerComponent(state.value, SpeedUnit.KMH, span = WidgetSpan.LARGE)
        }
        // Expected directions: lower-left at zero, up at half, lower-right at full scale.
        for ((speed, direction) in listOf(
            0f to Pair(-1.0, 1.0),
            110f to Pair(0.0, -1.0),
            220f to Pair(1.0, 1.0)
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
                    if (color.red > 0.75f && color.green < 0.4f && color.blue < 0.4f) {
                        sumX += x + 0.5
                        sumY += y + 0.5
                        count++
                    }
                }
            }
            assertTrue("Red needle should be visible at $speed km/h", count > 0)
            val dx = sumX / count - pixels.width / 2.0
            val dy = sumY / count - pixels.height / 2.0
            val alignment = (dx * direction.first + dy * direction.second) /
                (hypot(dx, dy) * hypot(direction.first, direction.second))
            assertTrue("Needle points the wrong way at $speed km/h: ($dx, $dy)", alignment > 0.98)
        }
    }
}

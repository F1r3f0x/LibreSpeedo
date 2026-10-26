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

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.plabin.librespeedo.data.AnalogSpeedometerStyle
import com.plabin.librespeedo.data.FakeSharedPreferences
import com.plabin.librespeedo.data.SettingsRepository
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.components.AnalogSpeedometerComponent
import com.plabin.librespeedo.ui.components.WidgetContainer
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1000dp-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AnalogSpeedometerStyleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun widgetSettings_selectsStyleAndUpdatesPreview() {
        val settings = SettingsRepository(FakeSharedPreferences())
        compose.setContent {
            val style by settings.analogSpeedometerStyle.collectAsState()
            LibreSpeedoTheme(darkTheme = true) {
                Box(Modifier.size(360.dp, 280.dp)) {
                    WidgetContainer(
                        widget = WidgetType.ANALOG_SPEEDOMETER, span = WidgetSpan.LARGE,
                        uiState = SpeedometerUiState(), isEditMode = true, speedUnit = SpeedUnit.KMH,
                        onRemove = {}, onToggleSpan = {}, onSpeedUnitChange = {}, elevation = 0.dp,
                        analogStyle = style, onAnalogStyleChange = settings::setAnalogSpeedometerStyle
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Widget Settings").performClick()
        compose.onNodeWithText("Sport").performScrollTo().performClick()
        compose.onNodeWithText("Sport").assertIsSelected()
        assertEquals(AnalogSpeedometerStyle.SPORT, settings.analogSpeedometerStyle.value)
        compose.onNodeWithContentDescription("Preview of Sport style").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Minimal").performScrollTo().performClick()
        assertEquals(AnalogSpeedometerStyle.MINIMAL, settings.analogSpeedometerStyle.value)
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Appearance").assertDoesNotExist()
        // A style change must not turn unavailable speed into a fake live reading.
        compose.onNodeWithContentDescription("Speed unavailable").assertExists()
    }

    @Test
    fun allStyles_renderAndRespectUnavailableSpeed() {
        val state = mutableStateOf(SpeedometerUiState(
            speedKmh = 88f, isLocationFresh = true, isSpeedAvailable = true
        ))
        compose.setContent {
            LibreSpeedoTheme(darkTheme = true) {
                Column(Modifier.width(900.dp).background(MaterialTheme.colorScheme.background)
                    .padding(24.dp).testTag("style-gallery")) {
                    Text("Analog speedometer", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AnalogSpeedometerStyle.entries.forEach { style ->
                            Surface(modifier = Modifier.weight(1f).height(300.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.large) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(style.label, style = MaterialTheme.typography.titleMedium)
                                    Text(style.description, style = MaterialTheme.typography.bodySmall)
                                    AnalogSpeedometerComponent(state.value, SpeedUnit.KMH,
                                        span = WidgetSpan.LARGE, style = style)
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onAllNodesWithContentDescription("Speed gauge").assertCountEquals(3)
        compose.onAllNodesWithText("88.0").assertCountEquals(3)
        val gallery = compose.onNodeWithTag("style-gallery").captureToImage()
        File("/tmp/librespeedo-analog-styles.png").outputStream().use {
            gallery.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.runOnIdle { state.value = state.value.copy(isLocationFresh = false) }
        compose.onAllNodesWithContentDescription("Speed unavailable").assertCountEquals(3)
        compose.onAllNodesWithText("—").assertCountEquals(3)
    }
}

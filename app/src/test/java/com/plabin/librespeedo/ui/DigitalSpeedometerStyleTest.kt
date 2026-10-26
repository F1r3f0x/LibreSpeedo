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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.plabin.librespeedo.data.*
import com.plabin.librespeedo.ui.components.*
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
class DigitalSpeedometerStyleTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun widgetSettings_selectsSevenSegmentAndUpdatesPreview() {
        val settings = SettingsRepository(FakeSharedPreferences())
        compose.setContent {
            val style by settings.digitalSpeedometerStyle.collectAsState()
            LibreSpeedoTheme(darkTheme = true) {
                Box(Modifier.size(360.dp, 280.dp)) {
                    WidgetContainer(WidgetType.SPEEDOMETER, WidgetSpan.LARGE,
                        SpeedometerUiState(), true, SpeedUnit.KMH, {}, {}, {}, 0.dp,
                        digitalStyle = style, onDigitalStyleChange = settings::setDigitalSpeedometerStyle)
                }
            }
        }
        compose.onNodeWithContentDescription("Widget Settings").performClick()
        compose.onNodeWithText("7-segment").performScrollTo().performClick()
        compose.onNodeWithText("7-segment").assertIsSelected()
        assertEquals(DigitalSpeedometerStyle.SEVEN_SEGMENT, settings.digitalSpeedometerStyle.value)
        compose.onNodeWithContentDescription("Preview of 7-segment style").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("—").assertExists()
    }

    @Test
    fun sevenSegment_preservesSpeedUnitsAndUnavailableState() {
        val state = mutableStateOf(SpeedometerUiState(speedKmh = 72f,
            isLocationFresh = true, isSpeedAvailable = true))
        val unit = mutableStateOf(SpeedUnit.KMH)
        compose.setContent {
            LibreSpeedoTheme(darkTheme = true) {
                SpeedComponent(state.value, unit.value, style = DigitalSpeedometerStyle.SEVEN_SEGMENT)
            }
        }
        compose.onNodeWithText("72.0").assertExists()
        compose.runOnIdle { unit.value = SpeedUnit.MPH }
        compose.onNodeWithText("44.7").assertExists()
        compose.runOnIdle { unit.value = SpeedUnit.MS }
        compose.onNodeWithText("20.0").assertExists()
        compose.runOnIdle { state.value = state.value.copy(isLocationFresh = false) }
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("20.0").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(speedKmh = 0f, isLocationFresh = true) }
        compose.onNodeWithText("0.0").assertExists()
    }

    @Test
    @Config(qualifiers = "de-w1000dp-h1000dp")
    fun sevenSegment_preservesLocalizedDecimalSeparator() {
        compose.setContent {
            SpeedComponent(SpeedometerUiState(speedKmh = 72f, isLocationFresh = true,
                isSpeedAvailable = true), SpeedUnit.KMH, style = DigitalSpeedometerStyle.SEVEN_SEGMENT)
        }
        compose.onNodeWithText("72,0").assertExists()
    }

    @Test
    fun stylesRenderWithAllDigits() {
        val state = SpeedometerUiState(speedKmh = 128.4f, isLocationFresh = true, isSpeedAvailable = true)
        compose.setContent {
            LibreSpeedoTheme(darkTheme = true) {
                Column(Modifier.width(900.dp).background(MaterialTheme.colorScheme.background)
                    .padding(24.dp).testTag("digital-gallery")) {
                    Text("Digital speedometer", style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DigitalSpeedometerStyle.entries.forEach { style ->
                            Surface(Modifier.weight(1f).height(220.dp), shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(Modifier.padding(20.dp)) {
                                    Text(style.label, style = MaterialTheme.typography.titleMedium)
                                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                        SpeedComponent(state, SpeedUnit.KMH, style = style)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    SevenSegmentReadout("0123456789", Modifier.fillMaxWidth().height(60.dp))
                }
            }
        }
        compose.onAllNodesWithText("128.4").assertCountEquals(2)
        compose.onNodeWithText("0123456789").assertExists()
        File("/tmp/librespeedo-seven-segment.png").outputStream().use {
            compose.onNodeWithTag("digital-gallery").captureToImage().asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}

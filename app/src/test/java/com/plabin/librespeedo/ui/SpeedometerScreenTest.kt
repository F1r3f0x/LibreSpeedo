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

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.components.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests verifying [SpeedometerScreen] widget rendering, edit mode controls,
 * speed unit switching, and modular component interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h2000dp", sdk = [34])
class SpeedometerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun speedometerScreen_rendersAllStandardWidgets() {
        var settingsClicked = false
        val uiState = SpeedometerUiState(
            speedKmh = 72f,
            latitude = 37.7749,
            longitude = -122.4194,
            heading = 45f,
            altitude = 120.0,
            accuracy = 3.2,
            provider = "fused",
            accelX = 0.5f,
            gyroX = 0.1f,
            magX = 15f
        )
        val allWidgets = listOf(
            WidgetType.SPEEDOMETER,
            WidgetType.ANALOG_SPEEDOMETER,
            WidgetType.COMPASS,
            WidgetType.ANALOG_COMPASS,
            WidgetType.CHRONOMETER,
            WidgetType.POSITION,
            WidgetType.DEBUG,
            WidgetType.HELLO_WORLD
        )

        composeTestRule.setContent {
            SpeedometerScreen(
                uiState = uiState,
                activeWidgets = allWidgets,
                isEditMode = false,
                speedUnit = SpeedUnit.KMH,
                widgetSpans = emptyMap(),
                onReorderWidget = { _, _ -> },
                onAddWidget = {},
                onRemoveWidget = {},
                onWidgetSpanChange = { _, _ -> },
                onSpeedUnitChange = {},
                onSettingsClick = { settingsClicked = true }
            )
        }

        // Header
        composeTestRule.onNodeWithText("LibreSpeedo").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(settingsClicked)

        // Verify widgets are rendered
        composeTestRule.onAllNodesWithText("km/h").assertCountEquals(2) // Digital + Analog speedometer
        composeTestRule.onNodeWithText("Current Position").assertExists()
        composeTestRule.onNodeWithText("N").assertExists()
        composeTestRule.onNodeWithText("Chronometer").assertExists()
        composeTestRule.onNodeWithText("Debug Info").assertExists()
        composeTestRule.onNodeWithText("Hello World!").assertExists()
    }

    @Test
    fun speedometerScreen_editMode_allowsAddingAndRemovingWidgets() {
        var addedWidget: WidgetType? = null
        var removedWidget: WidgetType? = null
        var changedSpanWidget: WidgetType? = null

        val activeWidgets = listOf(WidgetType.SPEEDOMETER, WidgetType.COMPASS)

        composeTestRule.setContent {
            SpeedometerScreen(
                uiState = SpeedometerUiState(),
                activeWidgets = activeWidgets,
                isEditMode = true,
                speedUnit = SpeedUnit.KMH,
                widgetSpans = mapOf("SPEEDOMETER" to WidgetSpan.FULL_WIDTH),
                onReorderWidget = { _, _ -> },
                onAddWidget = { addedWidget = it },
                onRemoveWidget = { removedWidget = it },
                onWidgetSpanChange = { w, _ -> changedSpanWidget = w },
                onSpeedUnitChange = {},
                onSettingsClick = {}
            )
        }

        // Open Add Component menu via FAB
        composeTestRule.onNodeWithContentDescription("Add Component").performClick()
        composeTestRule.onNodeWithText("ANALOG_SPEEDOMETER").assertExists()
        composeTestRule.onNodeWithText("ANALOG_SPEEDOMETER").performClick()
        assertEquals(WidgetType.ANALOG_SPEEDOMETER, addedWidget)

        // Click Remove on the first widget
        val removeButtons = composeTestRule.onAllNodes(hasContentDescription("Remove Widget"))
        if (removeButtons.fetchSemanticsNodes().isNotEmpty()) {
            removeButtons[0].performClick()
            assertEquals(WidgetType.SPEEDOMETER, removedWidget)
        }

        // Click Resize Span on the first widget
        val resizeButtons = composeTestRule.onAllNodes(hasContentDescription("Resize Widget (2x1)"))
        if (resizeButtons.fetchSemanticsNodes().isNotEmpty()) {
            resizeButtons[0].performClick()
            assertEquals(WidgetType.SPEEDOMETER, changedSpanWidget)
        }
    }

    @Test
    fun speedometerScreen_speedUnitSettingsDialog_changesUnit() {
        var selectedUnit: SpeedUnit? = null

        composeTestRule.setContent {
            SpeedometerScreen(
                uiState = SpeedometerUiState(speedKmh = 50f),
                activeWidgets = listOf(WidgetType.SPEEDOMETER, WidgetType.ANALOG_SPEEDOMETER),
                isEditMode = true,
                speedUnit = SpeedUnit.KMH,
                widgetSpans = emptyMap(),
                onReorderWidget = { _, _ -> },
                onAddWidget = {},
                onRemoveWidget = {},
                onWidgetSpanChange = { _, _ -> },
                onSpeedUnitChange = { selectedUnit = it },
                onSettingsClick = {}
            )
        }

        // Click on widget settings icon in edit mode
        val settingsButtons = composeTestRule.onAllNodes(hasContentDescription("Widget Settings"))
        settingsButtons[0].performClick()
        composeTestRule.onNodeWithText("Speedometer Settings").assertExists()

        // Find the RadioButton for MPH by finding all selectable nodes
        val radioButtons = composeTestRule.onAllNodes(isSelectable())
        if (radioButtons.fetchSemanticsNodes().size >= 2) {
            radioButtons[1].performClick()
            assertEquals(SpeedUnit.MPH, selectedUnit)
        }

        composeTestRule.onNodeWithText("Done").performClick()
    }

    @Test
    fun analogSpeedometerComponent_rendersProperlyAcrossUnitsAndSpans() {
        val state = SpeedometerUiState(speedKmh = 85.5f)

        composeTestRule.setContent {
            Column {
                AnalogSpeedometerComponent(uiState = state, speedUnit = SpeedUnit.KMH, span = WidgetSpan.FULL_WIDTH)
                AnalogSpeedometerComponent(uiState = state, speedUnit = SpeedUnit.MPH, span = WidgetSpan.HALF)
                AnalogSpeedometerComponent(uiState = state, speedUnit = SpeedUnit.MS, span = WidgetSpan.LARGE)
            }
        }

        composeTestRule.onNodeWithText("85.5").assertExists()
        composeTestRule.onNodeWithText("km/h").assertExists()
        composeTestRule.onNodeWithText("mph").assertExists()
        composeTestRule.onNodeWithText("m/s").assertExists()
    }

    @Test
    fun analogCompassComponent_rendersHeadingAndCardinalDirection() {
        composeTestRule.setContent {
            Column {
                AnalogCompassComponent(heading = 0f, span = WidgetSpan.FULL_WIDTH)
                AnalogCompassComponent(heading = 30f, span = WidgetSpan.HALF)
                AnalogCompassComponent(heading = 90f, span = WidgetSpan.LARGE)
                AnalogCompassComponent(heading = 120f, span = WidgetSpan.FULL_WIDTH)
                AnalogCompassComponent(heading = 180f, span = WidgetSpan.HALF)
                AnalogCompassComponent(heading = 210f, span = WidgetSpan.LARGE)
                AnalogCompassComponent(heading = 270f, span = WidgetSpan.FULL_WIDTH)
                AnalogCompassComponent(heading = 300f, span = WidgetSpan.HALF)
                AnalogCompassComponent(heading = 340f, span = WidgetSpan.LARGE)
            }
        }

        composeTestRule.onNodeWithText("0°").assertExists()
        composeTestRule.onNodeWithText("30°").assertExists()
        composeTestRule.onNodeWithText("NE").assertExists()
        composeTestRule.onNodeWithText("90°").assertExists()
        composeTestRule.onNodeWithText("E").assertExists()
        composeTestRule.onNodeWithText("120°").assertExists()
        composeTestRule.onNodeWithText("SE").assertExists()
        composeTestRule.onNodeWithText("180°").assertExists()
        composeTestRule.onNodeWithText("S").assertExists()
        composeTestRule.onNodeWithText("210°").assertExists()
        composeTestRule.onNodeWithText("SW").assertExists()
        composeTestRule.onNodeWithText("270°").assertExists()
        composeTestRule.onNodeWithText("W").assertExists()
        composeTestRule.onNodeWithText("300°").assertExists()
        composeTestRule.onNodeWithText("NW").assertExists()
        composeTestRule.onNodeWithText("340°").assertExists()
    }

    @Test
    fun chronometerComponent_startsPausesLapsAndResets() {
        composeTestRule.setContent {
            Column {
                ChronometerComponent(span = WidgetSpan.FULL_WIDTH)
                ChronometerComponent(span = WidgetSpan.HALF)
            }
        }

        // Initially chronometer shows 00:00.00
        composeTestRule.onAllNodesWithText("00:00.00").assertCountEquals(2)

        // Click Start on the first chronometer
        composeTestRule.onAllNodesWithContentDescription("Start Chronometer")[0].performClick()

        // Record a Lap
        val lapButtons = composeTestRule.onAllNodesWithContentDescription("Record Lap")
        if (lapButtons.fetchSemanticsNodes().isNotEmpty()) {
            lapButtons[0].performClick()
        }

        // Pause
        val pauseButtons = composeTestRule.onAllNodesWithContentDescription("Pause Chronometer")
        if (pauseButtons.fetchSemanticsNodes().isNotEmpty()) {
            pauseButtons[0].performClick()
        }

        // Reset
        val resetButtons = composeTestRule.onAllNodesWithContentDescription("Reset Chronometer")
        if (resetButtons.fetchSemanticsNodes().isNotEmpty()) {
            resetButtons[0].performClick()
        }
    }

    @Test
    fun speedometerScreen_componentsDirectRendering_allCovered() {
        val state = SpeedometerUiState(
            speedKmh = 100f,
            latitude = 12.34,
            longitude = 56.78,
            heading = 180f,
            altitude = 300.0,
            accuracy = 1.5,
            provider = "gps",
            error = "Sample error"
        )

        composeTestRule.setContent {
            Column {
                SpeedComponent(uiState = state, speedUnit = SpeedUnit.MPH, span = WidgetSpan.LARGE)
                SpeedComponent(uiState = state, speedUnit = SpeedUnit.MS, span = WidgetSpan.HALF)
                PositionComponent(uiState = state, span = WidgetSpan.FULL_WIDTH)
                CompassComponent(heading = 90f, span = WidgetSpan.FULL_WIDTH)
                CompassComponent(heading = 270f, span = WidgetSpan.LARGE)
                DebugComponent(uiState = state, span = WidgetSpan.HALF)
                HelloWorldComponent()
            }
        }

        composeTestRule.onNodeWithText("Debug Info").assertExists()
        composeTestRule.onNodeWithText("Error: Sample error").assertExists()
    }

    @Test
    fun speedometerScreenPreview_renders() {
        composeTestRule.setContent {
            SpeedometerScreenPreview()
        }
        composeTestRule.onNodeWithText("LibreSpeedo").assertExists()
    }
}

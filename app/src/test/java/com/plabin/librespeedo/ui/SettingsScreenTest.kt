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

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * UI tests verifying [SettingsScreen] preference toggles, reset layout dialog flow, and navigation.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysAndInteractsWithAllElements() {
        var oledChanged = false
        var keepScreenOnChanged = false
        var editModeChanged = false
        var resetLayoutCalled = false
        var backCalled = false
        var aboutCalled = false

        composeTestRule.setContent {
            SettingsScreen(
                isOledTheme = false,
                isKeepScreenOn = true,
                isEditMode = false,
                onOledThemeChanged = { oledChanged = true },
                onKeepScreenOnChanged = { keepScreenOnChanged = true },
                onEditModeChanged = { editModeChanged = true },
                onResetLayout = { resetLayoutCalled = true },
                onNavigateBack = { backCalled = true },
                onNavigateToAbout = { aboutCalled = true }
            )
        }

        // Verify elements are displayed
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("OLED Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Screen On").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset Dashboard Layout").assertIsDisplayed()
        composeTestRule.onNodeWithText("About LibreSpeedo").assertIsDisplayed()

        // Test toggle switches
        val switches = composeTestRule.onAllNodes(isToggleable())
        if (switches.fetchSemanticsNodes().size >= 3) {
            switches[0].performClick()
            assertTrue(oledChanged)

            switches[1].performClick()
            assertTrue(keepScreenOnChanged)

            switches[2].performClick()
            assertTrue(editModeChanged)
        }

        // Test back navigation
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backCalled)

        // Test navigate to about
        composeTestRule.onNodeWithText("About LibreSpeedo").performClick()
        assertTrue(aboutCalled)

        // Test opening and confirming reset layout dialog
        composeTestRule.onNodeWithText("Reset Dashboard Layout").performClick()
        composeTestRule.onNodeWithText("Reset Layout").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reset").performClick()
        assertTrue(resetLayoutCalled)
    }

    @Test
    fun settingsScreen_resetDialog_dismissWorks() {
        var resetLayoutCalled = false

        composeTestRule.setContent {
            SettingsScreen(
                isOledTheme = false,
                isKeepScreenOn = true,
                isEditMode = false,
                onOledThemeChanged = {},
                onKeepScreenOnChanged = {},
                onEditModeChanged = {},
                onResetLayout = { resetLayoutCalled = true },
                onNavigateBack = {},
                onNavigateToAbout = {}
            )
        }

        // Open dialog and click cancel
        composeTestRule.onNodeWithText("Reset Dashboard Layout").performClick()
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertFalse(resetLayoutCalled)
    }
}

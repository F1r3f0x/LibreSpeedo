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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plabin.librespeedo.ui.theme.LibreSpeedoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysAllOptions() {
        composeTestRule.setContent {
            LibreSpeedoTheme {
                SettingsScreen(
                    isOledTheme = false,
                    isKeepScreenOn = true,
                    onOledThemeChanged = {},
                    onKeepScreenOnChanged = {},
                    onNavigateBack = {},
                    onNavigateToAbout = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithText("OLED Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep Screen On").assertIsDisplayed()
        composeTestRule.onNodeWithText("About LibreSpeedo").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_aboutClick_triggersCallback() {
        var aboutClicked = false

        composeTestRule.setContent {
            LibreSpeedoTheme {
                SettingsScreen(
                    isOledTheme = false,
                    isKeepScreenOn = true,
                    onOledThemeChanged = {},
                    onKeepScreenOnChanged = {},
                    onNavigateBack = {},
                    onNavigateToAbout = { aboutClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("About LibreSpeedo").performClick()
        assertTrue(aboutClicked)
    }
}

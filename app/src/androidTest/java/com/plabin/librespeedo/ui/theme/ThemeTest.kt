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
package com.plabin.librespeedo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test verifying color schemes in [LibreSpeedoTheme].
 */
@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Verifies that enabling the OLED theme override applies pure black (#000000) to background and surface.
     */
    @Test
    fun oledTheme_whenEnabled_appliesPureBlackBackgroundAndSurface() {
        var observedBackground: Color = Color.Unspecified
        var observedSurface: Color = Color.Unspecified

        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = true, isOledTheme = true, dynamicColor = false) {
                observedBackground = MaterialTheme.colorScheme.background
                observedSurface = MaterialTheme.colorScheme.surface
            }
        }

        // Verify that OLED mode changes theme background and surface directly to pure black (#000000)
        assertEquals(Color.Black, observedBackground)
        assertEquals(Color.Black, observedSurface)
    }

    @Test
    fun oledTheme_whenDisabled_appliesStandardSlateBackground() {
        var observedBackground: Color = Color.Unspecified

        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = true, isOledTheme = false, dynamicColor = false) {
                observedBackground = MaterialTheme.colorScheme.background
            }
        }

        // Verify that non-OLED mode uses the standard SlateBackground (#0E1626) instead of pure black
        assertNotEquals(Color.Black, observedBackground)
        assertEquals(SlateBackground, observedBackground)
    }
}

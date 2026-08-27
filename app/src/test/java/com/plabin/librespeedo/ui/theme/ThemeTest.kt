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
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests verifying theme configuration, color tokens, and typography setup.
 */
@RunWith(RobolectricTestRunner::class)
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun colorTokens_haveExpectedValues() {
        assertEquals(Color(0xFF236077), TealPrimary)
        assertEquals(Color(0xFFF29938), AmberAccent)
        assertEquals(Color(0xFF1E2A35), SlateBackground)
        assertEquals(Color(0xFFF1F5F9), TextWhite)
        assertEquals(Color(0xFF2B3945), SurfaceDark)
    }

    @Test
    fun typography_hasExpectedDefaults() {
        assertNotNull(Typography.bodyLarge)
        assertEquals(16.sp, Typography.bodyLarge.fontSize)
        assertEquals(24.sp, Typography.bodyLarge.lineHeight)
    }

    @Test
    fun libreSpeedoTheme_darkTheme_appliesDarkColorScheme() {
        var primaryColor: Color? = null
        var backgroundColor: Color? = null
        var surfaceContainerColor: Color? = null
        var onSurfaceColor: Color? = null

        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = true, isOledTheme = false) {
                primaryColor = MaterialTheme.colorScheme.primary
                backgroundColor = MaterialTheme.colorScheme.background
                surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
                onSurfaceColor = MaterialTheme.colorScheme.onSurface
                Text("Theme Test")
            }
        }

        assertEquals(TealPrimary, primaryColor)
        assertEquals(SlateBackground, backgroundColor)
        assertEquals(SurfaceDark, surfaceContainerColor)
        assertEquals(TextWhite, onSurfaceColor)
    }

    @Test
    fun libreSpeedoTheme_oledTheme_appliesOledColorScheme() {
        var backgroundColor: Color? = null
        var surfaceContainerColor: Color? = null

        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = true, isOledTheme = true) {
                backgroundColor = MaterialTheme.colorScheme.background
                surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
                Text("OLED Test")
            }
        }

        assertEquals(Color.Black, backgroundColor)
        assertEquals(Color(0xFF121212), surfaceContainerColor)
    }

    @Test
    fun libreSpeedoTheme_lightTheme_appliesLightColorScheme() {
        var primaryColor: Color? = null
        var surfaceContainerColor: Color? = null
        var onSurfaceColor: Color? = null

        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = false, isOledTheme = false) {
                primaryColor = MaterialTheme.colorScheme.primary
                surfaceContainerColor = MaterialTheme.colorScheme.surfaceContainer
                onSurfaceColor = MaterialTheme.colorScheme.onSurface
                Text("Light Test")
            }
        }

        assertEquals(TealPrimary, primaryColor)
        assertEquals(SurfaceDark, surfaceContainerColor)
        assertEquals(TextWhite, onSurfaceColor)
    }

    @Test
    fun libreSpeedoTheme_dynamicDarkColor_rendersSuccessfully() {
        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = true, isOledTheme = false, dynamicColor = true) {
                Text("Dynamic Dark")
            }
        }
    }

    @Test
    fun libreSpeedoTheme_dynamicLightColor_rendersSuccessfully() {
        composeTestRule.setContent {
            LibreSpeedoTheme(darkTheme = false, isOledTheme = false, dynamicColor = true) {
                Text("Dynamic Light")
            }
        }
    }
}

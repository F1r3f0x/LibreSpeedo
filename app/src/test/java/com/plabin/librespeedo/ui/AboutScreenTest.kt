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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests verifying the [AboutScreen] elements, click handlers, and navigation actions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h2000dp", sdk = [34])
class AboutScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aboutScreen_displaysAllContent() {
        var backClicked = false
        var licenseClicked = false

        composeTestRule.setContent {
            AboutScreen(
                onNavigateBack = { backClicked = true },
                onViewLicense = { licenseClicked = true }
            )
        }

        composeTestRule.onNodeWithText("About LibreSpeedo").assertExists()
        composeTestRule.onNodeWithText("Patricio Labin Correa (f1r3f0x)").assertExists()
        composeTestRule.onNodeWithText("GitHub Repository").assertExists()
        composeTestRule.onNodeWithText("View GPLv3 License").assertExists()

        // Test back navigation button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        assertTrue(backClicked)

        // Test view license button
        composeTestRule.onNodeWithText("View GPLv3 License").performClick()
        assertTrue(licenseClicked)

        // Test clicking github repository link
        composeTestRule.onNodeWithText("GitHub Repository").performClick()
    }
}

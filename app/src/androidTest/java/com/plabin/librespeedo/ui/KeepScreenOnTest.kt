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

import android.view.WindowManager
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.plabin.librespeedo.MainActivity
import com.plabin.librespeedo.data.SettingsRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test verifying that toggling Keep Screen On updates [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON].
 */
@RunWith(AndroidJUnit4::class)
class KeepScreenOnTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Verifies that modifying the Keep Screen On preference dynamically adds and removes the window flag.
     */
    @Test
    fun keepScreenOn_settingToggled_modifiesWindowFlags() {
        val activity = composeTestRule.activity
        val repository = SettingsRepository(activity)

        // Enable Keep Screen On
        composeTestRule.runOnUiThread {
            repository.setKeepScreenOn(true)
        }
        composeTestRule.waitForIdle()

        var flags = activity.window.attributes.flags
        var isFlagPresent = (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        assertTrue("Expected FLAG_KEEP_SCREEN_ON to be set on activity window", isFlagPresent)

        // Disable Keep Screen On
        composeTestRule.runOnUiThread {
            repository.setKeepScreenOn(false)
        }
        composeTestRule.waitForIdle()

        flags = activity.window.attributes.flags
        isFlagPresent = (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        assertFalse("Expected FLAG_KEEP_SCREEN_ON to be cleared from activity window", isFlagPresent)
    }
}

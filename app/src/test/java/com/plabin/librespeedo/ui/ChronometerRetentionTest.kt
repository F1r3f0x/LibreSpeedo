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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.plabin.librespeedo.ui.components.ChronometerComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChronometerRetentionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun stopwatch_keepsRunningWhileWidgetLeavesComposition() {
        var now = 1_000L
        val state = ChronometerState { now }
        val visible = mutableStateOf(true)
        state.toggleRunning()
        compose.setContent {
            if (visible.value) ChronometerComponent(state = state)
        }
        compose.runOnIdle {
            now += 2_000L
            state.recordLap()
            visible.value = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            now += 10_000L
            visible.value = true
        }
        compose.onNodeWithText("00:12.00").assertExists()
        compose.onNodeWithText("Last Lap: 00:02.00 (1 laps)").assertExists()
        compose.onNodeWithContentDescription("Pause Chronometer").assertExists()
    }
}

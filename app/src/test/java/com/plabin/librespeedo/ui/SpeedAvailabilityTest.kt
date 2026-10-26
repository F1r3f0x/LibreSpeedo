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
import androidx.compose.ui.test.onNodeWithText
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.ui.components.SpeedComponent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeedAvailabilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unavailableSpeed_isNotDisplayedAsZeroOrLastKnownSpeed() {
        val state = mutableStateOf(SpeedometerUiState(
            speedKmh = 72f, isLocationFresh = true, isSpeedAvailable = true
        ))
        compose.setContent { SpeedComponent(state.value, SpeedUnit.KMH) }
        compose.onNodeWithText("72.0").assertExists()
        compose.runOnIdle { state.value = state.value.copy(isLocationFresh = false) }
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("72.0").assertDoesNotExist()
        compose.runOnIdle {
            state.value = state.value.copy(speedKmh = 0f, isLocationFresh = true, isSpeedAvailable = false)
        }
        compose.onNodeWithText("—").assertExists()
        compose.onNodeWithText("0.0").assertDoesNotExist()
    }
}

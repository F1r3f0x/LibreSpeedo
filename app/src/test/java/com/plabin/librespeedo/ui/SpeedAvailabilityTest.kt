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
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.plabin.librespeedo.data.SpeedUnit
import com.plabin.librespeedo.ui.components.SpeedComponent
import com.plabin.librespeedo.ui.components.AnalogSpeedometerComponent
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
        compose.setContent {
            Column {
                SpeedComponent(state.value, SpeedUnit.KMH)
                AnalogSpeedometerComponent(state.value, SpeedUnit.KMH)
            }
        }
        compose.onAllNodesWithText("72.0").assertCountEquals(2)
        compose.onNodeWithContentDescription("Speed gauge").assertExists()
        compose.runOnIdle { state.value = state.value.copy(isLocationFresh = false) }
        compose.onAllNodesWithText("—").assertCountEquals(2)
        compose.onNodeWithContentDescription("Speed unavailable").assertExists()
        compose.onNodeWithText("72.0").assertDoesNotExist()
        compose.runOnIdle {
            state.value = state.value.copy(speedKmh = 0f, isLocationFresh = true, isSpeedAvailable = false)
        }
        compose.onAllNodesWithText("—").assertCountEquals(2)
        compose.onNodeWithText("0.0").assertDoesNotExist()
        compose.runOnIdle {
            state.value = state.value.copy(isSpeedAvailable = true)
        }
        compose.onAllNodesWithText("0.0").assertCountEquals(2)
        compose.onNodeWithContentDescription("Speed gauge").assertExists()
    }
}

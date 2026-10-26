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

import org.junit.Assert.*
import org.junit.Test

class ChronometerStateTest {
    @Test
    fun elapsedTime_survivesTimeAwayAndPauseResume() {
        var now = 1_000L
        val state = ChronometerState { now }
        state.toggleRunning()
        now += 2_000L
        state.recordLap()
        // No UI or ticker needs to run while the widget is absent.
        now += 10_000L
        assertEquals(12_000L, state.elapsedMillis)
        assertEquals(listOf(2_000L), state.laps)

        state.toggleRunning()
        now += 5_000L
        assertEquals(12_000L, state.elapsedMillis)
        state.toggleRunning()
        now += 500L
        assertEquals(12_500L, state.elapsedMillis)
        state.reset()
        assertFalse(state.isRunning)
        assertEquals(0L, state.elapsedMillis)
        assertTrue(state.laps.isEmpty())
    }
}

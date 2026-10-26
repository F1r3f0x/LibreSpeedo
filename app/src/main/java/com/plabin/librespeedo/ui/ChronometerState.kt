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

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Timing state owned by the dashboard ViewModel, independent of widget composition. */
class ChronometerState(private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime) {
    private var accumulatedMillis by mutableLongStateOf(0L)
    private var startedAt by mutableStateOf<Long?>(null)
    var laps by mutableStateOf<List<Long>>(emptyList())
        private set

    val isRunning: Boolean get() = startedAt != null
    val elapsedMillis: Long
        get() = accumulatedMillis + (startedAt?.let { elapsedRealtime() - it } ?: 0L)

    fun toggleRunning() {
        if (isRunning) {
            accumulatedMillis = elapsedMillis
            startedAt = null
        } else {
            startedAt = elapsedRealtime()
        }
    }

    fun recordLap() {
        if (isRunning) laps = laps + elapsedMillis
    }

    fun reset() {
        startedAt = null
        accumulatedMillis = 0L
        laps = emptyList()
    }
}

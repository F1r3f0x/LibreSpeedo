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
package com.plabin.librespeedo.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedConverterTest {

    @Test
    fun msToKmh_zero_returnsZero() {
        val result = SpeedConverter.msToKmh(0f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun msToKmh_positiveValue_convertsCorrectly() {
        // 10 m/s = 36 km/h
        val result = SpeedConverter.msToKmh(10f)
        assertEquals(36f, result, 0.001f)
    }

    @Test
    fun msToKmh_hundredKmh_convertsCorrectly() {
        // 27.777778 m/s ≈ 100 km/h
        val result = SpeedConverter.msToKmh(27.777778f)
        assertEquals(100f, result, 0.01f)
    }

    @Test
    fun msToMph_zero_returnsZero() {
        val result = SpeedConverter.msToMph(0f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun msToMph_positiveValue_convertsCorrectly() {
        // 10 m/s ≈ 22.3694 mph
        val result = SpeedConverter.msToMph(10f)
        assertEquals(22.3694f, result, 0.001f)
    }

    @Test
    fun msToKnots_zero_returnsZero() {
        val result = SpeedConverter.msToKnots(0f)
        assertEquals(0f, result, 0.001f)
    }

    @Test
    fun msToKnots_positiveValue_convertsCorrectly() {
        // 10 m/s ≈ 19.4384 knots
        val result = SpeedConverter.msToKnots(10f)
        assertEquals(19.4384f, result, 0.001f)
    }
}

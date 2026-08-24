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
package com.plabin.librespeedo.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying [WidgetSpan] dimensions, labels, and cycle transitions,
 * as well as [SpeedUnit] definitions.
 */
class WidgetSpanTest {

    /**
     * Verifies that [WidgetSpan.HALF] has expected 1-column span, 160dp height, and "1x1" label.
     */
    @Test
    fun halfSpan_hasExpectedProperties() {
        val span = WidgetSpan.HALF
        assertEquals(1, span.colSpan)
        assertEquals(160, span.heightDp)
        assertEquals("1x1", span.label)
    }

    /**
     * Verifies that [WidgetSpan.FULL_WIDTH] has expected 2-column span, 180dp height, and "2x1" label.
     */
    @Test
    fun fullWidthSpan_hasExpectedProperties() {
        val span = WidgetSpan.FULL_WIDTH
        assertEquals(2, span.colSpan)
        assertEquals(180, span.heightDp)
        assertEquals("2x1", span.label)
    }

    /**
     * Verifies that [WidgetSpan.LARGE] has expected 2-column span, 280dp height, and "2x2" label.
     */
    @Test
    fun largeSpan_hasExpectedProperties() {
        val span = WidgetSpan.LARGE
        assertEquals(2, span.colSpan)
        assertEquals(280, span.heightDp)
        assertEquals("2x2", span.label)
    }

    /**
     * Verifies that cycling spans progresses in order: HALF -> FULL_WIDTH -> LARGE -> HALF.
     */
    @Test
    fun next_cyclesThroughSpansCorrectly() {
        assertEquals(WidgetSpan.FULL_WIDTH, WidgetSpan.HALF.next())
        assertEquals(WidgetSpan.LARGE, WidgetSpan.FULL_WIDTH.next())
        assertEquals(WidgetSpan.HALF, WidgetSpan.LARGE.next())
    }

    /**
     * Verifies valueOf and entries for [SpeedUnit].
     */
    @Test
    fun speedUnit_entriesAreValid() {
        assertEquals(3, SpeedUnit.entries.size)
        assertEquals(SpeedUnit.KMH, SpeedUnit.valueOf("KMH"))
        assertEquals(SpeedUnit.MPH, SpeedUnit.valueOf("MPH"))
        assertEquals(SpeedUnit.MS, SpeedUnit.valueOf("MS"))
    }
}

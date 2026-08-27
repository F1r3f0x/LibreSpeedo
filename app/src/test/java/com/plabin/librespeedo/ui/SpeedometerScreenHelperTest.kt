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

import com.plabin.librespeedo.data.WidgetSpan
import com.plabin.librespeedo.ui.components.formatChronometerTime
import com.plabin.librespeedo.ui.components.getCardinalDirection
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying default widget span mappings, [WidgetType] declarations, and component utility helpers.
 */
class SpeedometerScreenHelperTest {

    /**
     * Verifies default widget spans mapped for all [WidgetType] items.
     */
    @Test
    fun getDefaultWidgetSpan_returnsExpectedSpans() {
        assertEquals(WidgetSpan.FULL_WIDTH, getDefaultWidgetSpan(WidgetType.SPEEDOMETER))
        assertEquals(WidgetSpan.HALF, getDefaultWidgetSpan(WidgetType.COMPASS))
        assertEquals(WidgetSpan.FULL_WIDTH, getDefaultWidgetSpan(WidgetType.ANALOG_COMPASS))
        assertEquals(WidgetSpan.FULL_WIDTH, getDefaultWidgetSpan(WidgetType.CHRONOMETER))
        assertEquals(WidgetSpan.HALF, getDefaultWidgetSpan(WidgetType.POSITION))
        assertEquals(WidgetSpan.LARGE, getDefaultWidgetSpan(WidgetType.DEBUG))
        assertEquals(WidgetSpan.HALF, getDefaultWidgetSpan(WidgetType.HELLO_WORLD))
    }

    /**
     * Verifies all [WidgetType] enum entries and valueOf conversions.
     */
    @Test
    fun widgetType_entriesAreValid() {
        assertEquals(7, WidgetType.entries.size)
        assertEquals(WidgetType.SPEEDOMETER, WidgetType.valueOf("SPEEDOMETER"))
        assertEquals(WidgetType.COMPASS, WidgetType.valueOf("COMPASS"))
        assertEquals(WidgetType.ANALOG_COMPASS, WidgetType.valueOf("ANALOG_COMPASS"))
        assertEquals(WidgetType.CHRONOMETER, WidgetType.valueOf("CHRONOMETER"))
        assertEquals(WidgetType.POSITION, WidgetType.valueOf("POSITION"))
        assertEquals(WidgetType.DEBUG, WidgetType.valueOf("DEBUG"))
        assertEquals(WidgetType.HELLO_WORLD, WidgetType.valueOf("HELLO_WORLD"))
    }

    /**
     * Verifies cardinal direction string mapping across all angular sectors.
     */
    @Test
    fun getCardinalDirection_mapsCorrectly() {
        assertEquals("N", getCardinalDirection(0f))
        assertEquals("N", getCardinalDirection(350f))
        assertEquals("N", getCardinalDirection(10f))
        assertEquals("NE", getCardinalDirection(45f))
        assertEquals("E", getCardinalDirection(90f))
        assertEquals("SE", getCardinalDirection(135f))
        assertEquals("S", getCardinalDirection(180f))
        assertEquals("SW", getCardinalDirection(225f))
        assertEquals("W", getCardinalDirection(270f))
        assertEquals("NW", getCardinalDirection(315f))
    }

    /**
     * Verifies stopwatch millisecond time formatting.
     */
    @Test
    fun formatChronometerTime_formatsCorrectly() {
        assertEquals("00:00.00", formatChronometerTime(0L))
        assertEquals("00:05.42", formatChronometerTime(5420L))
        assertEquals("02:15.50", formatChronometerTime(135500L))
        assertEquals("01:05:30.25", formatChronometerTime(3930250L))
    }
}

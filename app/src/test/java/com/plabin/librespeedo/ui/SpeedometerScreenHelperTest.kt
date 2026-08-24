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
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying default widget span mappings and [WidgetType] declarations.
 */
class SpeedometerScreenHelperTest {

    /**
     * Verifies default widget spans mapped for all [WidgetType] items.
     */
    @Test
    fun getDefaultWidgetSpan_returnsExpectedSpans() {
        assertEquals(WidgetSpan.FULL_WIDTH, getDefaultWidgetSpan(WidgetType.SPEEDOMETER))
        assertEquals(WidgetSpan.HALF, getDefaultWidgetSpan(WidgetType.COMPASS))
        assertEquals(WidgetSpan.HALF, getDefaultWidgetSpan(WidgetType.POSITION))
        assertEquals(WidgetSpan.LARGE, getDefaultWidgetSpan(WidgetType.DEBUG))
        assertEquals(WidgetSpan.HALF, getDefaultWidgetSpan(WidgetType.HELLO_WORLD))
    }

    /**
     * Verifies all [WidgetType] enum entries and valueOf conversions.
     */
    @Test
    fun widgetType_entriesAreValid() {
        assertEquals(5, WidgetType.entries.size)
        assertEquals(WidgetType.SPEEDOMETER, WidgetType.valueOf("SPEEDOMETER"))
        assertEquals(WidgetType.COMPASS, WidgetType.valueOf("COMPASS"))
        assertEquals(WidgetType.POSITION, WidgetType.valueOf("POSITION"))
        assertEquals(WidgetType.DEBUG, WidgetType.valueOf("DEBUG"))
        assertEquals(WidgetType.HELLO_WORLD, WidgetType.valueOf("HELLO_WORLD"))
    }
}

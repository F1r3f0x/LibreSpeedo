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

/**
 * Utility functions for converting speed measurements between different units.
 */
object SpeedConverter {
    private const val MS_TO_KMH_FACTOR = 3.6f
    private const val MS_TO_MPH_FACTOR = 2.23694f
    private const val MS_TO_KNOTS_FACTOR = 1.94384f

    /**
     * Converts meters per second (m/s) to kilometers per hour (km/h).
     *
     * @param ms Speed in meters per second.
     * @return Equivalent speed in kilometers per hour.
     */
    fun msToKmh(ms: Float): Float = ms * MS_TO_KMH_FACTOR

    /**
     * Converts meters per second (m/s) to miles per hour (mph).
     *
     * @param ms Speed in meters per second.
     * @return Equivalent speed in miles per hour.
     */
    fun msToMph(ms: Float): Float = ms * MS_TO_MPH_FACTOR

    /**
     * Converts meters per second (m/s) to knots (nautical miles per hour).
     *
     * @param ms Speed in meters per second.
     * @return Equivalent speed in nautical miles per hour.
     */
    fun msToKnots(ms: Float): Float = ms * MS_TO_KNOTS_FACTOR
}

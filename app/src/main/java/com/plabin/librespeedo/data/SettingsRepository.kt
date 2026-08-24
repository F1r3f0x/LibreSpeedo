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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the supported measurement units for speed display.
 */
enum class SpeedUnit {
    /** Kilometers per hour (km/h). */
    KMH,
    /** Miles per hour (mph). */
    MPH,
    /** Meters per second (m/s). */
    MS
}

/**
 * Defines grid dimensions and heights for dashboard widgets.
 *
 * @property colSpan Number of grid columns spanned by the widget (1 or 2).
 * @property heightDp Base height of the widget card in density-independent pixels.
 * @property label Human-readable descriptor of the grid cell span (e.g., "1x1", "2x1", "2x2").
 */
enum class WidgetSpan(val colSpan: Int, val heightDp: Int, val label: String) {
    /** Half-width compact square card (1 column wide, 160 dp high). */
    HALF(colSpan = 1, heightDp = 160, label = "1x1"),
    /** Full-width horizontal banner card (2 columns wide, 180 dp high). */
    FULL_WIDTH(colSpan = 2, heightDp = 180, label = "2x1"),
    /** Full-width expanded large card (2 columns wide, 280 dp high). */
    LARGE(colSpan = 2, heightDp = 280, label = "2x2");

    /**
     * Cycles to the next span size in sequence (HALF -> FULL_WIDTH -> LARGE -> HALF).
     *
     * @return The next [WidgetSpan] in the cycle.
     */
    fun next(): WidgetSpan = when (this) {
        HALF -> FULL_WIDTH
        FULL_WIDTH -> LARGE
        LARGE -> HALF
    }
}

/**
 * A repository for persisting and observing user preferences via [SharedPreferences].
 *
 * Exposes settings as [StateFlow] streams so Jetpack Compose UI components can observe
 * and react to changes reactively.
 *
 * @param prefs The [SharedPreferences] instance used for backing storage.
 */
class SettingsRepository(private val prefs: SharedPreferences) {
    /**
     * Convenience constructor that opens the default private SharedPreferences file.
     *
     * @param context Application context used to retrieve [SharedPreferences].
     */
    constructor(context: Context) : this(
        context.getSharedPreferences("librespeedo_settings", Context.MODE_PRIVATE)
    )

    private val _isOledTheme = MutableStateFlow(prefs.getBoolean("oled_theme", false))
    /** Emits true if the pure-black OLED dark theme is enabled. */
    val isOledTheme: StateFlow<Boolean> = _isOledTheme.asStateFlow()

    private val _isKeepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", true))
    /** Emits true if the screen should be kept awake while the app is in the foreground. */
    val isKeepScreenOn: StateFlow<Boolean> = _isKeepScreenOn.asStateFlow()

    private val _isEditMode = MutableStateFlow(prefs.getBoolean("edit_mode", false))
    /** Emits true if dashboard edit mode (drag-and-drop, resizing, widget management) is active. */
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _widgetSpans = MutableStateFlow(
        prefs.getString("widget_spans", null)?.split(",")?.mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2 && parts[0].isNotBlank()) {
                try {
                    parts[0] to WidgetSpan.valueOf(parts[1])
                } catch (_: Exception) { null }
            } else null
        }?.toMap() ?: emptyMap()
    )
    /** Emits a mapping of widget names to their configured [WidgetSpan] sizes. */
    val widgetSpans: StateFlow<Map<String, WidgetSpan>> = _widgetSpans.asStateFlow()

    private val _speedUnit = MutableStateFlow(
        try {
            SpeedUnit.valueOf(prefs.getString("speed_unit", SpeedUnit.KMH.name) ?: SpeedUnit.KMH.name)
        } catch (_: Exception) {
            SpeedUnit.KMH
        }
    )
    /** Emits the user's active [SpeedUnit] preference for speedometer readouts. */
    val speedUnit: StateFlow<SpeedUnit> = _speedUnit.asStateFlow()

    /**
     * Sets the OLED theme preference and persists it to storage.
     *
     * @param enabled True to enable pure-black backgrounds, false for slate dark theme.
     */
    fun setOledTheme(enabled: Boolean) {
        prefs.edit { putBoolean("oled_theme", enabled) }
        _isOledTheme.value = enabled
    }

    /**
     * Sets the screen wake lock preference and persists it to storage.
     *
     * @param enabled True to keep screen active, false to follow system sleep timeout.
     */
    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit { putBoolean("keep_screen_on", enabled) }
        _isKeepScreenOn.value = enabled
    }

    /**
     * Toggles dashboard edit mode.
     *
     * @param enabled True to unlock dashboard editing gestures and controls.
     */
    fun setEditMode(enabled: Boolean) {
        prefs.edit { putBoolean("edit_mode", enabled) }
        _isEditMode.value = enabled
    }

    /**
     * Updates the preferred speed measurement unit.
     *
     * @param unit The [SpeedUnit] to display (KMH, MPH, MS).
     */
    fun setSpeedUnit(unit: SpeedUnit) {
        prefs.edit { putString("speed_unit", unit.name) }
        _speedUnit.value = unit
    }

    /**
     * Updates and persists the grid span size for a specific widget.
     *
     * @param widgetName The name identifier of the widget.
     * @param span The new [WidgetSpan] size configuration.
     */
    fun setWidgetSpan(widgetName: String, span: WidgetSpan) {
        val newMap = _widgetSpans.value.toMutableMap()
        newMap[widgetName] = span
        _widgetSpans.value = newMap
        val str = newMap.entries.joinToString(",") { "${it.key}:${it.value.name}" }
        prefs.edit { putString("widget_spans", str) }
    }

    /**
     * Retrieves the ordered list of active widget names from storage, or returns [defaultList] if unset.
     *
     * @param defaultList Fallback list of widget names if no custom configuration exists.
     * @return The active widget names in display order.
     */
    fun getActiveWidgets(defaultList: List<String>): List<String> {
        val stringList = prefs.getString("active_widgets", null)
        return if (stringList != null && stringList.isNotEmpty()) {
            stringList.split(",")
        } else {
            defaultList
        }
    }

    /**
     * Persists the ordered list of active widget names to storage.
     *
     * @param widgets The active widget names in desired display order.
     */
    fun setActiveWidgets(widgets: List<String>) {
        prefs.edit { putString("active_widgets", widgets.joinToString(",")) }
    }

    /**
     * Clears all dashboard layout customizations (active widgets, widget spans, and legacy heights),
     * resetting the layout back to defaults.
     */
    fun clearLayout() {
        prefs.edit {
            remove("active_widgets")
            remove("widget_spans")
            remove("widget_heights")
        }
        _widgetSpans.value = emptyMap()
    }
}

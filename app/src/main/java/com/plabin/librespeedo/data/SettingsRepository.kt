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

enum class SpeedUnit { KMH, MPH, MS }

/**
 * A simple repository for managing user preferences using SharedPreferences.
 * Exposes settings as StateFlows so the Compose UI can react instantly.
 */
class SettingsRepository(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(
        context.getSharedPreferences("librespeedo_settings", Context.MODE_PRIVATE)
    )

    private val _isOledTheme = MutableStateFlow(prefs.getBoolean("oled_theme", false))
    val isOledTheme: StateFlow<Boolean> = _isOledTheme.asStateFlow()

    private val _isKeepScreenOn = MutableStateFlow(prefs.getBoolean("keep_screen_on", true))
    val isKeepScreenOn: StateFlow<Boolean> = _isKeepScreenOn.asStateFlow()

    private val _isEditMode = MutableStateFlow(prefs.getBoolean("edit_mode", false))
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _widgetHeights = MutableStateFlow(
        prefs.getString("widget_heights", null)?.split(",")?.mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2) {
                try {
                    parts[0] to parts[1].toInt()
                } catch (_: Exception) { null }
            } else null
        }?.toMap() ?: emptyMap()
    )
    val widgetHeights: StateFlow<Map<String, Int>> = _widgetHeights.asStateFlow()

    private val _speedUnit = MutableStateFlow(
        try {
            SpeedUnit.valueOf(prefs.getString("speed_unit", SpeedUnit.KMH.name) ?: SpeedUnit.KMH.name)
        } catch (_: Exception) {
            SpeedUnit.KMH
        }
    )
    val speedUnit: StateFlow<SpeedUnit> = _speedUnit.asStateFlow()

    fun setOledTheme(enabled: Boolean) {
        prefs.edit { putBoolean("oled_theme", enabled) }
        _isOledTheme.value = enabled
    }

    fun setKeepScreenOn(enabled: Boolean) {
        prefs.edit { putBoolean("keep_screen_on", enabled) }
        _isKeepScreenOn.value = enabled
    }

    fun setEditMode(enabled: Boolean) {
        prefs.edit { putBoolean("edit_mode", enabled) }
        _isEditMode.value = enabled
    }

    fun setSpeedUnit(unit: SpeedUnit) {
        prefs.edit { putString("speed_unit", unit.name) }
        _speedUnit.value = unit
    }

    fun setWidgetHeight(widgetName: String, heightDp: Int) {
        val newMap = _widgetHeights.value.toMutableMap()
        newMap[widgetName] = heightDp
        _widgetHeights.value = newMap
        val str = newMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit { putString("widget_heights", str) }
    }

    fun getActiveWidgets(defaultList: List<String>): List<String> {
        val stringList = prefs.getString("active_widgets", null)
        return if (stringList != null && stringList.isNotEmpty()) {
            stringList.split(",")
        } else {
            defaultList
        }
    }

    fun setActiveWidgets(widgets: List<String>) {
        prefs.edit { putString("active_widgets", widgets.joinToString(",")) }
    }

    fun clearLayout() {
        prefs.edit {
            remove("active_widgets")
            remove("widget_heights")
        }
        _widgetHeights.value = emptyMap()
    }
}

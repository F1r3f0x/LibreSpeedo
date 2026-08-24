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

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        repository = SettingsRepository(fakePrefs)
    }

    @Test
    fun defaultSettings_haveExpectedInitialValues() {
        assertFalse(repository.isOledTheme.value)
        assertTrue(repository.isKeepScreenOn.value)
    }

    @Test
    fun setOledTheme_updatesStateAndPreferences() {
        repository.setOledTheme(true)

        assertTrue(repository.isOledTheme.value)
        assertTrue(fakePrefs.getBoolean("oled_theme", false))

        repository.setOledTheme(false)

        assertFalse(repository.isOledTheme.value)
        assertFalse(fakePrefs.getBoolean("oled_theme", true))
    }

    @Test
    fun setKeepScreenOn_updatesStateAndPreferences() {
        repository.setKeepScreenOn(false)

        assertFalse(repository.isKeepScreenOn.value)
        assertFalse(fakePrefs.getBoolean("keep_screen_on", true))

        repository.setKeepScreenOn(true)

        assertTrue(repository.isKeepScreenOn.value)
        assertTrue(fakePrefs.getBoolean("keep_screen_on", false))
    }

    @Test
    fun setEditMode_updatesStateAndPreferences() {
        assertFalse(repository.isEditMode.value)
        repository.setEditMode(true)
        assertTrue(repository.isEditMode.value)
        assertTrue(fakePrefs.getBoolean("edit_mode", false))
    }

    @Test
    fun setSpeedUnit_updatesStateAndPreferences() {
        assertEquals(SpeedUnit.KMH, repository.speedUnit.value)
        repository.setSpeedUnit(SpeedUnit.MPH)
        assertEquals(SpeedUnit.MPH, repository.speedUnit.value)
        assertEquals("MPH", fakePrefs.getString("speed_unit", ""))
    }

    @Test
    fun setWidgetSpan_updatesStateAndPreferences() {
        assertTrue(repository.widgetSpans.value.isEmpty())
        repository.setWidgetSpan("SPEEDOMETER", WidgetSpan.FULL_WIDTH)
        assertEquals(WidgetSpan.FULL_WIDTH, repository.widgetSpans.value["SPEEDOMETER"])
        assertEquals("SPEEDOMETER:FULL_WIDTH", fakePrefs.getString("widget_spans", ""))
        
        repository.setWidgetSpan("COMPASS", WidgetSpan.HALF)
        assertEquals(WidgetSpan.FULL_WIDTH, repository.widgetSpans.value["SPEEDOMETER"])
        assertEquals(WidgetSpan.HALF, repository.widgetSpans.value["COMPASS"])
        assertTrue(fakePrefs.getString("widget_spans", "")!!.contains("SPEEDOMETER:FULL_WIDTH"))
        assertTrue(fakePrefs.getString("widget_spans", "")!!.contains("COMPASS:HALF"))
    }

    @Test
    fun activeWidgets_readAndWriteCorrectly() {
        val defaultList = listOf("SPEEDOMETER", "COMPASS")
        assertEquals(defaultList, repository.getActiveWidgets(defaultList))

        val customList = listOf("SPEEDOMETER", "DEBUG")
        repository.setActiveWidgets(customList)
        assertEquals("SPEEDOMETER,DEBUG", fakePrefs.getString("active_widgets", ""))
        assertEquals(customList, repository.getActiveWidgets(defaultList))
    }

    @Test
    fun clearLayout_removesLayoutPreferences() {
        repository.setActiveWidgets(listOf("DEBUG"))
        repository.setWidgetSpan("SPEEDOMETER", WidgetSpan.LARGE)
        
        repository.clearLayout()
        
        assertFalse(fakePrefs.contains("active_widgets"))
        assertFalse(fakePrefs.contains("widget_spans"))
        assertTrue(repository.widgetSpans.value.isEmpty())
    }
}

class FakeSharedPreferences : SharedPreferences, SharedPreferences.Editor {
    private val values = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = this
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        if (key != null) if (value != null) values[key] = value else values.remove(key)
        return this
    }
    override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        if (key != null) if (values != null) this.values[key] = values else this.values.remove(key)
        return this
    }
    override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        if (key != null) values[key] = value
        return this
    }
    override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        if (key != null) values[key] = value
        return this
    }
    override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        if (key != null) values[key] = value
        return this
    }
    override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        if (key != null) values[key] = value
        return this
    }
    override fun remove(key: String?): SharedPreferences.Editor {
        values.remove(key)
        return this
    }
    override fun clear(): SharedPreferences.Editor {
        values.clear()
        return this
    }
    override fun commit(): Boolean = true
    override fun apply() {}
}

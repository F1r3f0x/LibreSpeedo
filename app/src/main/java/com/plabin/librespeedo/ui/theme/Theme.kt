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
package com.plabin.librespeedo.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Pure black color scheme optimized for zero-power pixel illumination on OLED displays. */
private val OledColorScheme = darkColorScheme(
    primary = TealPrimary,
    secondary = AmberAccent,
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color.Black,
    onPrimary = TextWhite,
    onSecondary = Color.Black,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextWhite
)

/** Default high-contrast dark theme utilizing slate tones to minimize glare. */
private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    secondary = AmberAccent,
    background = SlateBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDark,
    onPrimary = TextWhite,
    onSecondary = SlateBackground,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextWhite
)

/** Light theme scheme fallback (mirrors high-contrast dark scheme for outdoor visibility). */
private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = AmberAccent,
    background = SlateBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDark,
    onPrimary = TextWhite,
    onSecondary = SlateBackground,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextWhite
)

/**
 * Main application theme wrapper for LibreSpeedo.
 *
 * Configures Material 3 color palettes and typography. Enforces high-contrast dark theme
 * aesthetic with optional pure-black OLED power-saving mode.
 *
 * @param darkTheme Whether system dark theme is active.
 * @param isOledTheme Whether pure black OLED background override is enabled.
 * @param dynamicColor Whether Android 12+ dynamic wallpaper coloring should be used (defaults to false for brand consistency).
 * @param content The composable UI content hierarchy to style.
 */
@Composable
fun LibreSpeedoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isOledTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme && isOledTheme -> OledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
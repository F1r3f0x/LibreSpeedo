package com.plabin.librespeedo.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

@Composable
fun LibreSpeedoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+, but default to false for strong brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
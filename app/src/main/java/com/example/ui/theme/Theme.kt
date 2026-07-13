package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PoeticGold,
    onPrimary = Color.White,
    primaryContainer = PoeticSurfaceVariant,
    onPrimaryContainer = PoeticOnSurface,
    secondary = PoeticGoldLight,
    onSecondary = Color.White,
    background = PoeticDarkBg,
    onBackground = PoeticOnSurface,
    surface = PoeticSurface,
    onSurface = PoeticOnSurface,
    surfaceVariant = PoeticSurfaceVariant,
    onSurfaceVariant = PoeticTextSecondary,
    outline = PoeticBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            // In a light theme, we want light status/navigation bars (dark icons)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


package com.mengjizhang.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = TextLight,
    primaryContainer = PinkLight,
    onPrimaryContainer = TextPrimary,
    secondary = MintGreen,
    onSecondary = TextPrimary,
    secondaryContainer = MintGreen.copy(alpha = 0.3f),
    onSecondaryContainer = TextPrimary,
    tertiary = LavenderPurple,
    onTertiary = TextPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = BackgroundCard,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,
    error = ExpenseRed,
    onError = TextLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PinkLight,
    onPrimary = TextPrimary,
    primaryContainer = PinkDark,
    onPrimaryContainer = TextLight,
    secondary = MintGreen,
    onSecondary = TextPrimary,
    secondaryContainer = MintGreen.copy(alpha = 0.3f),
    onSecondaryContainer = TextLight,
    tertiary = LavenderPurple,
    onTertiary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextLight,
    surface = BackgroundDark.copy(alpha = 0.8f),
    onSurface = TextLight,
    surfaceVariant = BackgroundDark,
    onSurfaceVariant = TextLight.copy(alpha = 0.7f),
    error = ExpenseRed,
    onError = TextLight
)

@Composable
fun MengJiZhangTheme(
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()

    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDarkTheme
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

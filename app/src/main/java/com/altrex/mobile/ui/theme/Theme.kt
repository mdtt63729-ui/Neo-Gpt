package com.altrex.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val AltrexColorScheme = darkColorScheme(
    primary = AltrexColors.accent,
    onPrimary = Color(0xFF161616),
    secondary = AltrexColors.textSecondary,
    onSecondary = Color(0xFF161616),
    tertiary = AltrexColors.success,
    background = AltrexColors.bgApp,
    onBackground = AltrexColors.textPrimary,
    surface = AltrexColors.bgSurface,
    onSurface = AltrexColors.textPrimary,
    surfaceVariant = AltrexColors.bgSurfaceHover,
    onSurfaceVariant = AltrexColors.textSecondary,
    error = AltrexColors.danger,
    onError = Color(0xFF161616),
    outline = AltrexColors.borderSubtle,
    outlineVariant = AltrexColors.borderStrong,
)

@Composable
fun AltrexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AltrexColorScheme,
        typography = AltrexTypography,
        content = content
    )
}

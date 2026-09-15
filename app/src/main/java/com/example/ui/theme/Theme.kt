package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF082044),
    primaryContainer = Color(0xFF17345E),
    onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Color(0xFFC7C9D9),
    background = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF181818),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFC5C5C5),
    outline = Color(0xFF5E5E5E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2457A6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E7FF),
    onPrimaryContainer = Color(0xFF071A35),
    secondary = Color(0xFF5C6170),
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE9EAF0),
    onSurfaceVariant = Color(0xFF45464D),
    outline = Color(0xFF777780)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontName: String = "Inter",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = getAppTypography(fontName),
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
        ),
        content = content
    )
}

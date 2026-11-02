package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B1A66),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0E3F4E),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF501235),
    onTertiaryContainer = Color(0xFFFCE7F3),
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

@Composable
fun MuseCraftTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MuseCraftTheme(darkTheme, dynamicColor, content)


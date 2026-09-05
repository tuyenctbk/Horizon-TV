package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SleekColorScheme = lightColorScheme(
    primary = SleekBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SleekBlueContainer,
    onPrimaryContainer = SleekOnBlueContainer,
    secondary = SleekBluePrimary,
    onSecondary = Color.White,
    secondaryContainer = SleekSurfaceSecondary,
    onSecondaryContainer = SleekTextSecondary,
    tertiary = SolarGold,
    onTertiary = Color.White,
    background = SleekBackground,
    onBackground = SleekTextPrimary,
    surface = SleekSurface,
    onSurface = SleekTextPrimary,
    surfaceVariant = SleekSurfaceSecondary,
    onSurfaceVariant = SleekTextSecondary,
    outline = SleekBorder,
    outlineVariant = SleekBorderSubtle
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleekColorScheme,
        typography = Typography,
        content = content
    )
}



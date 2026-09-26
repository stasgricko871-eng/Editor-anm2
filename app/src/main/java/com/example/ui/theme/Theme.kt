package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IsaacColorScheme = darkColorScheme(
    primary = IsaacPrimary,
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0D47A1),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = IsaacGold,
    onSecondary = Color(0xFF402D00),
    secondaryContainer = Color(0xFF5C4300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = IsaacCrimson,
    onTertiary = Color.White,
    background = IsaacBackground,
    onBackground = IsaacBone,
    surface = IsaacSurface,
    onSurface = IsaacBone,
    surfaceVariant = IsaacSurfaceVariant,
    onSurfaceVariant = IsaacOnSurfaceMuted,
    outline = Color(0xFF4E4758),
    outlineVariant = Color(0xFF332E3C)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = IsaacColorScheme,
        typography = Typography,
        content = content
    )
}

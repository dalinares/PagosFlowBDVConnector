package com.pagosflow.bdvconnector.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0D5C75),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7E7FF),
    onPrimaryContainer = Color(0xFF001E2E),
    secondary = Color(0xFF007A5E),
    onSecondary = Color.White,
    surface = Color(0xFFFCFCFD),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFF0F4F8),
    onSurfaceVariant = Color(0xFF41484D),
    outline = Color(0xFF71787E),
    outlineVariant = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF86D1F0),
    onPrimary = Color(0xFF003546),
    primaryContainer = Color(0xFF004D65),
    onPrimaryContainer = Color(0xFFC7E7FF),
    secondary = Color(0xFF4EDEAE),
    onSecondary = Color(0xFF00382A),
    surface = Color(0xFF111416),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF1E2428),
    onSurfaceVariant = Color(0xFFC1C7CE),
    outline = Color(0xFF8B9297),
    outlineVariant = Color(0xFF2C3236)
)

@Composable
fun PagosFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

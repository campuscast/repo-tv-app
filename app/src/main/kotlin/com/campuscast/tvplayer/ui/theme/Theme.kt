package com.campuscast.tvplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusCastDark = darkColorScheme(
    primary = Color(0xFF5A82FF),
    onPrimary = Color(0xFFF3F7FF),
    secondary = Color(0xFF88A1D8),
    onSecondary = Color(0xFF101924),
    tertiary = Color(0xFF70CEB6),
    onTertiary = Color(0xFF062018),
    background = Color(0xFF090A0D),
    onBackground = Color(0xFFE7E9EF),
    surface = Color(0xFF10141C),
    onSurface = Color(0xFFDDE4F4),
    surfaceVariant = Color(0xFF1A2232),
    onSurfaceVariant = Color(0xFF8D9AB1),
    error = Color(0xFFF96D75),
    onError = Color(0xFF2A0A0C),
)

@Composable
fun CampusCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CampusCastDark,
        content = content,
    )
}

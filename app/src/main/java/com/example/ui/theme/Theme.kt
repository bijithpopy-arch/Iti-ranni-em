package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val ItiDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF041E2F),
    primaryContainer = Color(0xFF0C2D48),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF063729),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xFF2E1A00),
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFF451216),
    onErrorContainer = Color(0xFFFCA5A5),
    outline = Color(0xFF334155)
)

@Composable
fun ItiRanniTheme(
    content: @Composable () -> Unit
) {
    val itiColors = ItiColors()
    CompositionLocalProvider(LocalItiColors provides itiColors) {
        MaterialTheme(
            colorScheme = ItiDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val ItiDarkColorScheme = darkColorScheme(
    primary = Sky400,
    onPrimary = Slate900,
    primaryContainer = Color(0xFF0F2D46),
    onPrimaryContainer = Sky400,
    secondary = Emerald500,
    onSecondary = Slate900,
    secondaryContainer = Color(0xFF0A2E26),
    onSecondaryContainer = Emerald400,
    tertiary = Amber500,
    onTertiary = Slate900,
    tertiaryContainer = Color(0xFF33230A),
    onTertiaryContainer = Amber400,
    background = Slate900,
    onBackground = Slate50,
    surface = Slate800,
    onSurface = Slate50,
    surfaceVariant = Slate750,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate600,
    error = Red500,
    onError = Slate50,
    errorContainer = Color(0xFF361217),
    onErrorContainer = Red400
)

@Composable
fun ItiRanniTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = ItiExtendedColors()
    CompositionLocalProvider(LocalItiColors provides extendedColors) {
        MaterialTheme(
            colorScheme = ItiDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}

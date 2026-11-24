package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ItiColors(
    val skyAccent: Color = Color(0xFF38BDF8),
    val skyContainer: Color = Color(0xFF0C2D48),
    val emeraldAccent: Color = Color(0xFF10B981),
    val emeraldContainer: Color = Color(0xFF063729),
    val amberAccent: Color = Color(0xFFF59E0B),
    val amberContainer: Color = Color(0xFF3F2807),
    val redAccent: Color = Color(0xFFEF4444),
    val redContainer: Color = Color(0xFF451216),
    val violetAccent: Color = Color(0xFFA78BFA),
    val violetContainer: Color = Color(0xFF281D4D),
    val cardSurface: Color = Color(0xFF111827),
    val cardElevated: Color = Color(0xFF1E293B),
    val borderSubtle: Color = Color(0xFF283548),
    val textMuted: Color = Color(0xFF94A3B8),
    val saffron: Color = Color(0xFFFF9933),
    val flagGreen: Color = Color(0xFF138808)
)

val LocalItiColors = staticCompositionLocalOf { ItiColors() }

val DarkBg = Color(0xFF070C17)
val DarkSurface = Color(0xFF111827)
val DarkSurfaceVariant = Color(0xFF1E293B)
val DarkOnSurface = Color(0xFFF1F5F9)

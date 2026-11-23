package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Dark-Slate Palette
val Slate950 = Color(0xFF090E1A)
val Slate900 = Color(0xFF0F172A) // Primary background
val Slate800 = Color(0xFF1E293B) // Card surface
val Slate750 = Color(0xFF263348) // Elevated surface / input fill
val Slate700 = Color(0xFF334155) // Borders & dividers
val Slate600 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8) // Muted labels
val Slate300 = Color(0xFFCBD5E1)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)  // High-contrast text

// Brand & Semantic Accents
val Sky400 = Color(0xFF38BDF8)       // Primary sky-blue accent
val Sky500 = Color(0xFF0EA5E9)
val Sky900 = Color(0xFF0C4A6E)
val Emerald500 = Color(0xFF10B981)   // Secondary emerald accent / Present / Live
val Emerald400 = Color(0xFF34D399)
val Emerald950 = Color(0xFF064E3B)
val Amber500 = Color(0xFFF59E0B)     // Warning / Half-Day / Out-Pass / Pending
val Amber400 = Color(0xFFFBBF24)
val Amber950 = Color(0xFF451A03)
val Red500 = Color(0xFFEF4444)       // Error / Absent
val Red400 = Color(0xFFF87171)
val Red950 = Color(0xFF450A0A)
val Violet400 = Color(0xFFA78BFA)    // Principal review accent

// Indian National Flag Tricolour Tokens
val IndiaSaffron = Color(0xFFFF9933)
val IndiaWhite = Color(0xFFFFFFFF)
val IndiaGreen = Color(0xFF138808)
val AshokaChakraNavy = Color(0xFF000080)

@Immutable
data class ItiExtendedColors(
    val cardSurface: Color = Slate800,
    val cardElevated: Color = Slate750,
    val borderSubtle: Color = Slate700,
    val textMuted: Color = Slate400,
    val skyAccent: Color = Sky400,
    val skyContainer: Color = Color(0xFF0F2D46),
    val emeraldAccent: Color = Emerald500,
    val emeraldContainer: Color = Color(0xFF0A2E26),
    val amberAccent: Color = Amber500,
    val amberContainer: Color = Color(0xFF33230A),
    val redAccent: Color = Red500,
    val redContainer: Color = Color(0xFF361217),
    val violetAccent: Color = Violet400,
    val violetContainer: Color = Color(0xFF23193B),
    val saffron: Color = IndiaSaffron,
    val tricolourWhite: Color = IndiaWhite,
    val indiaGreen: Color = IndiaGreen,
    val ashokaNavy: Color = AshokaChakraNavy
)

val LocalItiColors = staticCompositionLocalOf { ItiExtendedColors() }

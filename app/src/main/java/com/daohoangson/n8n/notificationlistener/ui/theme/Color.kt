package com.daohoangson.n8n.notificationlistener.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Listener Design System — tokens/colors.css. Warm-grey neutrals plus a single
// green signal hue meaning "listening"; red/amber only for destructive or
// permission problems.

internal val N0 = Color(0xFFFFFFFF)
internal val N25 = Color(0xFFFBFBFA)
internal val N50 = Color(0xFFF6F6F4)
internal val N100 = Color(0xFFEEEEEB)
internal val N200 = Color(0xFFE2E2DE)
internal val N300 = Color(0xFFCDCDC8)
internal val N400 = Color(0xFFA2A29C)
internal val N500 = Color(0xFF7A7A74)
internal val N600 = Color(0xFF585853)
internal val N700 = Color(0xFF3B3B38)
internal val N800 = Color(0xFF242422)
internal val N850 = Color(0xFF1B1B1A)
internal val N900 = Color(0xFF131312)
internal val N1000 = Color(0xFF0B0B0A)

internal val Signal50 = Color(0xFFECF7F0)
internal val Signal500 = Color(0xFF1E9E5A)
internal val Signal600 = Color(0xFF17844A)

internal val Red50 = Color(0xFFFCEEEC)
internal val Red500 = Color(0xFFD2402F)
internal val Amber50 = Color(0xFFFBF4E4)
internal val Amber500 = Color(0xFFC88A0E)

/** The semantic aliases the components read; dark mode remaps these, never the ramp. */
@Immutable
data class ListenerColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surfaceInverse: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val textInverse: Color,
    val textDisabled: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val accent: Color,
    val accentPressed: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val primary: Color,
    val onPrimary: Color,
    val danger: Color,
    val dangerSoft: Color,
    val warning: Color,
    val warningSoft: Color,
    val scrim: Color,
    val statePressed: Color,
    val isDark: Boolean,
)

val LightListenerColors = ListenerColors(
    bg = N50,
    surface = N0,
    surface2 = N100,
    surfaceInverse = N900,
    text1 = N900,
    text2 = N600,
    text3 = N500,
    textInverse = N0,
    textDisabled = N400,
    border = N200,
    borderStrong = N300,
    divider = N100,
    accent = Signal500,
    accentPressed = Signal600,
    accentSoft = Signal50,
    onAccent = N0,
    primary = N900,
    onPrimary = N0,
    danger = Red500,
    dangerSoft = Red50,
    warning = Amber500,
    warningSoft = Amber50,
    scrim = Color(0x660B0B0A),
    statePressed = Color(0x14131312),
    isDark = false,
)

val DarkListenerColors = ListenerColors(
    bg = N1000,
    surface = N900,
    surface2 = N850,
    surfaceInverse = N50,
    text1 = N50,
    text2 = N400,
    text3 = N500,
    textInverse = N900,
    textDisabled = N600,
    border = N800,
    borderStrong = N700,
    divider = N850,
    accent = Color(0xFF3FB874),
    accentPressed = Color(0xFF2FA463),
    accentSoft = Color(0x243FB874),
    onAccent = N1000,
    primary = N50,
    onPrimary = N900,
    danger = Red500,
    dangerSoft = Color(0x29D2402F),
    warning = Amber500,
    warningSoft = Color(0x29C88A0E),
    scrim = Color(0x99000000),
    statePressed = Color(0x1AF6F6F4),
    isDark = true,
)

/** Muted per-app monogram tints (AppIcon fallback when an app has no launcher icon). */
internal val AppTints = listOf(
    Color(0xFF2F6F5E), Color(0xFF3B5BA9), Color(0xFF8A4F7D), Color(0xFFA2572B),
    Color(0xFF5E6B2F), Color(0xFF2F5F7A), Color(0xFF7A3E3E), Color(0xFF4F4F8F),
)

package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MillesColorTokens(
    val isDark: Boolean,
    val lime: Color,
    val limeDarkAccent: Color,
    val lavender: Color,
    val lavenderDarkAccent: Color,
    val onLime: Color,
    val onLavender: Color,
    val backgroundMain: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val scrimDark: Color
)

val DarkMillesColors = MillesColorTokens(
    isDark = true,
    lime = LimePrimary,
    limeDarkAccent = LimeDarkAccent,
    lavender = LavenderPrimary,
    lavenderDarkAccent = LavenderDarkAccent,
    onLime = OnLimeInk,
    onLavender = OnLavenderInk,
    backgroundMain = DarkBackgroundMain,
    backgroundSecondary = DarkBackgroundSecondary,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    divider = DarkDivider,
    scrimDark = Color(0xCC151611)
)

val LightMillesColors = MillesColorTokens(
    isDark = false,
    lime = LimePrimary,
    limeDarkAccent = LimeDarkAccent,
    lavender = LavenderPrimary,
    lavenderDarkAccent = LavenderDarkAccent,
    onLime = OnLimeInk,
    onLavender = OnLavenderInk,
    backgroundMain = LightBackgroundMain,
    backgroundSecondary = LightBackgroundSecondary,
    surface = LightSurface,
    surfaceElevated = LightSurfaceElevated,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    divider = LightDivider,
    scrimDark = Color(0xB8171813)
)

object MillesRadius {
    val Small = 10.dp
    val Medium = 16.dp
    val Large = 22.dp
    val Hero = 26.dp
    val Pill = 999.dp
}

object MillesSpace {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
}

val LocalMillesColors = staticCompositionLocalOf { DarkMillesColors }

object MillesTheme {
    val colors: MillesColorTokens
        @androidx.compose.runtime.Composable
        get() = LocalMillesColors.current
}

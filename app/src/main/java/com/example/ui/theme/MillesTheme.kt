package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ThemeMode

/** Design tokens - single source of truth for MILLES MUSIC. */
object Milles {
    val Black = Color(0xFF050505)
    val Surface = Color(0xFF101010)
    val SurfaceHigh = Color(0xFF1A1A1A)
    val Stroke = Color(0x1FFFFFFF)
    val Lime = Color(0xFFC6F24E)      // primary accent
    val Violet = Color(0xFF8B6CF0)    // secondary accent
    val TextSecondary = Color(0xFFA3A3A3)
}

@Immutable
data class MillesAdaptivePalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val stroke: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val lime: Color = Milles.Lime,
    val violet: Color = Milles.Violet
)

val DarkAdaptivePalette = MillesAdaptivePalette(
    isDark = true,
    background = Milles.Black,
    surface = Milles.Surface,
    surfaceHigh = Milles.SurfaceHigh,
    stroke = Milles.Stroke,
    textPrimary = Color.White,
    textSecondary = Milles.TextSecondary
)

val LightAdaptivePalette = MillesAdaptivePalette(
    isDark = false,
    background = Color(0xFFF7F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEFEFED),
    stroke = Color(0xFFE2E2E2),
    textPrimary = Color(0xFF151515),
    textSecondary = Color(0xFF737373)
)

val LocalMillesPalette = staticCompositionLocalOf { DarkAdaptivePalette }

private val DarkScheme = darkColorScheme(
    primary = Milles.Lime,
    onPrimary = Color.Black,
    secondary = Milles.Violet,
    onSecondary = Color.White,
    background = Milles.Black,
    onBackground = Color.White,
    surface = Milles.Surface,
    onSurface = Color.White,
    surfaceVariant = Milles.SurfaceHigh,
    onSurfaceVariant = Milles.TextSecondary,
    outline = Milles.Stroke
)

private val LightScheme = lightColorScheme(
    primary = Milles.Lime,
    onPrimary = Color.Black,
    secondary = Milles.Violet,
    onSecondary = Color.White,
    background = Color(0xFFF7F7F5),
    onBackground = Color(0xFF151515),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151515),
    surfaceVariant = Color(0xFFEFEFED),
    onSurfaceVariant = Color(0xFF737373),
    outline = Color(0xFFE2E2E2)
)

private val Type = Typography(
    displaySmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.6).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun MillesTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDark
    }
    val palette = if (useDark) DarkAdaptivePalette else LightAdaptivePalette

    CompositionLocalProvider(LocalMillesPalette provides palette) {
        MaterialTheme(
            colorScheme = if (useDark) DarkScheme else LightScheme,
            typography = Type,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp)
            ),
            content = content
        )
    }
}

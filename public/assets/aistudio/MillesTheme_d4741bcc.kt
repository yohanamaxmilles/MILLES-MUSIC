package com.milles.music.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens - single source of truth for the whole app. */
object Milles {
    val Black = Color(0xFF050505)
    val Surface = Color(0xFF101010)
    val SurfaceHigh = Color(0xFF1A1A1A)
    val Stroke = Color(0x1FFFFFFF)
    val Lime = Color(0xFFC6F24E)      // primary accent
    val Violet = Color(0xFF8B6CF0)    // secondary accent
    val TextSecondary = Color(0xFFA3A3A3)
}

private val Scheme = darkColorScheme(
    primary = Milles.Lime, onPrimary = Color.Black,
    secondary = Milles.Violet, onSecondary = Color.White,
    background = Milles.Black, onBackground = Color.White,
    surface = Milles.Surface, onSurface = Color.White,
    surfaceVariant = Milles.SurfaceHigh, onSurfaceVariant = Milles.TextSecondary,
    outline = Milles.Stroke
)

private val Type = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)
)

@Composable
fun MillesTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = Scheme,
    typography = Type,
    shapes = Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp)
    ),
    content = content
)

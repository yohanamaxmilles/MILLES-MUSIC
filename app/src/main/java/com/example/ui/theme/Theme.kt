package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.model.ThemeMode

private val MillesDarkMaterialBridge = darkColorScheme(
    primary = LimePrimary,
    onPrimary = OnLimeInk,
    secondary = LavenderPrimary,
    onSecondary = OnLavenderInk,
    background = DarkBackgroundMain,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider
)

private val MillesLightMaterialBridge = lightColorScheme(
    primary = LimePrimary,
    onPrimary = OnLimeInk,
    secondary = LavenderPrimary,
    onSecondary = OnLavenderInk,
    background = LightBackgroundMain,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDivider
)

@Composable
fun MillesMusicTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDark
    }

    val tokens = if (useDark) DarkMillesColors else LightMillesColors
    val bridgeScheme = if (useDark) MillesDarkMaterialBridge else MillesLightMaterialBridge

    CompositionLocalProvider(
        LocalMillesColors provides tokens,
        LocalContentColor provides tokens.textPrimary
    ) {
        MaterialTheme(
            colorScheme = bridgeScheme,
            typography = MillesTypography,
            content = content
        )
    }
}

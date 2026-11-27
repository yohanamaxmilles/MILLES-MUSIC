package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ThemeMode
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun ThemeScreen(
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val palette = LocalMillesPalette.current
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDark
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
            .testTag("theme_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                description = stringResource(R.string.cd_back),
                tint = palette.textPrimary,
                onClick = onBack
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.menu_theme),
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThemePreviewCard(
                title = stringResource(R.string.theme_dark_mode),
                isDarkPreview = true,
                selected = effectiveDark,
                onClick = { onSelectThemeMode(ThemeMode.DARK) },
                modifier = Modifier.weight(1f).testTag("theme_card_dark")
            )
            ThemePreviewCard(
                title = stringResource(R.string.theme_light_mode),
                isDarkPreview = false,
                selected = !effectiveDark,
                onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f).testTag("theme_card_light")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.surface)
                .border(1.dp, palette.stroke, RoundedCornerShape(24.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.theme_dynamic_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = stringResource(R.string.theme_dynamic_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = themeMode == ThemeMode.AUTO,
                onCheckedChange = onToggleDynamicTheme,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Milles.Lime,
                    uncheckedThumbColor = palette.textPrimary,
                    uncheckedTrackColor = palette.surfaceHigh
                ),
                modifier = Modifier.testTag("theme_dynamic_switch")
            )
        }
    }
}

@Composable
private fun ThemePreviewCard(
    title: String,
    isDarkPreview: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMillesPalette.current
    val cardBg = if (isDarkPreview) Milles.Black else Color(0xFFF7F7F5)
    val cardSurface = if (isDarkPreview) Milles.SurfaceHigh else Color(0xFFFFFFFF)
    val borderColor = if (selected) Milles.Lime else palette.stroke

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(24.dp))
                .clickable(onClick = onClick)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Milles.Violet.copy(alpha = 0.22f),
                                Milles.Lime.copy(alpha = 0.16f),
                                cardBg
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardSurface.copy(alpha = 0.85f))
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Milles.Lime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (selected) Milles.Lime else cardSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = title, tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) palette.textPrimary else palette.textSecondary
        )
    }
}

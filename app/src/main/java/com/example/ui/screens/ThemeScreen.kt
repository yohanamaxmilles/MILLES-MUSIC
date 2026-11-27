package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeMode
import com.example.ui.components.MillesSilkWavesBackdrop
import com.example.ui.components.MillesSurfaceCard
import com.example.ui.components.MillesToggleSwitch
import com.example.ui.components.MillesTopDetailBar
import com.example.ui.components.MillesWaveLogoM
import com.example.ui.theme.DarkBackgroundMain
import com.example.ui.theme.DarkDivider
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LightBackgroundMain
import com.example.ui.theme.LightDivider
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LimePrimary
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme
import com.example.ui.theme.OnLimeInk

@Composable
fun ThemeScreen(
    themeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicTheme: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val colors = MillesTheme.colors
    val systemDark = isSystemInDarkTheme()
    val effectiveDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> systemDark
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundMain)
            .testTag("theme_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        MillesSilkWavesBackdrop(
            modifier = Modifier.fillMaxSize(),
            showTopWave = false,
            showBottomWave = true,
            intensity = 0.7f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MillesTopDetailBar(
                    title = stringResource(R.string.menu_theme),
                    onBack = onBack
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Side-by-side Dark Mode and Light Mode live miniature preview cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ThemePreviewPhoneCard(
                        title = stringResource(R.string.theme_dark_mode),
                        isDarkPreview = true,
                        selected = effectiveDark,
                        onClick = { onSelectThemeMode(ThemeMode.DARK) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_card_dark")
                    )

                    ThemePreviewPhoneCard(
                        title = stringResource(R.string.theme_light_mode),
                        isDarkPreview = false,
                        selected = !effectiveDark,
                        onClick = { onSelectThemeMode(ThemeMode.LIGHT) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_card_light")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dynamic Theme (Follow system theme) toggle card
                MillesSurfaceCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    backgroundColor = colors.surface,
                    cornerRadius = MillesRadius.Large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.theme_dynamic_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.theme_dynamic_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        MillesToggleSwitch(
                            checked = themeMode == ThemeMode.AUTO,
                            onCheckedChange = onToggleDynamicTheme,
                            modifier = Modifier.testTag("theme_dynamic_switch")
                        )
                    }
                }
            }

            // Bottom Brand Signature Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MillesWaveLogoM(modifier = Modifier.size(46.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.ExtraBold)) {
                            append("MILLES ")
                        }
                        withStyle(SpanStyle(color = LimePrimary, fontWeight = FontWeight.Bold)) {
                            append("MUSIC")
                        }
                    },
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.4.sp)
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewPhoneCard(
    title: String,
    isDarkPreview: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val cardBg = if (isDarkPreview) DarkBackgroundMain else LightBackgroundMain
    val cardSurface = if (isDarkPreview) DarkSurface else LightSurface
    val cardBorder = if (selected) LimePrimary else if (isDarkPreview) DarkDivider else LightDivider

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(242.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(cardBg)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = cardBorder,
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = LimePrimary),
                    onClick = onClick
                )
                .padding(12.dp)
        ) {
            // Subtle atmospheric aura inside the miniature preview card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LavenderPrimary.copy(alpha = if (isDarkPreview) 0.28f else 0.18f),
                                LimePrimary.copy(alpha = if (isDarkPreview) 0.18f else 0.22f),
                                cardBg
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Miniature top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MillesWaveLogoM(modifier = Modifier.size(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(cardSurface)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(LavenderPrimary.copy(alpha = 0.7f))
                        )
                    }
                }

                // Miniature hero artwork block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardSurface.copy(alpha = 0.75f))
                        .padding(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .width(56.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isDarkPreview) Color(0xFFF7F7F2) else Color(0xFF171813))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(LimePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = OnLimeInk,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Miniature bottom checkmark or subtle dot
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (selected) LimePrimary else cardSurface),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = title,
                            tint = OnLimeInk,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) colors.textPrimary else colors.textSecondary
        )
    }
}

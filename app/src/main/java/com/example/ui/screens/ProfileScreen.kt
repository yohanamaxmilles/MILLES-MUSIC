package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeMode
import com.example.state.ProfileSheetType
import com.example.ui.components.MillesEqualizerBars
import com.example.ui.components.MillesSurfaceCard
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    onOpenThemeSettings: () -> Unit,
    onOpenSheet: (ProfileSheetType) -> Unit,
    onLogoutNotice: () -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("profile_screen"),
            contentPadding = PaddingValues(top = 24.dp, bottom = bottomContentPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Centered User Profile Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cover_midnight),
                        contentDescription = stringResource(R.string.user_name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .border(2.dp, colors.lime.copy(alpha = 0.7f), CircleShape)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.user_name),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(MillesRadius.Pill))
                            .background(colors.lime.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WorkspacePremium,
                            contentDescription = null,
                            tint = colors.lime,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.user_tier),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (colors.isDark) colors.lime else colors.textPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Your Music Journey Editorial Stats Card
            item {
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
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.music_journey),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.music_journey_stats),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                        MillesEqualizerBars(
                            isPlaying = false,
                            barColor = colors.lime,
                            barCount = 5,
                            barWidth = 6.dp,
                            maxHeight = 36.dp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Menu List
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    ProfileMenuItem(
                        icon = Icons.Outlined.Settings,
                        label = stringResource(R.string.menu_settings),
                        onClick = { onOpenSheet(ProfileSheetType.SETTINGS) },
                        testTag = "profile_menu_settings"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.DarkMode,
                        label = stringResource(R.string.menu_theme),
                        trailingText = themeMode.label,
                        onClick = onOpenThemeSettings,
                        testTag = "profile_menu_theme"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Notifications,
                        label = stringResource(R.string.menu_notifications),
                        onClick = { onOpenSheet(ProfileSheetType.NOTIFICATIONS) },
                        testTag = "profile_menu_notifications"
                    )
                    ProfileMenuItem(
                        icon = Icons.Outlined.Info,
                        label = stringResource(R.string.menu_about),
                        onClick = { onOpenSheet(ProfileSheetType.ABOUT) },
                        testTag = "profile_menu_about"
                    )
                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        label = stringResource(R.string.menu_help),
                        onClick = { onOpenSheet(ProfileSheetType.HELP) },
                        testTag = "profile_menu_help"
                    )
                    ProfileMenuItem(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        label = stringResource(R.string.menu_logout),
                        showChevron = false,
                        accentTint = colors.lavender,
                        onClick = onLogoutNotice,
                        testTag = "profile_menu_logout"
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    testTag: String,
    trailingText: String? = null,
    showChevron: Boolean = true,
    accentTint: Color? = null
) {
    val colors = MillesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(MillesRadius.Medium))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentTint ?: colors.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

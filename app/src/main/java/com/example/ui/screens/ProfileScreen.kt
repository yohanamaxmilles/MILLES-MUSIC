package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ThemeMode
import com.example.state.ProfileSheetType
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun ProfileScreen(
    themeMode: ThemeMode,
    onBack: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenSheet: (ProfileSheetType) -> Unit,
    onLogoutNotice: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val palette = LocalMillesPalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("profile_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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
                    "Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = palette.textPrimary
                )
            }
            Spacer(Modifier.height(20.dp))
        }

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
                        .border(2.dp, Milles.Lime.copy(alpha = 0.8f), CircleShape)
                )
                Text(
                    text = stringResource(R.string.user_name),
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                    color = palette.textPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Milles.Lime.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        tint = Milles.Lime,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.user_tier),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                        color = if (palette.isDark) Milles.Lime else palette.textPrimary
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.stroke, RoundedCornerShape(24.dp))
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.music_journey),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.music_journey_stats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.height(34.dp)
                ) {
                    listOf(0.35f, 0.55f, 0.45f, 0.8f, 1.0f).forEach { h ->
                        Box(
                            Modifier
                                .width(6.dp)
                                .height(34.dp * h)
                                .clip(CircleShape)
                                .background(Milles.Lime)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

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
                    accentTint = Milles.Violet,
                    onClick = onLogoutNotice,
                    testTag = "profile_menu_logout"
                )
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
    val palette = LocalMillesPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentTint ?: palette.textSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

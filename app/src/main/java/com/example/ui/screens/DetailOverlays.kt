package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AudioQuality
import com.example.model.Playlist
import com.example.model.Track
import com.example.state.ProfileSheetType
import com.example.state.toUiTrack
import com.example.ui.components.GlassIconButton
import com.example.ui.components.TrackRow
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    coverUrl: Any?,
    tracks: List<Track>,
    isCircularArtwork: Boolean,
    currentTrackId: String,
    onBack: () -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    bottomInset: Dp = 184.dp,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val palette = LocalMillesPalette.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("collection_detail_screen"),
        contentPadding = PaddingValues(bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    description = stringResource(R.string.cd_back),
                    tint = palette.textPrimary,
                    onClick = onBack
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(196.dp)
                        .clip(if (isCircularArtwork) CircleShape else RoundedCornerShape(28.dp))
                        .background(palette.surfaceHigh)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(Milles.Lime)
                            .clickable(enabled = tracks.isNotEmpty()) { onPlayAll(false) }
                            .testTag("collection_play_all"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Play", fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }

                    val unselectedBg = if (palette.isDark) Color(0x1FFFFFFF) else palette.surfaceHigh
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(unselectedBg)
                            .clickable(enabled = tracks.isNotEmpty()) { onPlayAll(true) }
                            .testTag("collection_shuffle"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = palette.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shuffle", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                    }
                }
            }
        }

        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track.toUiTrack(),
                active = track.id == currentTrackId,
                onMore = { onOpenTrackOptions(track) },
                onClick = { onPlayTrack(track) }
            )
        }
    }
}

@Composable
fun MillesSheetContainer(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val palette = LocalMillesPalette.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(palette.surface)
                .border(1.dp, palette.stroke, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(enabled = false) {}
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(palette.stroke)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    description = stringResource(R.string.action_cancel),
                    tint = palette.textPrimary,
                    onClick = onDismiss
                )
            }
            content()
        }
    }
}

@Composable
fun CreatePlaylistModal(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    val palette = LocalMillesPalette.current
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    MillesSheetContainer(
        title = stringResource(R.string.create_playlist),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val fieldColors = TextFieldDefaults.colors(
                focusedContainerColor = palette.surfaceHigh,
                unfocusedContainerColor = palette.surfaceHigh,
                focusedTextColor = palette.textPrimary,
                unfocusedTextColor = palette.textPrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Milles.Lime
            )
            TextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text(stringResource(R.string.create_playlist_hint), color = palette.textSecondary) },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("create_playlist_title_input")
            )
            TextField(
                value = subtitle,
                onValueChange = { subtitle = it },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text(stringResource(R.string.create_playlist_desc_hint), color = palette.textSecondary) },
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("create_playlist_subtitle_input")
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceHigh)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.action_cancel), fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(if (title.isNotBlank()) Milles.Lime else palette.surfaceHigh)
                        .clickable(enabled = title.isNotBlank()) { onCreate(title, subtitle) }
                        .testTag("confirm_create_playlist"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.action_create),
                        fontWeight = FontWeight.SemiBold,
                        color = if (title.isNotBlank()) Color.Black else palette.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AudioQualitySheet(
    selectedQuality: AudioQuality,
    onSelectQuality: (AudioQuality) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalMillesPalette.current
    MillesSheetContainer(
        title = stringResource(R.string.audio_quality_label),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AudioQuality.entries.forEach { quality ->
                val active = quality == selectedQuality
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surfaceHigh)
                        .border(1.dp, if (active) Milles.Lime else palette.stroke, RoundedCornerShape(20.dp))
                        .clickable { onSelectQuality(quality) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(quality.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(quality.subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                    if (active) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Milles.Lime),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackOptionsSheet(
    track: Track,
    isFavorite: Boolean,
    playlists: List<Playlist>,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalMillesPalette.current
    MillesSheetContainer(title = track.title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = track.artwork.imageRes,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                )
                Column {
                    Text(track.artist, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text("${track.album} · ${track.genre}", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.surfaceHigh)
                    .clickable {
                        onToggleFavorite()
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Milles.Lime else palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (isFavorite) "Remove from Liked Songs" else "Save to Liked Songs",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
            }

            Text("Add to Playlist", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.surfaceHigh)
                            .clickable { onAddToPlaylist(pl.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = Milles.Violet, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(pl.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                        Text("${pl.displayedSongCount} songs", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailSheet(
    type: ProfileSheetType,
    notificationsEnabled: Boolean,
    audioQuality: AudioQuality,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenAudioQuality: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalMillesPalette.current
    val title = when (type) {
        ProfileSheetType.SETTINGS -> stringResource(R.string.menu_settings)
        ProfileSheetType.NOTIFICATIONS -> stringResource(R.string.menu_notifications)
        ProfileSheetType.ABOUT -> stringResource(R.string.menu_about)
        ProfileSheetType.HELP -> stringResource(R.string.menu_help)
    }

    MillesSheetContainer(title = title, onDismiss = onDismiss) {
        when (type) {
            ProfileSheetType.SETTINGS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surfaceHigh)
                        .clickable {
                            onDismiss()
                            onOpenAudioQuality()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Streaming Quality", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(audioQuality.subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                    Text(audioQuality.shortLabel, fontWeight = FontWeight.SemiBold, color = Milles.Lime)
                }
            }
            ProfileSheetType.NOTIFICATIONS -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surfaceHigh)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("New Releases & Artist Alerts", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text("Get notified when followed artists drop music", style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = Milles.Lime
                        )
                    )
                }
            }
            ProfileSheetType.ABOUT -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MILLES MUSIC · Version 1.0", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text(
                        "A different way to feel music. Crafted with a bespoke Lime & Violet editorial design system.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
            ProfileSheetType.HELP -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Support & Playback Guide", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Text(
                        "Tap any track or mix to preview live playback controls, swipe the bottom sheet in Now Playing for Up Next, Lyrics, and Related tracks, or customize your theme from Profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

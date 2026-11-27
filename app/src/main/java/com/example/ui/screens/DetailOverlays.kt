package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ArtworkDescriptor
import com.example.model.AudioQuality
import com.example.model.Playlist
import com.example.model.Track
import com.example.state.ProfileSheetType
import com.example.state.formatDuration
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesEqualizerBars
import com.example.ui.components.MillesSurfaceCard
import com.example.ui.components.MillesToggleSwitch
import com.example.ui.components.MillesTopDetailBar
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme
import com.example.ui.theme.TabularTimeStyle

@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    artwork: ArtworkDescriptor,
    tracks: List<Track>,
    isCircularArtwork: Boolean,
    currentTrackId: String,
    isPlaying: Boolean,
    favoriteTrackIds: Set<String>,
    onBack: () -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val colors = MillesTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundMain)
            .testTag("collection_detail_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MillesTopDetailBar(
                    title = "",
                    onBack = onBack
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MillesAlbumArtwork(
                        artwork = artwork,
                        contentDescription = title,
                        modifier = Modifier.size(188.dp),
                        shape = if (isCircularArtwork) CircleShape else RoundedCornerShape(MillesRadius.Hero)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(MillesRadius.Pill))
                                .background(colors.lime)
                                .clickable(
                                    enabled = tracks.isNotEmpty(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.onLime),
                                    onClick = { onPlayAll(false) }
                                )
                                .testTag("collection_play_all"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = colors.onLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_play_all),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.onLime
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(MillesRadius.Pill))
                                .background(colors.surface)
                                .border(1.dp, colors.divider, RoundedCornerShape(MillesRadius.Pill))
                                .clickable(
                                    enabled = tracks.isNotEmpty(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.lime),
                                    onClick = { onPlayAll(true) }
                                )
                                .testTag("collection_shuffle"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.action_shuffle),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            items(tracks, key = { it.id }) { track ->
                TrackListRow(
                    track = track,
                    isCurrent = track.id == currentTrackId,
                    isPlaying = isPlaying && track.id == currentTrackId,
                    isFavorite = favoriteTrackIds.contains(track.id),
                    onPlay = { onPlayTrack(track) },
                    onToggleFavorite = { onToggleFavorite(track.id) },
                    onOpenOptions = { onOpenTrackOptions(track) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
fun MillesBottomSheetContainer(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BackHandler(onBack = onDismiss)
    val colors = MillesTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(colors.backgroundSecondary)
                .border(
                    width = 1.dp,
                    color = colors.divider,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.divider)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.action_cancel),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
    val colors = MillesTheme.colors
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    MillesBottomSheetContainer(
        title = stringResource(R.string.create_playlist),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CustomSheetField(
                value = title,
                onValueChange = { title = it },
                placeholder = stringResource(R.string.create_playlist_hint),
                testTag = "create_playlist_title_input"
            )
            CustomSheetField(
                value = subtitle,
                onValueChange = { subtitle = it },
                placeholder = stringResource(R.string.create_playlist_desc_hint),
                testTag = "create_playlist_subtitle_input"
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(MillesRadius.Pill))
                        .background(colors.surface)
                        .border(1.dp, colors.divider, RoundedCornerShape(MillesRadius.Pill))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onDismiss
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(MillesRadius.Pill))
                        .background(if (title.isNotBlank()) colors.lime else colors.surfaceElevated)
                        .clickable(
                            enabled = title.isNotBlank(),
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.onLime),
                            onClick = { onCreate(title, subtitle) }
                        )
                        .testTag("confirm_create_playlist"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.action_create),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (title.isNotBlank()) colors.onLime else colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String
) {
    val colors = MillesTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(MillesRadius.Medium))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(MillesRadius.Medium))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.lime),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
fun QueueBottomSheet(
    queue: List<Track>,
    currentTrackId: String,
    isPlaying: Boolean,
    onSelectTrack: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MillesTheme.colors
    MillesBottomSheetContainer(
        title = stringResource(R.string.up_next),
        onDismiss = onDismiss
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(queue, key = { it.id }) { track ->
                val isCurrent = track.id == currentTrackId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MillesRadius.Medium))
                        .background(if (isCurrent) colors.surfaceElevated else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.lime),
                            onClick = { onSelectTrack(track) }
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MillesAlbumArtwork(
                        artwork = track.artwork,
                        contentDescription = track.title,
                        modifier = Modifier.size(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = if (isCurrent) colors.lime else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                    }
                    if (isCurrent) {
                        MillesEqualizerBars(
                            isPlaying = isPlaying,
                            barCount = 3,
                            barWidth = 3.dp,
                            maxHeight = 14.dp
                        )
                    } else {
                        Text(
                            text = formatDuration(track.durationSeconds),
                            style = TabularTimeStyle,
                            color = colors.textSecondary
                        )
                    }
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
    val colors = MillesTheme.colors
    MillesBottomSheetContainer(
        title = stringResource(R.string.audio_quality_label),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AudioQuality.entries.forEach { quality ->
                val active = quality == selectedQuality
                MillesSurfaceCard(
                    backgroundColor = if (active) colors.surfaceElevated else colors.surface,
                    borderColor = if (active) colors.lime else colors.divider,
                    cornerRadius = MillesRadius.Medium,
                    onClick = { onSelectQuality(quality) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = quality.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = quality.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                        if (active) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(colors.lime),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = colors.onLime,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
    val colors = MillesTheme.colors
    MillesBottomSheetContainer(
        title = track.title,
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MillesAlbumArtwork(
                    artwork = track.artwork,
                    contentDescription = track.title,
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                Column {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${track.album} · ${track.genre}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            MillesSurfaceCard(
                backgroundColor = colors.surface,
                cornerRadius = MillesRadius.Medium,
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) colors.lime else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isFavorite) "Remove from Favorites" else "Save to Favorites",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                }
            }

            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(playlists, key = { it.id }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MillesRadius.Medium))
                            .background(colors.surface)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.lime),
                                onClick = { onAddToPlaylist(pl.id) }
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlaylistAdd,
                            contentDescription = null,
                            tint = colors.lavender,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = pl.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${pl.displayedSongCount} songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
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
    val colors = MillesTheme.colors
    val title = when (type) {
        ProfileSheetType.SETTINGS -> stringResource(R.string.menu_settings)
        ProfileSheetType.NOTIFICATIONS -> stringResource(R.string.menu_notifications)
        ProfileSheetType.ABOUT -> stringResource(R.string.menu_about)
        ProfileSheetType.HELP -> stringResource(R.string.menu_help)
    }

    MillesBottomSheetContainer(title = title, onDismiss = onDismiss) {
        when (type) {
            ProfileSheetType.SETTINGS -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MillesSurfaceCard(
                        backgroundColor = colors.surface,
                        cornerRadius = MillesRadius.Medium,
                        onClick = {
                            onDismiss()
                            onOpenAudioQuality()
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Streaming Quality",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = audioQuality.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = audioQuality.shortLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.lime
                            )
                        }
                    }
                }
            }
            ProfileSheetType.NOTIFICATIONS -> {
                MillesSurfaceCard(
                    backgroundColor = colors.surface,
                    cornerRadius = MillesRadius.Medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "New Releases & Artist Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Get notified when followed artists drop music",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        MillesToggleSwitch(
                            checked = notificationsEnabled,
                            onCheckedChange = onToggleNotifications
                        )
                    }
                }
            }
            ProfileSheetType.ABOUT -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MILLES MUSIC · Version 1.0 (Phase 1)",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "A different way to feel music. Crafted with a bespoke Lime Green & Lavender editorial design language.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary
                    )
                }
            }
            ProfileSheetType.HELP -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Support & Playback Guide",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Tap any track or editorial mix to preview live playback controls, switch themes in Profile → Theme, or organize custom playlists in Your Library.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

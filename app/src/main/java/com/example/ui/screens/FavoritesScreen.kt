package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FavoritesFilter
import com.example.model.Track
import com.example.state.formatDuration
import com.example.ui.components.MillesPillChip
import com.example.ui.components.MillesSurfaceCard
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

@Composable
fun FavoritesScreen(
    favoriteTracks: List<Track>,
    selectedFilter: FavoritesFilter,
    currentTrackId: String,
    isPlaying: Boolean,
    onSelectFilter: (FavoritesFilter) -> Unit,
    onPlayAll: (List<Track>, Boolean) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val filteredTracks = remember(favoriteTracks, selectedFilter) {
        when (selectedFilter) {
            FavoritesFilter.ALL, FavoritesFilter.SONGS -> favoriteTracks
            FavoritesFilter.RECENT -> favoriteTracks.reversed()
            FavoritesFilter.HIGH_ENERGY -> favoriteTracks.filter { it.isUpbeat }
        }
    }
    val totalSeconds = remember(filteredTracks) { filteredTracks.sumOf { it.durationSeconds } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("favorites_screen"),
            contentPadding = PaddingValues(top = 18.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.title_favorites),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                        color = colors.textPrimary
                    )

                    MillesSurfaceCard(
                        backgroundColor = colors.surface,
                        cornerRadius = MillesRadius.Large
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colors.lavender.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = null,
                                        tint = colors.lavender,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "${filteredTracks.size} Loved Tracks",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Total runtime · ${formatDuration(totalSeconds)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(MillesRadius.Pill))
                                .background(colors.lime)
                                .clickable(
                                    enabled = filteredTracks.isNotEmpty(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.onLime),
                                    onClick = { onPlayAll(filteredTracks, false) }
                                )
                                .testTag("favorites_play_all"),
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
                                    enabled = filteredTracks.isNotEmpty(),
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.lime),
                                    onClick = { onPlayAll(filteredTracks, true) }
                                )
                                .testTag("favorites_shuffle"),
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

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(FavoritesFilter.entries, key = { it.name }) { filter ->
                        MillesPillChip(
                            label = filter.label,
                            selected = filter == selectedFilter,
                            onClick = { onSelectFilter(filter) },
                            modifier = Modifier.testTag("favorites_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (filteredTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = colors.lavender,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No favorites in this view yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Tap the heart on any track to save it here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredTracks, key = { it.id }) { track ->
                    TrackListRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        isPlaying = isPlaying && track.id == currentTrackId,
                        isFavorite = true,
                        onPlay = { onPlayTrack(track) },
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onOpenOptions = { onOpenTrackOptions(track) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

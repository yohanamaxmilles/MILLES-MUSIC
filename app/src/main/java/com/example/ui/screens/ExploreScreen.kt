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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Artist
import com.example.model.ExploreFilter
import com.example.model.GenreCategory
import com.example.model.SampleCatalog
import com.example.model.Track
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesPillChip
import com.example.ui.components.MillesSurfaceCard
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

@Composable
fun ExploreScreen(
    selectedFilter: ExploreFilter,
    currentTrackId: String,
    isPlaying: Boolean,
    favoriteTrackIds: Set<String>,
    onSelectFilter: (ExploreFilter) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenGenre: (GenreCategory) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val genrePairs = remember { SampleCatalog.genres.chunked(2) }
    val trendingTracks = remember { SampleCatalog.tracks.filter { it.isTrending } }
    val newReleases = remember { SampleCatalog.tracks.filter { it.isNewRelease } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("explore_screen"),
            contentPadding = PaddingValues(top = 18.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.title_explore),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 30.sp),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Capsule search launcher bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(MillesRadius.Pill))
                        .background(colors.surface)
                        .border(1.dp, colors.divider, RoundedCornerShape(MillesRadius.Pill))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.lime),
                            onClick = onOpenSearch
                        )
                        .padding(horizontal = 18.dp)
                        .testTag("explore_search_launcher"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary
                    )
                }
            }

            // Filter Pills
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ExploreFilter.entries, key = { it.name }) { filter ->
                        MillesPillChip(
                            label = filter.label,
                            selected = filter == selectedFilter,
                            onClick = { onSelectFilter(filter) },
                            modifier = Modifier.testTag("explore_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            when (selectedFilter) {
                ExploreFilter.GENRES -> {
                    items(genrePairs) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            pair.forEach { genre ->
                                GenreAtmosphericCard(
                                    genre = genre,
                                    onClick = { onOpenGenre(genre) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                ExploreFilter.ARTISTS -> {
                    items(SampleCatalog.artists, key = { it.id }) { artist ->
                        ArtistEditorialRow(
                            artist = artist,
                            onClick = { onOpenArtist(artist) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                ExploreFilter.TRENDING -> {
                    items(trendingTracks, key = { it.id }) { track ->
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

                ExploreFilter.NEW_RELEASES -> {
                    items(newReleases, key = { it.id }) { track ->
                        NewReleaseHighlightRow(
                            track = track,
                            onPlay = { onPlayTrack(track) },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreAtmosphericCard(
    genre: GenreCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Box(
        modifier = modifier
            .height(176.dp)
            .clip(RoundedCornerShape(MillesRadius.Large))
            .background(colors.surface)
            .border(1.dp, colors.divider.copy(alpha = 0.6f), RoundedCornerShape(MillesRadius.Large))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onClick
            )
            .testTag("genre_card_${genre.id}")
    ) {
        MillesAlbumArtwork(
            artwork = genre.artwork,
            contentDescription = genre.name,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(MillesRadius.Large)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33050505),
                            Color(0xCC050505)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = genre.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Color(0xFFF7F7F2)
            )
            Text(
                text = genre.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA7A99F)
            )
        }
    }
}

@Composable
private fun ArtistEditorialRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    MillesSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MillesAlbumArtwork(
                artwork = artist.artwork,
                contentDescription = artist.name,
                modifier = Modifier.size(58.dp),
                shape = CircleShape
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${artist.genre} · ${artist.monthlyListeners}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play),
                    tint = colors.lime,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NewReleaseHighlightRow(
    track: Track,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    MillesSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onPlay
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MillesAlbumArtwork(
                artwork = track.artwork,
                contentDescription = track.title,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MillesRadius.Pill))
                        .background(colors.lavender.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "NEW RELEASE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = colors.lavender
                    )
                }
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} · ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.lime),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play),
                    tint = colors.onLime,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.SampleCatalog
import com.example.model.Track
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesPillChip
import com.example.ui.components.MillesSearchInput
import com.example.ui.components.MillesTopDetailBar
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    bottomContentPadding: Dp,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val colors = MillesTheme.colors
    val trimmed = query.trim()

    val matchedArtists = remember(trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else SampleCatalog.artists.filter {
            it.name.contains(trimmed, ignoreCase = true) ||
                it.genre.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedTracks = remember(trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else SampleCatalog.tracks.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.artist.contains(trimmed, ignoreCase = true) ||
                it.genre.contains(trimmed, ignoreCase = true) ||
                it.album.contains(trimmed, ignoreCase = true)
        }
    }

    val matchedAlbums = remember(trimmed) {
        if (trimmed.isEmpty()) emptyList()
        else SampleCatalog.albums.filter {
            it.title.contains(trimmed, ignoreCase = true) ||
                it.artist.contains(trimmed, ignoreCase = true)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundMain)
            .testTag("search_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            MillesTopDetailBar(
                title = stringResource(R.string.title_search),
                onBack = onBack
            )

            MillesSearchInput(
                query = query,
                onQueryChange = onQueryChange,
                onSubmit = { onSubmitQuery(query) },
                placeholder = stringResource(R.string.search_placeholder),
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (trimmed.isEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = bottomContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.search_recent),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            recentSearches.forEach { term ->
                                MillesPillChip(
                                    label = term,
                                    selected = false,
                                    onClick = { onQueryChange(term) },
                                    modifier = Modifier.testTag("recent_search_$term")
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Trending Right Now",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }

                    items(SampleCatalog.tracks.filter { it.isTrending }, key = { it.id }) { track ->
                        SearchResultRow(
                            title = track.title,
                            subtitle = "Song · ${track.artist}",
                            artwork = track.artwork,
                            artworkShape = RoundedCornerShape(12.dp),
                            onClick = { onPlayTrack(track) },
                            testTag = "search_trending_${track.id}"
                        )
                    }
                }
            } else if (matchedArtists.isEmpty() && matchedTracks.isEmpty() && matchedAlbums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MusicOff,
                                contentDescription = null,
                                tint = colors.lavender,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.search_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.search_empty_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = bottomContentPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(matchedArtists, key = { "art_${it.id}" }) { artist ->
                        SearchResultRow(
                            title = artist.name,
                            subtitle = "Artist",
                            artwork = artist.artwork,
                            artworkShape = CircleShape,
                            onClick = { onOpenArtist(artist) },
                            testTag = "search_result_${artist.id}"
                        )
                    }
                    items(matchedTracks, key = { "trk_${it.id}" }) { track ->
                        SearchResultRow(
                            title = track.title,
                            subtitle = "Song · ${track.artist}",
                            artwork = track.artwork,
                            artworkShape = RoundedCornerShape(12.dp),
                            onClick = { onPlayTrack(track) },
                            testTag = "search_result_${track.id}"
                        )
                    }
                    items(matchedAlbums, key = { "alb_${it.id}" }) { album ->
                        SearchResultRow(
                            title = album.title,
                            subtitle = "Album · ${album.artist}",
                            artwork = album.artwork,
                            artworkShape = RoundedCornerShape(12.dp),
                            onClick = { onOpenAlbum(album) },
                            testTag = "search_result_${album.id}"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    artwork: ArtworkDescriptor,
    artworkShape: Shape,
    onClick: () -> Unit,
    testTag: String
) {
    val colors = MillesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MillesRadius.Medium))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onClick
            )
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillesAlbumArtwork(
            artwork = artwork,
            contentDescription = title,
            modifier = Modifier.size(54.dp),
            shape = artworkShape
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.divider, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

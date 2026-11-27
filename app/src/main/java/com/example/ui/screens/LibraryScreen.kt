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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.LibraryFilter
import com.example.model.Playlist
import com.example.model.SampleCatalog
import com.example.model.Track
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesCircleIconButton
import com.example.ui.components.MillesPillChip
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

@Composable
fun LibraryScreen(
    selectedFilter: LibraryFilter,
    playlists: List<Playlist>,
    favoriteTracks: List<Track>,
    currentTrackId: String,
    isPlaying: Boolean,
    onSelectFilter: (LibraryFilter) -> Unit,
    onOpenSearch: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onPlayTrack: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
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
                .testTag("library_screen"),
            contentPadding = PaddingValues(top = 14.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.title_library),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                        color = colors.textPrimary
                    )
                    MillesCircleIconButton(
                        icon = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.cd_search),
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("library_search_button")
                    )
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(LibraryFilter.entries, key = { it.name }) { filter ->
                        MillesPillChip(
                            label = filter.label,
                            selected = filter == selectedFilter,
                            onClick = { onSelectFilter(filter) },
                            modifier = Modifier.testTag("library_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            if (selectedFilter == LibraryFilter.PLAYLISTS) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(54.dp)
                            .clip(RoundedCornerShape(MillesRadius.Pill))
                            .background(colors.surface)
                            .border(1.dp, colors.divider, RoundedCornerShape(MillesRadius.Pill))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.lime),
                                onClick = onCreatePlaylist
                            )
                            .padding(horizontal = 20.dp)
                            .testTag("create_playlist_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = colors.lime,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.create_playlist),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary
                        )
                    }
                }

                items(playlists, key = { it.id }) { playlist ->
                    LibraryPlaylistRow(
                        playlist = playlist,
                        onClick = { onOpenPlaylist(playlist) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            if (selectedFilter == LibraryFilter.ALBUMS) {
                items(SampleCatalog.albums, key = { it.id }) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(MillesRadius.Medium))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.lime),
                                onClick = { onOpenAlbum(album) }
                            )
                            .padding(vertical = 6.dp)
                            .testTag("library_album_${album.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MillesAlbumArtwork(
                            artwork = album.artwork,
                            contentDescription = album.title,
                            modifier = Modifier.size(62.dp),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} · ${album.year}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (selectedFilter == LibraryFilter.ARTISTS) {
                items(SampleCatalog.artists, key = { it.id }) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(MillesRadius.Medium))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.lime),
                                onClick = { onOpenArtist(artist) }
                            )
                            .padding(vertical = 6.dp)
                            .testTag("library_artist_${artist.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MillesAlbumArtwork(
                            artwork = artist.artwork,
                            contentDescription = artist.name,
                            modifier = Modifier.size(60.dp),
                            shape = CircleShape
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = artist.genre,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            if (selectedFilter == LibraryFilter.LIKED_SONGS) {
                items(favoriteTracks, key = { it.id }) { track ->
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

@Composable
private fun LibraryPlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MillesRadius.Medium))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onClick
            )
            .padding(vertical = 4.dp)
            .testTag("library_playlist_${playlist.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillesAlbumArtwork(
            artwork = playlist.artwork,
            contentDescription = playlist.title,
            modifier = Modifier.size(62.dp),
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.displayedSongCount} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

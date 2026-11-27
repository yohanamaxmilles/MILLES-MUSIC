package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ProviderId
import com.example.provider.ProviderFailure
import com.example.ui.Collection
import com.example.ui.Track
import com.example.ui.components.FilterChips
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackRow
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    selectedProviderFilter: ProviderId? = null,
    onSelectProviderFilter: (ProviderId?) -> Unit = {},
    providerNotices: List<ProviderFailure> = emptyList(),
    isSearching: Boolean,
    errorMessage: String?,
    results: List<Track>,
    artists: List<Collection>,
    albums: List<Collection>,
    genres: List<Collection>,
    activeTrackId: String?,
    hasMoreResults: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onRetry: () -> Unit,
    onTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    onArtist: (Collection) -> Unit,
    onAlbum: (Collection) -> Unit,
    onGenre: (Collection) -> Unit,
    bottomInset: Dp = 184.dp
) {
    val palette = LocalMillesPalette.current
    val fill = if (palette.isDark) Color(0x24FFFFFF) else palette.surfaceHigh

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("search_screen"),
        contentPadding = PaddingValues(bottom = bottomInset)
    ) {
        item { ScreenTitle("Search") }
        item {
            TextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                shape = CircleShape,
                placeholder = { Text("Search Jamendo, Audius & YouTube", color = palette.textSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textSecondary) },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = palette.textSecondary)
                        }
                    }
                } else null,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fill,
                    unfocusedContainerColor = fill,
                    focusedTextColor = palette.textPrimary,
                    unfocusedTextColor = palette.textPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Milles.Lime
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .testTag("search_input")
            )
            Spacer(Modifier.height(12.dp))
            FilterChips(
                options = listOf<ProviderId?>(null, ProviderId.JAMENDO, ProviderId.AUDIUS, ProviderId.YOUTUBE),
                selected = selectedProviderFilter,
                label = { it?.displayName ?: "All" },
                onSelect = onSelectProviderFilter
            )
            Spacer(Modifier.height(18.dp))
        }
        if (providerNotices.isNotEmpty() && !isSearching) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.stroke, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    providerNotices.forEach { failure ->
                        Text(
                            text = "${failure.providerId.displayName}: ${failure.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        if (query.isBlank()) {
            item {
                SectionHeader("Browse by Genre")
                Spacer(Modifier.height(12.dp))
            }
            items(genres.chunked(2)) { pair ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { genre ->
                        GenreTile(genre, Modifier.weight(1f)) { onGenre(genre) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else if (isSearching) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Milles.Lime,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp)
                    )
                    Text("Searching enabled providers…", color = palette.textSecondary)
                }
            }
        } else if (errorMessage != null && results.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.stroke, RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(errorMessage, color = palette.textPrimary)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Milles.Lime)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Retry", fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                }
            }
        } else if (results.isEmpty() && artists.isEmpty() && albums.isEmpty()) {
            item {
                Text(
                    "No results for \"$query\"",
                    Modifier.padding(20.dp),
                    color = palette.textSecondary
                )
            }
        } else {
            if (artists.isNotEmpty()) {
                item {
                    SectionHeader("Artists")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(artists, key = { "sr_art_${it.id}" }) { artist ->
                            Column(
                                modifier = Modifier
                                    .width(96.dp)
                                    .clickable { onArtist(artist) }
                                    .testTag("search_artist_${artist.id}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = artist.coverUrl,
                                    contentDescription = artist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .background(palette.surfaceHigh)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    artist.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (albums.isNotEmpty()) {
                item {
                    SectionHeader("Albums")
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(albums, key = { "sr_alb_${it.id}" }) { album ->
                            Column(
                                modifier = Modifier
                                    .width(120.dp)
                                    .clickable { onAlbum(album) }
                                    .testTag("search_album_${album.id}")
                            ) {
                                AsyncImage(
                                    model = album.coverUrl,
                                    contentDescription = album.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(palette.surfaceHigh)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    album.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    album.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            if (results.isNotEmpty()) {
                item {
                    SectionHeader("Tracks")
                    Spacer(Modifier.height(6.dp))
                }
                items(results, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        active = track.id == activeTrackId,
                        onMore = { onMoreTrack(track) },
                        onClick = { onTrack(track) }
                    )
                }
                if (hasMoreResults) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.surfaceHigh)
                                    .border(1.dp, palette.stroke, CircleShape)
                                    .clickable(enabled = !isLoadingMore, onClick = onLoadMore)
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                                    .testTag("search_load_more")
                            ) {
                                Text(
                                    text = if (isLoadingMore) "Loading more…" else "Load more results",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
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
private fun GenreTile(g: Collection, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalMillesPalette.current
    Box(
        modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("genre_tile_${g.id}")
    ) {
        AsyncImage(
            model = g.coverUrl,
            contentDescription = g.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(palette.surfaceHigh)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
        )
        Text(
            g.title,
            Modifier.align(Alignment.BottomStart).padding(14.dp),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

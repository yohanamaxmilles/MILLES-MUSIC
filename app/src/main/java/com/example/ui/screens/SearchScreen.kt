package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.Collection
import com.example.ui.Track
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackRow
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    results: List<Track>,
    genres: List<Collection>,
    activeTrackId: String,
    onTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    onGenre: (Collection) -> Unit,
    isSearching: Boolean = false,
    errorMessage: String? = null,
    bottomInset: Dp = 184.dp
) {
    val uriHandler = LocalUriHandler.current
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
                placeholder = { Text("Artists, songs, albums", color = palette.textSecondary) },
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
            Spacer(Modifier.height(20.dp))
        }
        if (query.isBlank()) {
            item {
                SectionHeader("Browse all")
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
        } else if (results.isEmpty()) {
            item {
                Text(
                    when {
                        isSearching -> "Searching Jamendo catalogue…"
                        errorMessage != null -> errorMessage
                        else -> "No results for \"$query\""
                    },
                    Modifier.padding(20.dp),
                    color = palette.textSecondary
                )
            }
        } else {
            items(results, key = { it.id }) { track ->
                Column {
                    TrackRow(
                        track = track,
                        active = track.id == activeTrackId,
                        onMore = { onMoreTrack(track) },
                        onClick = { onTrack(track) }
                    )
                    if (track.providerName != null) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 76.dp, end = 20.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Provided by Jamendo", color = palette.textSecondary, fontSize = 11.sp)
                            track.providerTrackUrl?.let { url ->
                                Text(
                                    "Track page",
                                    Modifier.clickable { runCatching { uriHandler.openUri(url) } },
                                    color = Milles.Lime,
                                    fontSize = 11.sp
                                )
                            }
                            track.licenseUrl?.let { url ->
                                Text(
                                    "License",
                                    Modifier.clickable { runCatching { uriHandler.openUri(url) } },
                                    color = palette.textSecondary,
                                    fontSize = 11.sp
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

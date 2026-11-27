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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.Collection
import com.example.ui.Track
import com.example.ui.components.FilterChips
import com.example.ui.components.GlassIconButton
import com.example.ui.components.ScreenTitle
import com.example.ui.components.TrackRow
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

enum class LibrarySegment(val label: String) {
    Playlists("Playlists"),
    Albums("Albums"),
    Artists("Artists"),
    Liked("Liked Songs")
}

/** One Library for everything saved and catalog Artists/Albums. */
@Composable
fun LibraryScreen(
    liked: List<Track>,
    playlists: List<Collection>,
    albums: List<Collection>,
    artists: List<Collection>,
    isCatalogLoading: Boolean,
    catalogErrorMessage: String?,
    activeTrackId: String?,
    onRetryCatalog: () -> Unit,
    onTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    onCollection: (Collection, LibrarySegment) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlayLiked: () -> Unit,
    onShuffleLiked: () -> Unit,
    bottomInset: Dp = 184.dp
) {
    val palette = LocalMillesPalette.current
    var seg by remember { mutableStateOf(LibrarySegment.Playlists) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("library_screen"),
        contentPadding = PaddingValues(bottom = bottomInset)
    ) {
        item {
            ScreenTitle("Library") {
                GlassIconButton(
                    icon = Icons.Rounded.Add,
                    description = "Create playlist",
                    tint = palette.textPrimary,
                    onClick = onCreatePlaylist
                )
            }
        }
        item {
            FilterChips(LibrarySegment.entries, seg, { it.label }) { seg = it }
            Spacer(Modifier.height(12.dp))
        }
        if (seg == LibrarySegment.Liked) {
            if (liked.isEmpty()) {
                item {
                    LibraryEmptyCard(
                        title = "No liked songs yet",
                        subtitle = "Tap the heart icon on any track to save it here."
                    )
                }
            } else {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionPill("Play", Icons.Rounded.PlayArrow, true, Modifier.weight(1f), onPlayLiked)
                        ActionPill("Shuffle", Icons.Rounded.Shuffle, false, Modifier.weight(1f), onShuffleLiked)
                    }
                }
                items(liked, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        active = track.id == activeTrackId,
                        onMore = { onMoreTrack(track) },
                        onClick = { onTrack(track) }
                    )
                }
            }
        } else {
            val list = when (seg) {
                LibrarySegment.Playlists -> playlists
                LibrarySegment.Albums -> albums
                LibrarySegment.Artists -> artists
                LibrarySegment.Liked -> emptyList()
            }
            if (list.isEmpty()) {
                item {
                    when {
                        seg == LibrarySegment.Playlists -> {
                            LibraryEmptyCard(
                                title = "No playlists yet",
                                subtitle = "Tap + above to create your first playlist."
                            )
                        }
                        isCatalogLoading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.surface)
                                    .border(1.dp, palette.stroke, RoundedCornerShape(20.dp))
                                    .padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Milles.Lime,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Loading ${seg.label.lowercase()}…", color = palette.textPrimary)
                            }
                        }
                        catalogErrorMessage != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(palette.surface)
                                    .border(1.dp, palette.stroke, RoundedCornerShape(20.dp))
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Unable to load ${seg.label.lowercase()}", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                Text(catalogErrorMessage, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Milles.Lime)
                                        .clickable(onClick = onRetryCatalog)
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text("Retry", fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }
                            }
                        }
                        else -> {
                            LibraryEmptyCard(
                                title = "No ${seg.label.lowercase()} available",
                                subtitle = "Try refreshing when connected to music providers."
                            )
                        }
                    }
                }
            } else {
                items(list, key = { it.id }) { collection ->
                    CollectionRow(collection, round = seg == LibrarySegment.Artists) {
                        onCollection(collection, seg)
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyCard(title: String, subtitle: String) {
    val palette = LocalMillesPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.surface)
            .border(1.dp, palette.stroke, RoundedCornerShape(22.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
    }
}

@Composable
private fun ActionPill(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val palette = LocalMillesPalette.current
    val unselectedBg = if (palette.isDark) Color(0x1FFFFFFF) else palette.surfaceHigh
    Row(
        modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(if (primary) Milles.Lime else unselectedBg)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (primary) Color.Black else palette.textPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.SemiBold, color = if (primary) Color.Black else palette.textPrimary)
    }
}

@Composable
private fun CollectionRow(c: Collection, round: Boolean, onClick: () -> Unit) {
    val palette = LocalMillesPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("collection_row_${c.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = c.coverUrl,
            contentDescription = c.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(if (round) CircleShape else RoundedCornerShape(14.dp))
                .background(palette.surfaceHigh)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(c.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (c.subtitle.isNotBlank()) {
                Text(c.subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

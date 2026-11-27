package com.milles.music.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.milles.music.ui.Collection
import com.milles.music.ui.Track
import com.milles.music.ui.components.*
import com.milles.music.ui.theme.Milles

enum class LibrarySegment(val label: String) {
    Playlists("Playlists"), Albums("Albums"), Artists("Artists"), Liked("Liked Songs")
}

/** One Library for everything saved. "Favorites" is no longer a separate tab: it is "Liked Songs" here. */
@Composable
fun LibraryScreen(
    liked: List<Track>,
    playlists: List<Collection>,
    albums: List<Collection>,
    artists: List<Collection>,
    onTrack: (Track) -> Unit,
    onCollection: (Collection) -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlayLiked: () -> Unit,
    onShuffleLiked: () -> Unit,
    bottomInset: Dp = 184.dp
) {
    var seg by remember { mutableStateOf(LibrarySegment.Playlists) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset)) {
        item { ScreenTitle("Library") { GlassIconButton(Icons.Rounded.Add, "Create playlist", onClick = onCreatePlaylist) } }
        item {
            FilterChips(LibrarySegment.values().toList(), seg, { it.label }) { seg = it }
            Spacer(Modifier.height(12.dp))
        }
        if (seg == LibrarySegment.Liked) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ActionPill("Play", Icons.Rounded.PlayArrow, true, Modifier.weight(1f), onPlayLiked)
                    ActionPill("Shuffle", Icons.Rounded.Shuffle, false, Modifier.weight(1f), onShuffleLiked)
                }
            }
            items(liked, key = { it.id }) { TrackRow(it, false) { onTrack(it) } }
        } else {
            val list = when (seg) { LibrarySegment.Playlists -> playlists; LibrarySegment.Albums -> albums; else -> artists }
            items(list, key = { it.id }) { CollectionRow(it, round = seg == LibrarySegment.Artists) { onCollection(it) } }
        }
    }
}

@Composable
private fun ActionPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(48.dp).clip(CircleShape)
            .background(if (primary) Milles.Lime else Color(0x1FFFFFFF)).clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (primary) Color.Black else Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.SemiBold, color = if (primary) Color.Black else Color.White)
    }
}

@Composable
private fun CollectionRow(c: Collection, round: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(c.coverUrl, null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(60.dp).clip(if (round) CircleShape else RoundedCornerShape(14.dp)).background(Milles.SurfaceHigh))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(c.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            if (c.subtitle.isNotBlank()) Text(c.subtitle, style = MaterialTheme.typography.bodyMedium, color = Milles.TextSecondary)
        }
    }
}

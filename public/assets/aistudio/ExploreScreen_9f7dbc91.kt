package com.milles.music.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.milles.music.ui.Track
import com.milles.music.ui.components.*
import com.milles.music.ui.theme.Milles

@Composable
fun ExploreScreen(
    newReleases: List<Track>, topCharts: List<Track>,
    onTrack: (Track) -> Unit, bottomInset: Dp = 184.dp
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { ScreenTitle("Explore") }
        item { SectionHeader("New Releases") }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(newReleases, key = { it.id }) { t ->
                    Box(Modifier.size(width = 260.dp, height = 320.dp).clip(RoundedCornerShape(28.dp)).clickable { onTrack(t) }) {
                        AsyncImage(t.coverUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(Milles.SurfaceHigh))
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xE6000000))))
                        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                            Text(t.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                            Text(t.artist, color = Color(0xCCFFFFFF))
                        }
                    }
                }
            }
        }
        item { SectionHeader("Top Charts") }
        itemsIndexed(topCharts, key = { _, t -> t.id }) { i, t -> TrackRow(t, false, rank = i + 1) { onTrack(t) } }
    }
}

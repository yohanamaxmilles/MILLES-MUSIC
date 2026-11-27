package com.milles.music.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.milles.music.ui.Collection
import com.milles.music.ui.Track
import com.milles.music.ui.components.*
import com.milles.music.ui.theme.Milles

@Composable
fun SearchScreen(
    query: String, onQuery: (String) -> Unit,
    results: List<Track>, genres: List<Collection>,
    onTrack: (Track) -> Unit, onGenre: (Collection) -> Unit,
    bottomInset: Dp = 184.dp
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = bottomInset)) {
        item { ScreenTitle("Search") }
        item {
            val fill = Color(0x24FFFFFF)
            TextField(
                value = query, onValueChange = onQuery, singleLine = true, shape = CircleShape,
                placeholder = { Text("Artists, songs, albums", color = Milles.TextSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Milles.TextSecondary) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fill, unfocusedContainerColor = fill,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Milles.Lime
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
        if (query.isBlank()) {
            item { SectionHeader("Browse all"); Spacer(Modifier.height(12.dp)) }
            items(genres.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { GenreTile(it, Modifier.weight(1f)) { onGenre(it) } }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else if (results.isEmpty()) {
            item { Text("No results for \"$query\"", Modifier.padding(20.dp), color = Milles.TextSecondary) }
        } else {
            items(results, key = { it.id }) { TrackRow(it, false) { onTrack(it) } }
        }
    }
}

@Composable
private fun GenreTile(g: Collection, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.aspectRatio(1.5f).clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)) {
        AsyncImage(g.coverUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().background(Milles.SurfaceHigh))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
        Text(g.title, Modifier.align(Alignment.BottomStart).padding(14.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

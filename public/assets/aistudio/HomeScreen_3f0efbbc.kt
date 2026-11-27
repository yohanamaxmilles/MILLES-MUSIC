package com.milles.music.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
fun HomeScreen(
    heroCover: Any?,
    avatar: Any?,
    trending: List<Track>,
    madeForYou: List<Track>,
    onPlayHero: () -> Unit,
    onTrack: (Track) -> Unit,
    onProfile: () -> Unit,
    bottomInset: Dp = 184.dp          // room for mini player + floating nav
) {
    LazyColumn(
        Modifier.fillMaxSize().background(Milles.Black),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item { // Top bar
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MILLES", style = MaterialTheme.typography.headlineSmall)
                Text(" MUSIC", style = MaterialTheme.typography.headlineSmall, color = Milles.Lime)
                Spacer(Modifier.weight(1f))
                AsyncImage(
                    avatar, "Profile", contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onProfile)
                )
            }
        }
        item { // Hero: edge-to-edge image, no inner box
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(380.dp).clip(RoundedCornerShape(32.dp))) {
                AsyncImage(heroCover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6050505)))))
                Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                    Text("Good Music,\nEvery Day.", style = MaterialTheme.typography.displaySmall)
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { PlayButton(false, 60, onPlayHero) }
            }
        }
        item { SectionHeader("Trending Now", onSeeAll = {}) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(trending, key = { it.id }) { CoverCard(it, it.artist) { onTrack(it) } }
            }
        }
        item { SectionHeader("Made for You", onSeeAll = {}) }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(madeForYou, key = { it.id }) { CoverCard(it, it.artist) { onTrack(it) } }
            }
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.Track
import com.example.ui.components.CoverCard
import com.example.ui.components.PlayButton
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun HomeScreen(
    heroCover: Any?,
    avatar: Any?,
    trending: List<Track>,
    madeForYou: List<Track>,
    isHeroPlaying: Boolean,
    onPlayHero: () -> Unit,
    onTrack: (Track) -> Unit,
    onProfile: () -> Unit,
    onSeeAllTrending: () -> Unit,
    onSeeAllMadeForYou: () -> Unit,
    bottomInset: Dp = 184.dp
) {
    val palette = LocalMillesPalette.current
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MILLES", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                Text(" MUSIC", style = MaterialTheme.typography.headlineSmall, color = Milles.Lime)
                Spacer(Modifier.weight(1f))
                AsyncImage(
                    model = avatar,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onProfile)
                        .testTag("home_profile_avatar")
                )
            }
        }
        item {
            Box(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(onClick = onPlayHero)
                    .testTag("home_hero_card")
            ) {
                AsyncImage(
                    model = heroCover,
                    contentDescription = "Good Music, Every Day.",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6050505))))
                )
                Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                    Text(
                        "Good Music,\nEvery Day.",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                }
                Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
                    PlayButton(
                        isPlaying = isHeroPlaying,
                        size = 60,
                        onClick = onPlayHero,
                        modifier = Modifier.testTag("home_hero_play")
                    )
                }
            }
        }
        item { SectionHeader("Trending Now", onSeeAll = onSeeAllTrending) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(trending, key = { it.id }) { track ->
                    CoverCard(track, track.artist) { onTrack(track) }
                }
            }
        }
        item { SectionHeader("Made for You", onSeeAll = onSeeAllMadeForYou) }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(madeForYou, key = { it.id }) { track ->
                    CoverCard(track, track.artist) { onTrack(track) }
                }
            }
        }
    }
}

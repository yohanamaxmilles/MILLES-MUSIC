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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.Collection
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
    isLoading: Boolean,
    errorMessage: String?,
    popular: List<Track>,
    recommended: List<Track>,
    newReleases: List<Track>,
    audiusTrending: List<Track> = emptyList(),
    recentlyPlayed: List<Track>,
    recentlyAddedAlbums: List<Collection>,
    genres: List<Collection>,
    isHeroPlaying: Boolean,
    onPlayHero: () -> Unit,
    onRetry: () -> Unit,
    onTrack: (Track, String) -> Unit,
    onAlbum: (Collection) -> Unit,
    onGenre: (Collection) -> Unit,
    onProfile: () -> Unit,
    onSeeAllPopular: () -> Unit,
    onSeeAllRecommended: () -> Unit,
    onSeeAllNewReleases: () -> Unit,
    onSeeAllAudiusTrending: () -> Unit = {},
    bottomInset: Dp = 184.dp
) {
    val palette = LocalMillesPalette.current
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("home_screen"),
        contentPadding = PaddingValues(top = 12.dp, bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(24.dp)
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
                    .height(340.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .clickable(
                        enabled = popular.isNotEmpty() || newReleases.isNotEmpty() || audiusTrending.isNotEmpty(),
                        onClick = onPlayHero
                    )
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
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Streaming via Jamendo & Audius",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xCCFFFFFF)
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

        if (isLoading && popular.isEmpty() && recommended.isEmpty() && newReleases.isEmpty() && audiusTrending.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.stroke, RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = Milles.Lime,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Loading live catalog…",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                }
            }
        } else if (errorMessage != null && popular.isEmpty() && recommended.isEmpty() && newReleases.isEmpty() && audiusTrending.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.stroke, RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Unable to load Jamendo catalog",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Milles.Lime)
                            .clickable(onClick = onRetry)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                            .testTag("home_retry_button")
                    ) {
                        Text("Retry", fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                }
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item { SectionHeader("Recently Played") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyPlayed, key = { "recent_${it.id}" }) { track ->
                        CoverCard(track, track.artist) { onTrack(track, "Recently Played") }
                    }
                }
            }
        }

        if (popular.isNotEmpty()) {
            item { SectionHeader("Popular Tracks", onSeeAll = onSeeAllPopular) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(popular, key = { "pop_${it.id}" }) { track ->
                        CoverCard(track, track.artist) { onTrack(track, "Popular Tracks") }
                    }
                }
            }
        }

        if (recommended.isNotEmpty()) {
            item { SectionHeader("Recommended Music", onSeeAll = onSeeAllRecommended) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recommended, key = { "rec_${it.id}" }) { track ->
                        CoverCard(track, track.artist) { onTrack(track, "Recommended Music") }
                    }
                }
            }
        }

        if (newReleases.isNotEmpty()) {
            item { SectionHeader("New Releases", onSeeAll = onSeeAllNewReleases) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(newReleases, key = { "new_${it.id}" }) { track ->
                        CoverCard(track, track.artist) { onTrack(track, "New Releases") }
                    }
                }
            }
        }

        if (audiusTrending.isNotEmpty()) {
            item { SectionHeader("Trending on Audius", onSeeAll = onSeeAllAudiusTrending) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(audiusTrending, key = { "aud_${it.id}" }) { track ->
                        CoverCard(track, track.artist) { onTrack(track, "Trending on Audius") }
                    }
                }
            }
        }

        if (recentlyAddedAlbums.isNotEmpty()) {
            item { SectionHeader("Recently Added") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(recentlyAddedAlbums, key = { "alb_${it.id}" }) { album ->
                        CollectionCoverCard(album) { onAlbum(album) }
                    }
                }
            }
        }

        if (genres.isNotEmpty()) {
            item { SectionHeader("Browse by Genre") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(genres, key = { "home_genre_${it.id}" }) { genre ->
                        val chipBg = if (palette.isDark) Color(0x1FFFFFFF) else palette.surfaceHigh
                        Text(
                            text = genre.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(chipBg)
                                .border(1.dp, palette.stroke, CircleShape)
                                .clickable { onGenre(genre) }
                                .padding(horizontal = 18.dp, vertical = 11.dp)
                                .testTag("home_genre_${genre.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionCoverCard(collection: Collection, onClick: () -> Unit) {
    val palette = LocalMillesPalette.current
    Column(
        Modifier
            .width(144.dp)
            .clickable(onClick = onClick)
            .testTag("collection_card_${collection.id}")
    ) {
        AsyncImage(
            model = collection.coverUrl,
            contentDescription = collection.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.surfaceHigh)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            collection.title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            collection.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

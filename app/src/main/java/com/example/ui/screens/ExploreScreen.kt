package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.ui.Track
import com.example.ui.components.ScreenTitle
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrackRow
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

@Composable
fun ExploreScreen(
    isLoading: Boolean,
    errorMessage: String?,
    newReleases: List<Track>,
    topCharts: List<Track>,
    activeTrackId: String?,
    hasMoreTopCharts: Boolean = false,
    isLoadingMoreTopCharts: Boolean = false,
    onLoadMoreTopCharts: () -> Unit = {},
    onRetry: () -> Unit,
    onTrack: (Track) -> Unit,
    onMoreTrack: (Track) -> Unit,
    bottomInset: Dp = 184.dp
) {
    val palette = LocalMillesPalette.current
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("explore_screen"),
        contentPadding = PaddingValues(bottom = bottomInset),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ScreenTitle("Explore") }

        if (isLoading && newReleases.isEmpty() && topCharts.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
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
                        "Loading releases and charts…",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                }
            }
        } else if (errorMessage != null && newReleases.isEmpty() && topCharts.isEmpty()) {
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
                        "Unable to load Explore",
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
                    ) {
                        Text("Retry", fontWeight = FontWeight.SemiBold, color = Color.Black)
                    }
                }
            }
        }

        if (newReleases.isNotEmpty()) {
            item { SectionHeader("New Releases") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(newReleases, key = { it.id }) { t ->
                        Box(
                            Modifier
                                .size(width = 260.dp, height = 320.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .clickable { onTrack(t) }
                                .testTag("explore_new_release_${t.id}")
                        ) {
                            AsyncImage(
                                model = t.coverUrl,
                                contentDescription = t.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(palette.surfaceHigh)
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color(0xE6000000)))
                            )
                            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                                Text(
                                    t.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(t.artist, color = Color(0xCCFFFFFF), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        if (topCharts.isNotEmpty()) {
            item { SectionHeader("Trending Now") }
            itemsIndexed(topCharts, key = { _, t -> t.id }) { i, t ->
                TrackRow(
                    track = t,
                    active = t.id == activeTrackId,
                    rank = i + 1,
                    onMore = { onMoreTrack(t) },
                    onClick = { onTrack(t) }
                )
            }
            if (hasMoreTopCharts) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.surfaceHigh)
                                .border(1.dp, palette.stroke, CircleShape)
                                .clickable(enabled = !isLoadingMoreTopCharts, onClick = onLoadMoreTopCharts)
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .testTag("explore_load_more_trending")
                        ) {
                            Text(
                                text = if (isLoadingMoreTopCharts) "Loading more…" else "Load more trending",
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

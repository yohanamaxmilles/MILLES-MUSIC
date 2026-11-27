package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Playlist
import com.example.model.SampleCatalog
import com.example.model.Track
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesCircleIconButton
import com.example.ui.components.MillesEqualizerBars
import com.example.ui.components.MillesWaveLogoM
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme
import com.example.ui.theme.TabularTimeStyle

@Composable
fun HomeScreen(
    currentTrackId: String,
    isPlaying: Boolean,
    favoriteTrackIds: Set<String>,
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit,
    onPlayHeroMix: () -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onSeeAllTrending: () -> Unit,
    onSeeAllMadeForYou: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenTrackOptions: (Track) -> Unit,
    bottomContentPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val recentTracks = remember { SampleCatalog.tracks.take(4) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("home_screen"),
            contentPadding = PaddingValues(top = 12.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Custom Brand Header Bar
            item {
                HomeTopBar(
                    onOpenSearch = onOpenSearch,
                    onOpenProfile = onOpenProfile
                )
            }

            // Editorial Hero Banner Card
            item {
                HeroFeaturedCard(
                    onPlayClick = onPlayHeroMix,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // Trending Now Horizontal Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.section_trending),
                        actionLabel = stringResource(R.string.action_see_all),
                        onActionClick = onSeeAllTrending,
                        testTag = "home_see_all_trending"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(SampleCatalog.trendingCollections, key = { it.id }) { collection ->
                            EditorialCollectionCard(
                                playlist = collection,
                                onClick = { onOpenPlaylist(collection) }
                            )
                        }
                    }
                }
            }

            // Made for You Horizontal Carousel
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SectionHeader(
                        title = stringResource(R.string.section_made_for_you),
                        actionLabel = stringResource(R.string.action_see_all),
                        onActionClick = onSeeAllMadeForYou,
                        testTag = "home_see_all_made_for_you"
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(SampleCatalog.madeForYouPlaylists, key = { it.id }) { mix ->
                            EditorialCollectionCard(
                                playlist = mix,
                                onClick = { onOpenPlaylist(mix) }
                            )
                        }
                    }
                }
            }

            // Recently Played Quick Tracks
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.section_recently_played),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    recentTracks.forEach { track ->
                        TrackListRow(
                            track = track,
                            isCurrent = track.id == currentTrackId,
                            isPlaying = isPlaying && track.id == currentTrackId,
                            isFavorite = favoriteTrackIds.contains(track.id),
                            onPlay = { onPlayTrack(track) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onOpenOptions = { onOpenTrackOptions(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    onOpenSearch: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val colors = MillesTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MillesWaveLogoM(modifier = Modifier.size(30.dp))
            Column {
                Text(
                    text = stringResource(R.string.brand_milles),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.brand_music),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp,
                        fontSize = 10.sp
                    ),
                    color = colors.lime
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MillesCircleIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.cd_search),
                onClick = onOpenSearch,
                containerColor = colors.surface,
                size = 48.dp,
                modifier = Modifier.testTag("home_search_button")
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.lime),
                        onClick = onOpenProfile
                    )
                    .testTag("home_profile_avatar"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cover_midnight),
                    contentDescription = stringResource(R.string.cd_profile),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, colors.lime.copy(alpha = 0.7f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun HeroFeaturedCard(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(268.dp)
            .clip(RoundedCornerShape(MillesRadius.Hero))
            .background(Color(0xFF1A1C15))
            .border(1.dp, colors.divider.copy(alpha = 0.5f), RoundedCornerShape(MillesRadius.Hero))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onPlayClick
            )
            .testTag("home_hero_card")
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_music),
            contentDescription = stringResource(R.string.hero_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle editorial dark scrim at bottom for effortless headline legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x33151611),
                            Color(0xD9151611)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MillesRadius.Pill))
                        .background(colors.lavender.copy(alpha = 0.22f))
                        .border(1.dp, colors.lavender.copy(alpha = 0.45f), RoundedCornerShape(MillesRadius.Pill))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF7F7F2)
                    )
                }
                Text(
                    text = stringResource(R.string.hero_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 27.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFF7F7F2)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.lime)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.onLime),
                        onClick = onPlayClick
                    )
                    .testTag("home_hero_play_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play),
                    tint = colors.onLime,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(MillesRadius.Pill))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colors.lime),
                    onClick = onActionClick
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EditorialCollectionCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Column(
        modifier = modifier
            .width(134.dp)
            .clip(RoundedCornerShape(MillesRadius.Medium))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onClick
            )
            .padding(bottom = 4.dp)
            .testTag("collection_card_${playlist.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MillesAlbumArtwork(
            artwork = playlist.artwork,
            contentDescription = playlist.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp),
            shape = RoundedCornerShape(MillesRadius.Large)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playlist.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TrackListRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MillesRadius.Medium))
            .background(
                if (isCurrent) colors.surfaceElevated.copy(alpha = 0.85f)
                else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onPlay
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillesAlbumArtwork(
            artwork = track.artwork,
            contentDescription = track.title,
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isCurrent) {
                    MillesEqualizerBars(
                        isPlaying = isPlaying,
                        barCount = 3,
                        barWidth = 2.5.dp,
                        maxHeight = 12.dp
                    )
                }
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    color = if (isCurrent) colors.lime else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${track.artist} · ${com.example.state.formatDuration(track.durationSeconds)}",
                style = TabularTimeStyle.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = colors.lime),
                    onClick = onToggleFavorite
                )
                .testTag("track_fav_${track.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = stringResource(R.string.cd_favorite),
                tint = if (isFavorite) colors.lime else colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onOpenOptions
                )
                .testTag("track_more_${track.id}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

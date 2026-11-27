package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioQuality
import com.example.model.RepeatMode
import com.example.model.SampleCatalog
import com.example.model.Track
import com.example.state.formatDuration
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesEqualizerBars
import com.example.ui.components.MillesScrubberSlider
import com.example.ui.theme.DarkBackgroundMain
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LimePrimary
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.OnLimeInk
import com.example.ui.theme.TabularTimeStyle
import kotlinx.coroutines.launch

data class SyncedLyricLine(val timeSeconds: Int, val text: String)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    track: Track,
    queue: List<Track>,
    relatedTracks: List<Track>,
    isPlaying: Boolean,
    progressSeconds: Int,
    isFavorite: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    audioQuality: AudioQuality,
    onMinimize: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenAudioQuality: () -> Unit,
    onOpenTrackOptions: () -> Unit,
    onSelectTrack: (Track) -> Unit,
    lyrics: List<SyncedLyricLine> = emptyList(),
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onMinimize)

    val scaffoldState = rememberBottomSheetScaffoldState()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Up Next", "Lyrics", "Related")

    BottomSheetScaffold(
        modifier = modifier.testTag("now_playing_screen"),
        scaffoldState = scaffoldState,
        sheetPeekHeight = 112.dp,
        sheetContainerColor = Color(0xF20B0B0B),
        sheetContentColor = Color.White,
        sheetShadowElevation = 0.dp,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0x66FFFFFF), width = 36.dp, height = 4.dp)
        },
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.78f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = pagerState.currentPage == index
                        Column(
                            modifier = Modifier
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(index)
                                        scaffoldState.bottomSheetState.expand()
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = title,
                                color = if (selected) LimePrimary else Color(0xFF929292),
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Spacer(Modifier.height(7.dp))
                            Box(
                                Modifier.width(if (selected) 22.dp else 0.dp)
                                    .height(2.dp).background(LimePrimary)
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${queue.size} tracks",
                        color = Color(0xFF858585),
                        fontSize = 11.sp
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { page ->
                    when (page) {
                        0 -> PlayerTrackList(
                            tracks = queue,
                            activeTrackId = track.id,
                            onSelectTrack = onSelectTrack
                        )
                        1 -> LyricsPanel(
                            lyrics = lyrics,
                            progressSeconds = progressSeconds
                        )
                        else -> PlayerTrackList(
                            tracks = relatedTracks,
                            activeTrackId = track.id,
                            onSelectTrack = onSelectTrack
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(DarkBackgroundMain)
        ) {
            // Claude's full-bleed cover treatment: no card, frame, or circular crop.
            MillesAlbumArtwork(
                artwork = track.artwork,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize().scale(1.06f)
                    .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(6.dp, BlurredEdgeTreatment.Unbounded) else Modifier),
                shape = RectangleShape
            )

            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color(0xCC050505),
                        0.20f to Color(0x55050505),
                        0.48f to Color(0x33050505),
                        0.72f to Color(0xCC050505),
                        1f to Color(0xFF050505)
                    )
                )
            )

            Column(
                modifier = Modifier.fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerIcon(Icons.AutoMirrored.Rounded.QueueMusic, "Minimize player", onMinimize)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "NOW PLAYING",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.2.sp
                    )
                    Spacer(Modifier.weight(1f))
                    PlayerIcon(Icons.Default.MoreVert, "Track options", onOpenTrackOptions)
                }

                Spacer(Modifier.weight(1f))

                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                track.title,
                                color = Color.White,
                                fontSize = 27.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.7).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.artist,
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        PlayerIcon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            "Favorite",
                            onToggleFavorite,
                            tint = if (isFavorite) LimePrimary else Color.White
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        MillesScrubberSlider(
                            progressSeconds = progressSeconds,
                            durationSeconds = track.durationSeconds,
                            onSeek = onSeek
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(progressSeconds), style = TabularTimeStyle, color = Color.White.copy(alpha = 0.72f))
                            Text(formatDuration(track.durationSeconds), style = TabularTimeStyle, color = Color.White.copy(alpha = 0.72f))
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PlayerIcon(Icons.Default.Shuffle, "Shuffle", onToggleShuffle,
                            tint = if (isShuffleEnabled) LimePrimary else Color.White.copy(alpha = 0.75f))
                        PlayerIcon(Icons.Default.SkipPrevious, "Previous", onPrevious, size = 32.dp)
                        Box(
                            Modifier.size(72.dp).background(LimePrimary, androidx.compose.foundation.shape.CircleShape)
                                .clickable(onClick = onTogglePlayPause).testTag("now_playing_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = OnLimeInk,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        PlayerIcon(Icons.Default.SkipNext, "Next", onNext, size = 32.dp)
                        PlayerIcon(
                            if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            "Repeat",
                            onCycleRepeat,
                            tint = if (repeatMode != RepeatMode.OFF) LavenderPrimary else Color.White.copy(alpha = 0.75f)
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            Modifier.clickable(onClick = onOpenAudioQuality),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            MillesEqualizerBars(
                                isPlaying = isPlaying,
                                barColor = LimePrimary,
                                barCount = 4,
                                barWidth = 2.5.dp,
                                maxHeight = 14.dp
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("AUDIO QUALITY", color = Color.White.copy(alpha = 0.55f), fontSize = 9.sp, letterSpacing = 1.2.sp)
                                Text(audioQuality.shortLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text(
                            "QUEUE  ·  ${queue.size}",
                            Modifier.clickable { scope.launch { scaffoldState.bottomSheetState.expand() } },
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp
                        )
                    }
                    Spacer(Modifier.height(116.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 23.dp
) {
    Box(
        Modifier.size(44.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun PlayerTrackList(
    tracks: List<Track>,
    activeTrackId: String,
    onSelectTrack: (Track) -> Unit
) {
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No tracks available yet", color = Color(0xFF888888), fontSize = 14.sp)
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        itemsIndexed(tracks, key = { _, item -> item.id }) { index, item ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelectTrack(item) }.padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    (index + 1).toString().padStart(2, '0'),
                    Modifier.width(30.dp),
                    color = if (item.id == activeTrackId) LimePrimary else Color(0xFF777777),
                    fontSize = 11.sp
                )
                MillesAlbumArtwork(
                    artwork = item.artwork,
                    contentDescription = item.title,
                    modifier = Modifier.size(46.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, color = if (item.id == activeTrackId) LimePrimary else Color.White,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.artist, color = Color(0xFF969696), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatDuration(item.durationSeconds), color = Color(0xFF777777), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun LyricsPanel(
    lyrics: List<SyncedLyricLine>,
    progressSeconds: Int
) {
    if (lyrics.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Lyrics", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Synced lyrics will appear here when available for this track.",
                    color = Color(0xFF969696), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val currentIndex = lyrics.indexOfLast { it.timeSeconds <= progressSeconds }.coerceAtLeast(0)
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val active = index == currentIndex
            Text(
                line.text,
                fontSize = if (active) 24.sp else 19.sp,
                lineHeight = if (active) 31.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (active) Color.White else Color.White.copy(alpha = 0.34f)
            )
        }
    }
}

package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.PlayerUiState
import com.example.ui.Track
import com.example.ui.components.GlassIconButton
import com.example.ui.components.PlayButton
import com.example.ui.components.TrackRow
import com.example.ui.components.glass
import com.example.ui.formatTime
import com.example.ui.theme.Milles
import kotlinx.coroutines.launch

interface PlayerActions {
    fun onBack()
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onSeek(positionMs: Long)
    fun onLike()
    fun onShuffle()
    fun onRepeat()
    fun onMore()
    fun onQualityClick()
    fun onTrackClick(track: Track)
}

/**
 * Full-screen Now Playing layout:
 *  1. Header   : close | "PLAYING FROM" + source
 *  2. Cover    : full-screen, 6dp blur, behind everything
 *  3. Info     : title + artist + album/attribution | like + more
 *  4. Scrubber : elapsed ... -remaining
 *  5. Transport: previous, PLAY (with buffering state), next
 *  6. Utilities: lyrics | audio quality | queue
 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    actions: PlayerActions
) {
    BackHandler(onBack = actions::onBack)
    var sheetTab by remember { mutableStateOf<Int?>(null) }
    val dur = state.track.durationMs.coerceAtLeast(1)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("now_playing_screen")
    ) {
        AsyncImage(
            model = state.track.coverUrl,
            contentDescription = state.track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.06f)
                .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(6.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xA6000000),
                        0.22f to Color.Transparent,
                        0.45f to Color.Transparent,
                        0.78f to Color(0xE6000000),
                        1f to Color.Black
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // 1. Header
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close", onClick = actions::onBack)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM", fontSize = 10.sp, letterSpacing = 1.5.sp, color = Color(0x99FFFFFF))
                    Text(
                        state.source.ifBlank { "Jamendo Stream" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.weight(1f))

            if (state.playbackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC3B1818))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = state.playbackError,
                        color = Color(0xFFFFB4AB),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Info + quick actions
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        state.track.title,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.6).sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        state.track.artist,
                        fontSize = 17.sp,
                        color = Color(0xCCFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val metaLine = listOfNotNull(
                        state.track.album.takeIf { it.isNotBlank() },
                        state.track.attribution.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (metaLine.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            metaLine,
                            fontSize = 12.sp,
                            color = Color(0x99FFFFFF),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                GlassIconButton(
                    icon = if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    description = "Like",
                    tint = if (state.isLiked) Milles.Lime else Color.White,
                    onClick = actions::onLike
                )
                Spacer(Modifier.width(10.dp))
                GlassIconButton(Icons.Rounded.MoreHoriz, "More", onClick = actions::onMore)
            }

            Spacer(Modifier.height(16.dp))

            // 4. Scrubber
            Scrubber(state.positionMs.toFloat() / dur) { actions.onSeek((it * dur).toLong()) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall, color = Color(0xB3FFFFFF))
                if (state.isBuffering) {
                    Text("Buffering…", style = MaterialTheme.typography.labelSmall, color = Milles.Lime)
                }
                Text("-" + formatTime(dur - state.positionMs), style = MaterialTheme.typography.labelSmall, color = Color(0xB3FFFFFF))
            }

            Spacer(Modifier.height(18.dp))

            // 5. Transport
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(actions::onPrevious, Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(40.dp), tint = Color.White)
                }
                Box(contentAlignment = Alignment.Center) {
                    PlayButton(state.isPlaying, 76, actions::onPlayPause, Modifier.testTag("now_playing_play_pause"))
                    if (state.isBuffering) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(82.dp)
                        )
                    }
                }
                IconButton(actions::onNext, Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(40.dp), tint = Color.White)
                }
            }

            Spacer(Modifier.height(26.dp))

            // 6. Utilities
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(Icons.Rounded.Lyrics, "Lyrics") { sheetTab = 1 }
                Row(
                    Modifier
                        .glass(CircleShape)
                        .clickable(onClick = actions::onQualityClick)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("now_playing_quality"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Milles.Lime, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(state.quality, style = MaterialTheme.typography.labelSmall, fontSize = 12.sp, color = Color.White)
                }
                GlassIconButton(Icons.AutoMirrored.Rounded.QueueMusic, "Queue") { sheetTab = 0 }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    sheetTab?.let { QueueSheet(state, actions, it) { sheetTab = null } }
}

@Composable
private fun Scrubber(progress: Float, onSeek: (Float) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val p = (drag ?: progress).coerceIn(0f, 1f)
    val h by animateDpAsState(if (drag != null) 10.dp else 5.dp, label = "scrubber")
    Box(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { drag?.let(onSeek); drag = null },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) }
                )
            }
            .testTag("now_playing_scrubber"),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(h).clip(CircleShape).background(Color(0x33FFFFFF))) {
            Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(Milles.Lime))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun QueueSheet(
    state: PlayerUiState,
    actions: PlayerActions,
    initialTab: Int,
    onDismiss: () -> Unit
) {
    val pager = rememberPagerState(initialPage = initialTab) { 3 }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0C0C0C),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x55FFFFFF)) }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Up Next", "Lyrics", "Related").forEachIndexed { i, t ->
                    val sel = pager.currentPage == i
                    Text(
                        t,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (sel) Color.Black else Color.White,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(if (sel) Milles.Lime else Color(0x1FFFFFFF))
                            .clickable { scope.launch { pager.animateScrollToPage(i) } }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(actions::onShuffle) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.shuffle) Milles.Lime else Milles.TextSecondary
                    )
                }
                IconButton(actions::onRepeat) {
                    Icon(
                        if (state.repeatOne) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (state.repeat) Milles.Lime else Milles.TextSecondary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                when (page) {
                    0 -> TrackList(state.queue, state.track.id, actions::onTrackClick)
                    1 -> LyricsView(state)
                    else -> TrackList(state.related, state.track.id, actions::onTrackClick)
                }
            }
        }
    }
}

@Composable
private fun TrackList(items: List<Track>, activeId: String, onClick: (Track) -> Unit) {
    if (items.isEmpty()) {
        EmptyHint("Nothing here yet")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        itemsIndexed(items, key = { _, t -> t.id }) { _, t ->
            TrackRow(t, t.id == activeId) { onClick(t) }
        }
    }
}

@Composable
private fun LyricsView(state: PlayerUiState) {
    if (state.lyrics.isEmpty()) {
        EmptyHint("Lyrics not available for this song")
        return
    }
    val current = state.lyrics.indexOfLast { it.timeMs <= state.positionMs }.coerceAtLeast(0)
    val list = rememberLazyListState()
    LaunchedEffect(current) { list.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    LazyColumn(
        state = list,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(state.lyrics) { i, line ->
            val active = i == current
            Text(
                line.text,
                fontSize = if (active) 26.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp,
                color = if (active) Color.White else Color(0x59FFFFFF)
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Text(text, color = Milles.TextSecondary, textAlign = TextAlign.Center)
}

package com.milles.music.ui.player

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import com.milles.music.ui.*
import com.milles.music.ui.components.*
import com.milles.music.ui.theme.Milles
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
    fun onTrackClick(track: Track)
}

/**
 * Layout (top -> bottom), every row shares the same 24dp side margins:
 *  1. Header   : close | "PLAYING FROM" + source
 *  2. Cover    : full-screen, slight blur, behind everything
 *  3. Info     : title + artist  |  like + more
 *  4. Scrubber : elapsed ... -remaining
 *  5. Transport: previous, PLAY, next   (shuffle/repeat live in the queue sheet)
 *  6. Utilities: lyrics | audio quality | queue   -> each opens the sheet on its tab
 */
@Composable
fun NowPlayingScreen(state: PlayerUiState, actions: PlayerActions) {
    var sheetTab by remember { mutableStateOf<Int?>(null) }
    val dur = state.track.durationMs.coerceAtLeast(1)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = state.track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().scale(1.06f)
                .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(6.dp, BlurredEdgeTreatment.Unbounded) else Modifier)
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0xA6000000), 0.22f to Color.Transparent,
            0.45f to Color.Transparent, 0.78f to Color(0xE6000000), 1f to Color.Black)))

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            // 1. Header
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close", onClick = actions::onBack)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM", fontSize = 10.sp, letterSpacing = 1.5.sp, color = Color(0x99FFFFFF))
                    Text(state.source.ifBlank { "Your Library" }, style = MaterialTheme.typography.titleMedium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.size(40.dp)) // balances the close button so the title stays centered
            }

            Spacer(Modifier.weight(1f))

            // 3. Info + quick actions
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(state.track.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.track.artist, fontSize = 17.sp, color = Color(0xCCFFFFFF), maxLines = 1)
                }
                Spacer(Modifier.width(12.dp))
                GlassIconButton(
                    if (state.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Like",
                    tint = if (state.isLiked) Milles.Lime else Color.White, onClick = actions::onLike
                )
                Spacer(Modifier.width(10.dp))
                GlassIconButton(Icons.Rounded.MoreHoriz, "More", onClick = actions::onMore)
            }

            Spacer(Modifier.height(16.dp))

            // 4. Scrubber
            Scrubber(state.positionMs.toFloat() / dur) { actions.onSeek((it * dur).toLong()) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelSmall, color = Color(0xB3FFFFFF))
                Text("-" + formatTime(dur - state.positionMs), style = MaterialTheme.typography.labelSmall, color = Color(0xB3FFFFFF))
            }

            Spacer(Modifier.height(18.dp))

            // 5. Transport
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(actions::onPrevious, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(40.dp)) }
                PlayButton(state.isPlaying, 76, actions::onPlayPause)
                IconButton(actions::onNext, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(40.dp)) }
            }

            Spacer(Modifier.height(26.dp))

            // 6. Utilities
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(Icons.Rounded.Lyrics, "Lyrics") { sheetTab = 1 }
                Row(Modifier.glass(CircleShape).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = Milles.Lime, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(state.quality, style = MaterialTheme.typography.labelSmall, fontSize = 12.sp)
                }
                GlassIconButton(Icons.Rounded.QueueMusic, "Queue") { sheetTab = 0 }
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
        Modifier.fillMaxWidth().height(32.dp)
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { drag?.let(onSeek); drag = null },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { change, _ -> drag = (change.position.x / size.width).coerceIn(0f, 1f) }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(h).clip(CircleShape).background(Color(0x33FFFFFF))) {
            Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(Milles.Lime))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun QueueSheet(state: PlayerUiState, actions: PlayerActions, initialTab: Int, onDismiss: () -> Unit) {
    val pager = rememberPagerState(initialPage = initialTab) { 3 }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0C0C0C),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0x55FFFFFF)) }
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Up Next", "Lyrics", "Related").forEachIndexed { i, t ->
                    val sel = pager.currentPage == i
                    Text(
                        t, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = if (sel) Color.Black else Color.White,
                        modifier = Modifier.padding(end = 8.dp).clip(CircleShape)
                            .background(if (sel) Milles.Lime else Color(0x1FFFFFFF))
                            .clickable { scope.launch { pager.animateScrollToPage(i) } }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(actions::onShuffle) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (state.shuffle) Milles.Lime else Milles.TextSecondary) }
                IconButton(actions::onRepeat) { Icon(Icons.Rounded.Repeat, "Repeat", tint = if (state.repeat) Milles.Lime else Milles.TextSecondary) }
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
    if (items.isEmpty()) { EmptyHint("Nothing here yet"); return }
    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        itemsIndexed(items, key = { _, t -> t.id }) { _, t -> TrackRow(t, t.id == activeId) { onClick(t) } }
    }
}

@Composable
private fun LyricsView(state: PlayerUiState) {
    if (state.lyrics.isEmpty()) { EmptyHint("Lyrics not available for this song"); return }
    val current = state.lyrics.indexOfLast { it.timeMs <= state.positionMs }.coerceAtLeast(0)
    val list = rememberLazyListState()
    LaunchedEffect(current) { list.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    LazyColumn(state = list, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        itemsIndexed(state.lyrics) { i, line ->
            val active = i == current
            Text(line.text, fontSize = if (active) 26.sp else 22.sp, fontWeight = FontWeight.Bold,
                lineHeight = 32.sp, color = if (active) Color.White else Color(0x59FFFFFF))
        }
    }
}

@Composable
private fun EmptyHint(text: String) = Box(Modifier.fillMaxSize(), Alignment.Center) {
    Text(text, color = Milles.TextSecondary, textAlign = TextAlign.Center)
}

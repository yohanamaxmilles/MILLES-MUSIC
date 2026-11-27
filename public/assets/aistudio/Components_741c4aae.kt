package com.milles.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.milles.music.ui.Track
import com.milles.music.ui.theme.Milles

/** Frosted-glass look (translucent fill + hairline stroke). */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)) =
    this.clip(shape).background(Color(0x24FFFFFF)).border(1.dp, Milles.Stroke, shape)

@Composable
fun PlayButton(isPlaying: Boolean, size: Int = 56, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Milles.Lime).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null,
            tint = Color.Black, modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        if (onSeeAll != null) TextButton(onClick = onSeeAll) {
            Text("See all", color = Milles.TextSecondary)
            Icon(Icons.Rounded.ChevronRight, null, tint = Milles.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CoverCard(track: Track, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.width(144.dp).clickable(onClick = onClick)) {
        AsyncImage(
            model = track.coverUrl, contentDescription = track.title, contentScale = ContentScale.Crop,
            modifier = Modifier.size(144.dp).clip(RoundedCornerShape(22.dp)).background(Milles.SurfaceHigh)
        )
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Milles.TextSecondary, maxLines = 1)
    }
}

@Composable
fun TrackRow(track: Track, active: Boolean, rank: Int? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) Text("$rank", Modifier.width(30.dp), style = MaterialTheme.typography.titleMedium,
            color = if (rank <= 3) Milles.Lime else Milles.TextSecondary)
        AsyncImage(
            model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Milles.SurfaceHigh)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium,
                color = if (active) Milles.Lime else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = Milles.TextSecondary, maxLines = 1)
        }
        Icon(Icons.Rounded.MoreVert, null, tint = Milles.TextSecondary)
    }
}

/** Floating mini player: sits above the bottom bar, with thin progress line. */
@Composable
fun MiniPlayer(
    track: Track, isPlaying: Boolean, progress: Float, isLiked: Boolean,
    onOpen: () -> Unit, onPlayPause: () -> Unit, onLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.padding(horizontal = 12.dp).fillMaxWidth().height(64.dp)
            .clip(shape).background(Color(0xF0161616)).border(1.dp, Milles.Stroke, shape)
            .clickable(onClick = onOpen)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = track.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = Milles.TextSecondary, maxLines = 1)
            }
            IconButton(onClick = onLike) {
                Icon(if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null,
                    tint = if (isLiked) Milles.Lime else Milles.TextSecondary)
            }
            PlayButton(isPlaying, size = 42, onClick = onPlayPause)
        }
        Box(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 18.dp)
                .fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(Milles.Lime)
        )
    }
}


// ---------- Screen furniture ----------

@Composable
fun ScreenTitle(title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
        trailing?.invoke()
    }
}

/** Round 40dp glass button - used for secondary actions (like, more, add...). */
@Composable
fun GlassIconButton(icon: ImageVector, description: String, tint: Color = Color.White, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).glass(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun <T> FilterChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { item ->
            val sel = item == selected
            Text(
                label(item), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (sel) Color.Black else Color.White,
                modifier = Modifier.clip(CircleShape)
                    .background(if (sel) Milles.Lime else Color(0x1FFFFFFF))
                    .clickable { onSelect(item) }.padding(horizontal = 16.dp, vertical = 9.dp)
            )
        }
    }
}

// ---------- Navigation: 4 destinations, Apple-Music style ----------

enum class MillesTab(val label: String, val icon: ImageVector, val iconSelected: ImageVector) {
    Home("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    Explore("Explore", Icons.Outlined.GridView, Icons.Rounded.GridView),
    Library("Library", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic),
    Search("Search", Icons.Rounded.Search, Icons.Rounded.Search)
}

/** Floating pill with 3 tabs + a separate round Search button. Profile lives behind the avatar on Home. */
@Composable
fun FloatingNav(selected: MillesTab, onSelect: (MillesTab) -> Unit, modifier: Modifier = Modifier) {
    val barColor = Color(0xF2141414)
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).height(64.dp).clip(CircleShape).background(barColor)
                .border(1.dp, Milles.Stroke, CircleShape).padding(6.dp)
        ) {
            listOf(MillesTab.Home, MillesTab.Explore, MillesTab.Library).forEach { tab ->
                NavItem(tab, tab == selected, Modifier.weight(1f)) { onSelect(tab) }
            }
        }
        val searchOn = selected == MillesTab.Search
        val bg by animateColorAsState(if (searchOn) Milles.Lime else barColor, label = "searchBg")
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(bg).border(1.dp, Milles.Stroke, CircleShape)
                .clickable { onSelect(MillesTab.Search) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Rounded.Search, "Search", tint = if (searchOn) Color.Black else Color.White, modifier = Modifier.size(26.dp)) }
    }
}

@Composable
private fun NavItem(tab: MillesTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Color(0x29C6F24E) else Color.Transparent, label = "bg")
    val tint by animateColorAsState(if (selected) Milles.Lime else Milles.TextSecondary, label = "tint")
    Column(
        modifier.fillMaxHeight().clip(CircleShape).background(bg).clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(if (selected) tab.iconSelected else tab.icon, tab.label, tint = tint, modifier = Modifier.size(24.dp))
        Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

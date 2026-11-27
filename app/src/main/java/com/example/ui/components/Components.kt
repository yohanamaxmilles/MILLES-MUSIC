package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.Track
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

/** Frosted-glass look (translucent fill + hairline stroke). */
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)) =
    this.clip(shape).background(Color(0x24FFFFFF)).border(1.dp, Milles.Stroke, shape)

@Composable
fun PlayButton(
    isPlaying: Boolean,
    size: Int = 56,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Milles.Lime)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    val palette = LocalMillesPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", color = palette.textSecondary)
                Icon(Icons.Rounded.ChevronRight, contentDescription = "See all $title", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun CoverCard(track: Track, subtitle: String, onClick: () -> Unit) {
    val palette = LocalMillesPalette.current
    Column(
        Modifier
            .width(144.dp)
            .clickable(onClick = onClick)
            .testTag("cover_card_${track.id}")
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(palette.surfaceHigh)
        )
        Spacer(Modifier.height(8.dp))
        Text(track.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TrackRow(
    track: Track,
    active: Boolean,
    rank: Int? = null,
    onMore: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val palette = LocalMillesPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                "$rank",
                Modifier.width(30.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (rank <= 3) Milles.Lime else palette.textSecondary
            )
        }
        AsyncImage(
            model = track.coverUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceHigh)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (active) Milles.Lime else palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onMore != null) {
            IconButton(onClick = onMore) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "More options for ${track.title}", tint = palette.textSecondary)
            }
        } else {
            Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = palette.textSecondary)
        }
    }
}

/** Floating mini player: sits above the bottom bar, with thin progress line. */
@Composable
fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    progress: Float,
    isLiked: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onLike: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalMillesPalette.current
    val shape = RoundedCornerShape(22.dp)
    val bgColor = if (palette.isDark) Color(0xF0161616) else Color(0xF5FFFFFF)
    Box(
        modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, palette.stroke, shape)
            .clickable(onClick = onOpen)
            .testTag("mini_player")
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onLike, modifier = Modifier.testTag("mini_player_like")) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Milles.Lime else palette.textSecondary
                )
            }
            PlayButton(isPlaying, size = 42, onClick = onPlayPause, modifier = Modifier.testTag("mini_player_play_pause"))
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(Milles.Lime)
        )
    }
}

// ---------- Screen furniture ----------

@Composable
fun ScreenTitle(title: String, trailing: (@Composable () -> Unit)? = null) {
    val palette = LocalMillesPalette.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.8).sp,
            color = palette.textPrimary
        )
        trailing?.invoke()
    }
}

/** Round 40dp glass button - used for secondary actions (like, more, add...). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(48.dp)
            .padding(4.dp)
            .glass(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun <T> FilterChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val palette = LocalMillesPalette.current
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { item ->
            val sel = item == selected
            val unselectedBg = if (palette.isDark) Color(0x1FFFFFFF) else palette.surfaceHigh
            Text(
                label(item),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (sel) Color.Black else palette.textPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (sel) Milles.Lime else unselectedBg)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
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
    val palette = LocalMillesPalette.current
    val barColor = if (palette.isDark) Color(0xF2141414) else Color(0xF5FFFFFF)
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(64.dp)
                .clip(CircleShape)
                .background(barColor)
                .border(1.dp, palette.stroke, CircleShape)
                .padding(6.dp)
        ) {
            listOf(MillesTab.Home, MillesTab.Explore, MillesTab.Library).forEach { tab ->
                NavItem(tab, tab == selected, Modifier.weight(1f)) { onSelect(tab) }
            }
        }
        val searchOn = selected == MillesTab.Search
        val bg by animateColorAsState(if (searchOn) Milles.Lime else barColor, label = "searchBg")
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bg)
                .border(1.dp, palette.stroke, CircleShape)
                .clickable { onSelect(MillesTab.Search) }
                .testTag("nav_tab_search"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Search,
                contentDescription = "Search",
                tint = if (searchOn) Color.Black else palette.textPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun NavItem(tab: MillesTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalMillesPalette.current
    val tint by animateColorAsState(if (selected) Milles.Lime else palette.textSecondary, label = "tint")
    Column(
        modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("nav_tab_${tab.name.lowercase()}"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (selected) tab.iconSelected else tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

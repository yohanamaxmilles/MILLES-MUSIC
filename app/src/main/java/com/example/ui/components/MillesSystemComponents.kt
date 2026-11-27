package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.MainTab
import com.example.model.Track
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme
import kotlin.math.roundToInt

/**
 * Custom MILLES MUSIC container card — avoids default Material Card elevation & styling.
 */
@Composable
fun MillesSurfaceCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MillesTheme.colors.surface,
    borderColor: Color = MillesTheme.colors.divider.copy(alpha = 0.65f),
    cornerRadius: Dp = MillesRadius.Large,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(width = 1.dp, color = borderColor, shape = shape)
    val finalModifier = if (onClick != null) {
        baseModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = MillesTheme.colors.lime.copy(alpha = 0.25f)),
            onClick = onClick
        )
    } else {
        baseModifier
    }
    Box(modifier = finalModifier) {
        content()
    }
}

/**
 * Custom pill-shaped filter chip with Lime Green active state.
 */
@Composable
fun MillesPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val bg by animateColorAsState(
        targetValue = if (selected) colors.lime else colors.surface,
        animationSpec = tween(200),
        label = "pill_bg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.onLime else colors.textSecondary,
        animationSpec = tween(200),
        label = "pill_fg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) colors.lime else colors.divider,
        animationSpec = tween(200),
        label = "pill_border"
    )

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(MillesRadius.Pill))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(MillesRadius.Pill))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = fg,
            maxLines = 1
        )
    }
}

/**
 * Custom circular icon button with >= 48.dp touch target and bespoke surface styling.
 */
@Composable
fun MillesCircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MillesTheme.colors.surface,
    contentColor: Color = MillesTheme.colors.textPrimary,
    borderColor: Color = MillesTheme.colors.divider.copy(alpha = 0.7f),
    size: Dp = 48.dp,
    iconSize: Dp = 22.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = MillesTheme.colors.lime),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Custom header bar for secondary screens (Search, Theme, Collection Detail).
 */
@Composable
fun MillesTopDetailBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = MillesTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MillesCircleIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back),
            onClick = onBack,
            containerColor = colors.surface,
            size = 48.dp,
            modifier = Modifier.testTag("back_button")
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            trailing()
        }
    }
}

/**
 * Bespoke capsule search input built on BasicTextField.
 */
@Composable
fun MillesSearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(MillesRadius.Pill))
            .background(colors.surface)
            .border(1.dp, colors.divider, RoundedCornerShape(MillesRadius.Pill))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.lime),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { onQueryChange("") }
                    )
                    .testTag("clear_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.cd_clear_search),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Bespoke toggle switch matching MILLES MUSIC Theme screen (Lime Green track when active).
 */
@Composable
fun MillesToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.lime else colors.surfaceElevated,
        animationSpec = tween(220),
        label = "switch_track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(stiffness = 500f),
        label = "switch_thumb"
    )
    val thumbColor = if (checked) colors.onLime else colors.textPrimary

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(RoundedCornerShape(MillesRadius.Pill))
            .background(trackColor)
            .border(
                width = 1.dp,
                color = if (checked) colors.lime else colors.divider,
                shape = RoundedCornerShape(MillesRadius.Pill)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}

/**
 * Custom interactive playback progress scrubber with Lime Green active track & glowing thumb.
 */
@Composable
fun MillesScrubberSlider(
    progressSeconds: Int,
    durationSeconds: Int,
    onSeek: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    var widthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationSeconds.coerceAtLeast(1)
    val actualFraction = (progressSeconds.toFloat() / safeDuration).coerceIn(0f, 1f)
    val displayedFraction by animateFloatAsState(
        targetValue = if (isDragging) dragFraction else actualFraction,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 180),
        label = "scrubber_fraction"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    onSeek((fraction * safeDuration).roundToInt())
                }
            }
            .pointerInput(safeDuration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((dragFraction * safeDuration).roundToInt())
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            }
            .testTag("now_playing_scrubber"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val trackY = size.height / 2f
            val activeX = size.width * displayedFraction
            val trackStroke = 4.dp.toPx()

            // Inactive track
            drawLine(
                color = colors.divider.copy(alpha = 0.85f),
                start = Offset(0f, trackY),
                end = Offset(size.width, trackY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round
            )

            // Active Lime Green track
            drawLine(
                color = colors.lime,
                start = Offset(0f, trackY),
                end = Offset(activeX, trackY),
                strokeWidth = trackStroke,
                cap = StrokeCap.Round
            )

            // Soft outer halo on thumb
            drawCircle(
                color = colors.lime.copy(alpha = if (isDragging) 0.35f else 0.22f),
                radius = if (isDragging) 13.dp.toPx() else 10.dp.toPx(),
                center = Offset(activeX, trackY)
            )

            // Crisp Lime Green thumb
            drawCircle(
                color = colors.lime,
                radius = if (isDragging) 7.5.dp.toPx() else 6.dp.toPx(),
                center = Offset(activeX, trackY)
            )
        }
    }
}

/**
 * Floating editorial Mini Player docked neatly above the custom bottom navigation bar.
 */
@Composable
fun MillesMiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progressSeconds: Int,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    val progress = (progressSeconds.toFloat() / track.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(MillesRadius.Large))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.divider.copy(alpha = 0.8f), RoundedCornerShape(MillesRadius.Large))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.lime),
                onClick = onOpenNowPlaying
            )
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MillesAlbumArtwork(
                artwork = track.artwork,
                contentDescription = track.title,
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isPlaying) {
                        MillesEqualizerBars(
                            isPlaying = true,
                            barCount = 3,
                            barWidth = 2.5.dp,
                            maxHeight = 10.dp
                        )
                    }
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
                    .testTag("mini_player_favorite"),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.lime)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = colors.onLime),
                        onClick = onTogglePlayPause
                    )
                    .testTag("mini_player_play_pause"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                    tint = colors.onLime,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        // Edge-to-edge hairline Lime Green playback progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.divider.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(colors.lime)
            )
        }
    }
}

private data class NavTabSpec(
    val tab: MainTab,
    val labelRes: Int,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector,
    val testTag: String
)

private val MillesNavTabs = listOf(
    NavTabSpec(MainTab.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_home"),
    NavTabSpec(MainTab.EXPLORE, R.string.nav_explore, Icons.Rounded.Explore, Icons.Outlined.Explore, "nav_tab_explore"),
    NavTabSpec(MainTab.LIBRARY, R.string.nav_library, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_tab_library"),
    NavTabSpec(MainTab.FAVORITES, R.string.nav_favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_tab_favorites"),
    NavTabSpec(MainTab.PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
)

/**
 * Custom MILLES MUSIC bottom navigation bar (replaces default Material 3 NavigationBar).
 */
@Composable
fun MillesBottomNavBar(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.backgroundSecondary)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MillesNavTabs.forEach { spec ->
                val selected = spec.tab == activeTab
                val itemColor by animateColorAsState(
                    targetValue = if (selected) colors.lime else colors.textSecondary,
                    animationSpec = tween(200),
                    label = "nav_color_${spec.tab.name}"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.lime.copy(alpha = 0.3f)),
                            role = Role.Tab,
                            onClick = { onSelectTab(spec.tab) }
                        )
                        .testTag(spec.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Claude design: selected state uses Lime icon + label only.
                    Icon(
                        imageVector = if (selected) spec.activeIcon else spec.inactiveIcon,
                        contentDescription = stringResource(spec.labelRes),
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(spec.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = itemColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Adaptive Navigation Rail for tablet/foldable Expanded screens.
 */
@Composable
fun MillesNavigationRail(
    activeTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MillesTheme.colors
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(88.dp)
                .fillMaxHeight()
                .background(colors.backgroundSecondary)
                .padding(vertical = 24.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MillesWaveLogoM(modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(16.dp))
            MillesNavTabs.forEach { spec ->
                val selected = spec.tab == activeTab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) colors.lime.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.lime),
                            onClick = { onSelectTab(spec.tab) }
                        )
                        .testTag("${spec.testTag}_rail"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) spec.activeIcon else spec.inactiveIcon,
                        contentDescription = stringResource(spec.labelRes),
                        tint = if (selected) colors.lime else colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(spec.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) colors.lime else colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(colors.divider)
        )
    }
}

package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AudioQuality
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.state.formatDuration
import com.example.ui.components.MillesAlbumArtwork
import com.example.ui.components.MillesEqualizerBars
import com.example.ui.components.MillesScrubberSlider
import com.example.ui.theme.DarkBackgroundMain
import com.example.ui.theme.DarkTextPrimary
import com.example.ui.theme.DarkTextSecondary
import com.example.ui.theme.LimePrimary
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.OnLimeInk
import com.example.ui.theme.TabularTimeStyle

@Composable
fun NowPlayingScreen(
    track: Track,
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
    onOpenQueue: () -> Unit,
    onOpenAudioQuality: () -> Unit,
    onOpenTrackOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onMinimize)

    // Immersive full-bleed stage preserves original artwork hues with seamless top/bottom scrims
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackgroundMain)
            .testTag("now_playing_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Full-bleed atmospheric artwork backdrop
        MillesAlbumArtwork(
            artwork = track.artwork,
            contentDescription = track.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(540.dp)
                .align(Alignment.TopCenter),
            shape = RoundedCornerShape(0.dp)
        )

        // Editorial top & bottom scrims for crisp legibility without washing out original artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xAA151611),
                            0.20f to Color(0x1A151611),
                            0.46f to Color(0x33151611),
                            0.65f to Color(0xEB151611),
                            0.82f to DarkBackgroundMain,
                            1.0f to DarkBackgroundMain
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Minimize, Now Playing title, Overflow options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x44151611))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = LimePrimary),
                            onClick = onMinimize
                        )
                        .testTag("now_playing_minimize"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_minimize_player),
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.title_now_playing),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = DarkTextPrimary
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x44151611))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = LimePrimary),
                            onClick = onOpenTrackOptions
                        )
                        .testTag("now_playing_more"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = DarkTextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom editorial player console
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Track Title, Artist, and Favorite Heart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = DarkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onToggleFavorite
                            )
                            .testTag("now_playing_favorite"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.cd_favorite),
                            tint = if (isFavorite) LimePrimary else DarkTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Scrubber + Elapsed / Duration timestamps
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MillesScrubberSlider(
                        progressSeconds = progressSeconds,
                        durationSeconds = track.durationSeconds,
                        onSeek = onSeek
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(progressSeconds),
                            style = TabularTimeStyle,
                            color = DarkTextSecondary
                        )
                        Text(
                            text = formatDuration(track.durationSeconds),
                            style = TabularTimeStyle,
                            color = DarkTextSecondary
                        )
                    }
                }

                // Transport Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onToggleShuffle
                            )
                            .testTag("now_playing_shuffle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = stringResource(R.string.cd_shuffle),
                            tint = if (isShuffleEnabled) LimePrimary else DarkTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onPrevious
                            )
                            .testTag("now_playing_previous"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous),
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Prominent Lime Green Play / Pause Hero Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(LimePrimary)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = OnLimeInk),
                                onClick = onTogglePlayPause
                            )
                            .testTag("now_playing_play_pause"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.cd_pause else R.string.cd_play),
                            tint = OnLimeInk,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onNext
                            )
                            .testTag("now_playing_next"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.cd_next),
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onCycleRepeat
                            )
                            .testTag("now_playing_repeat"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = stringResource(R.string.cd_repeat),
                            tint = if (repeatMode != RepeatMode.OFF) LimePrimary else DarkTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Footer: Audio Quality Selector + Queue Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(MillesRadius.Medium))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onOpenAudioQuality
                            )
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .testTag("now_playing_audio_quality"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MillesEqualizerBars(
                            isPlaying = isPlaying,
                            barColor = LimePrimary,
                            barCount = 4,
                            barWidth = 3.dp,
                            maxHeight = 16.dp
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.audio_quality_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkTextSecondary
                            )
                            Text(
                                text = audioQuality.shortLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = DarkTextPrimary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x44FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = LimePrimary),
                                onClick = onOpenQueue
                            )
                            .testTag("now_playing_queue_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = stringResource(R.string.cd_queue),
                            tint = DarkTextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

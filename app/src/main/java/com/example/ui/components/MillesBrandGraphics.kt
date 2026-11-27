package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.ArtworkDescriptor
import com.example.ui.theme.LavenderDarkAccent
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LimeDarkAccent
import com.example.ui.theme.LimePrimary
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

/**
 * Bespoke sculpted ribbon "M" brand mark of MILLES MUSIC:
 * Left arch in Lime Green (#C6ED65) interlocking with right arch in Lavender (#B9A5F2).
 */
@Composable
fun MillesWaveLogoM(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left Lime Green ribbon arch
        val leftPath = Path().apply {
            moveTo(w * 0.12f, h * 0.86f)
            cubicTo(
                w * 0.10f, h * 0.45f,
                w * 0.18f, h * 0.14f,
                w * 0.34f, h * 0.15f
            )
            cubicTo(
                w * 0.45f, h * 0.16f,
                w * 0.49f, h * 0.44f,
                w * 0.51f, h * 0.58f
            )
            cubicTo(
                w * 0.53f, h * 0.68f,
                w * 0.44f, h * 0.72f,
                w * 0.39f, h * 0.57f
            )
            cubicTo(
                w * 0.36f, h * 0.45f,
                w * 0.34f, h * 0.38f,
                w * 0.31f, h * 0.40f
            )
            cubicTo(
                w * 0.27f, h * 0.43f,
                w * 0.26f, h * 0.65f,
                w * 0.26f, h * 0.86f
            )
            cubicTo(
                w * 0.24f, h * 0.92f,
                w * 0.14f, h * 0.92f,
                w * 0.12f, h * 0.86f
            )
            close()
        }

        // Right Lavender ribbon arch
        val rightPath = Path().apply {
            moveTo(w * 0.47f, h * 0.56f)
            cubicTo(
                w * 0.52f, h * 0.32f,
                w * 0.59f, h * 0.14f,
                w * 0.70f, h * 0.15f
            )
            cubicTo(
                w * 0.84f, h * 0.16f,
                w * 0.90f, h * 0.48f,
                w * 0.88f, h * 0.86f
            )
            cubicTo(
                w * 0.86f, h * 0.92f,
                w * 0.75f, h * 0.92f,
                w * 0.74f, h * 0.85f
            )
            cubicTo(
                w * 0.74f, h * 0.63f,
                w * 0.72f, h * 0.42f,
                w * 0.67f, h * 0.40f
            )
            cubicTo(
                w * 0.63f, h * 0.39f,
                w * 0.60f, h * 0.54f,
                w * 0.56f, h * 0.66f
            )
            cubicTo(
                w * 0.52f, h * 0.74f,
                w * 0.45f, h * 0.68f,
                w * 0.47f, h * 0.56f
            )
            close()
        }

        drawPath(
            path = leftPath,
            brush = Brush.linearGradient(
                colors = listOf(LimePrimary, LimeDarkAccent),
                start = Offset(0f, 0f),
                end = Offset(w * 0.55f, h)
            )
        )
        drawPath(
            path = rightPath,
            brush = Brush.linearGradient(
                colors = listOf(LavenderPrimary, LavenderDarkAccent),
                start = Offset(w * 0.45f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

/**
 * Subtle translucent Lavender (top) & Lime Green (bottom) silk wave ribbons
 * used exclusively on the Splash screen and Theme preview footer.
 */
@Composable
fun MillesSilkWavesBackdrop(
    modifier: Modifier = Modifier,
    showTopWave: Boolean = true,
    showBottomWave: Boolean = true,
    intensity: Float = 1f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (showTopWave) {
            val topRibbon = Path().apply {
                moveTo(-w * 0.1f, h * 0.18f)
                cubicTo(w * 0.25f, h * 0.04f, w * 0.60f, h * 0.28f, w * 1.15f, h * 0.08f)
                lineTo(w * 1.15f, -h * 0.05f)
                lineTo(-w * 0.1f, -h * 0.05f)
                close()
            }
            drawPath(
                path = topRibbon,
                brush = Brush.radialGradient(
                    colors = listOf(
                        LavenderPrimary.copy(alpha = 0.26f * intensity),
                        LavenderDarkAccent.copy(alpha = 0.10f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.68f, h * 0.10f),
                    radius = w * 0.9f
                )
            )
            drawPath(
                path = Path().apply {
                    moveTo(-w * 0.05f, h * 0.23f)
                    cubicTo(w * 0.28f, h * 0.10f, w * 0.62f, h * 0.30f, w * 1.08f, h * 0.14f)
                },
                color = LavenderPrimary.copy(alpha = 0.22f * intensity),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        if (showBottomWave) {
            val bottomRibbon = Path().apply {
                moveTo(-w * 0.1f, h * 0.84f)
                cubicTo(w * 0.28f, h * 0.72f, w * 0.65f, h * 0.96f, w * 1.12f, h * 0.76f)
                lineTo(w * 1.12f, h * 1.08f)
                lineTo(-w * 0.1f, h * 1.08f)
                close()
            }
            drawPath(
                path = bottomRibbon,
                brush = Brush.radialGradient(
                    colors = listOf(
                        LimePrimary.copy(alpha = 0.32f * intensity),
                        LimeDarkAccent.copy(alpha = 0.12f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.35f, h * 0.88f),
                    radius = w * 0.95f
                )
            )
            drawPath(
                path = Path().apply {
                    moveTo(-w * 0.05f, h * 0.80f)
                    cubicTo(w * 0.32f, h * 0.68f, w * 0.68f, h * 0.92f, w * 1.08f, h * 0.75f)
                },
                color = LimePrimary.copy(alpha = 0.26f * intensity),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Renders album/playlist/artist artwork preserving original photography colors,
 * or renders crisp editorial geometric artwork when no photo drawable is attached.
 */
@Composable
fun MillesAlbumArtwork(
    artwork: ArtworkDescriptor,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(MillesRadius.Medium)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(artwork.secondaryColor),
        contentAlignment = Alignment.Center
    ) {
        if (artwork.imageRes != null) {
            Image(
                painter = painterResource(id = artwork.imageRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(artwork.primaryColor, artwork.secondaryColor),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )

                when (artwork.patternVariant % 4) {
                    0 -> {
                        // Concentric vinyl resonance rings
                        drawCircle(
                            color = artwork.accentColor.copy(alpha = 0.22f),
                            radius = w * 0.42f,
                            center = Offset(w * 0.58f, h * 0.45f)
                        )
                        drawCircle(
                            color = artwork.accentColor.copy(alpha = 0.45f),
                            radius = w * 0.24f,
                            center = Offset(w * 0.58f, h * 0.45f),
                            style = Stroke(width = w * 0.03f)
                        )
                    }
                    1 -> {
                        // Diagonal editorial acoustic prism
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(artwork.accentColor.copy(alpha = 0.55f), Color.Transparent),
                                center = Offset(w * 0.28f, h * 0.28f),
                                radius = w * 0.6f
                            ),
                            radius = w * 0.6f,
                            center = Offset(w * 0.28f, h * 0.28f)
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.16f),
                            start = Offset(0f, h * 0.72f),
                            end = Offset(w, h * 0.25f),
                            strokeWidth = w * 0.025f
                        )
                    }
                    2 -> {
                        // Layered acoustic bars
                        for (i in 0..4) {
                            val x = w * (0.2f + i * 0.15f)
                            val barH = h * (0.26f + ((i * 37) % 40) / 100f)
                            drawRoundRect(
                                color = artwork.accentColor.copy(alpha = 0.28f + i * 0.08f),
                                topLeft = Offset(x, (h - barH) / 2f),
                                size = Size(w * 0.07f, barH),
                                cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
                            )
                        }
                    }
                    else -> {
                        // Soft orbital horizon
                        drawCircle(
                            color = artwork.accentColor.copy(alpha = 0.35f),
                            radius = w * 0.35f,
                            center = Offset(w * 0.5f, h * 0.78f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            radius = w * 0.48f,
                            center = Offset(w * 0.5f, h * 0.78f),
                            style = Stroke(width = w * 0.015f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Signature ascending equalizer bars (static for Journey card or animated for active playback).
 */
@Composable
fun MillesEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MillesTheme.colors.lime,
    barCount: Int = 4,
    barWidth: Dp = 4.dp,
    maxHeight: Dp = 20.dp
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val animValues = List(barCount) { index ->
        val animated by transition.animateFloat(
            initialValue = 0.28f,
            targetValue = 0.98f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 420 + index * 130,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
        if (isPlaying) animated else (0.35f + index * 0.16f).coerceAtMost(0.95f)
    }

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        animValues.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight * fraction)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

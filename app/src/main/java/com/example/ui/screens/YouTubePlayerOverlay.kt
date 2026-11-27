package com.example.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.Track
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles

/**
 * Dedicated official YouTube Embedded Player overlay.
 *
 * Completely separate from Media3 audio playback:
 * - Renders official YouTube IFrame embed (`https://www.youtube.com/embed/{videoId}`).
 * - Never extracts audio streams, never downloads media, and never plays in background.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerOverlay(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)
    val palette = LocalMillesPalette.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val videoId = track.providerTrackId.removePrefix("yt_")
    val embedUrl = "https://www.youtube.com/embed/$videoId?playsinline=1&rel=0"
    val watchUrl = track.originalProviderUrl ?: "https://www.youtube.com/watch?v=$videoId"

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(videoId) {
        onDispose {
            webViewRef?.let { webView ->
                runCatching {
                    webView.onPause()
                    webView.loadUrl("about:blank")
                    webView.destroy()
                }
            }
            webViewRef = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("youtube_player_overlay"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x26FF4D4F))
                    .border(1.dp, Color(0x66FF4D4F), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SmartDisplay,
                    contentDescription = "YouTube",
                    tint = Color(0xFFFF5A5F),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "YouTube Official Player",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
            }

            GlassIconButton(
                icon = Icons.Rounded.Close,
                description = "Close YouTube player",
                tint = palette.textPrimary,
                onClick = onDismiss
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black)
                .border(1.dp, palette.stroke, RoundedCornerShape(22.dp))
                .testTag("youtube_webview_container")
        ) {
            AndroidView(
                factory = {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val target = request?.url?.toString().orEmpty()
                                val host = request?.url?.host?.lowercase().orEmpty()
                                val allowedHost = host.endsWith("youtube.com") ||
                                    host.endsWith("youtube-nocookie.com") ||
                                    host.endsWith("youtu.be")
                                return if (allowedHost) {
                                    false
                                } else {
                                    if (target.startsWith("http")) {
                                        runCatching { uriHandler.openUri(target) }
                                    }
                                    true
                                }
                            }
                        }
                        loadUrl(embedUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    if (view.url != embedUrl) {
                        view.loadUrl(embedUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Channel: ${track.artist}",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textSecondary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(palette.surface)
                .border(1.dp, palette.stroke, RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Provider & Compliance",
                style = MaterialTheme.typography.labelSmall,
                color = Milles.Lime
            )
            Text(
                text = track.licenseType,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary
            )
            Text(
                text = "Played strictly inside the official YouTube embedded player. Background audio playback and stream extraction are disabled.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
            track.territoryRestrictions?.let { restriction ->
                Text(
                    text = restriction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Milles.Violet
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(CircleShape)
                .background(Milles.Lime)
                .clickable { runCatching { uriHandler.openUri(watchUrl) } }
                .testTag("youtube_open_original"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Watch on YouTube",
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }
    }
}

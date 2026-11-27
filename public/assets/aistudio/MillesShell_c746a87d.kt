package com.milles.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.milles.music.ui.components.FloatingNav
import com.milles.music.ui.components.MillesTab
import com.milles.music.ui.theme.Milles

/**
 * Hosts every main screen. Content scrolls UNDER the floating mini player + nav,
 * which sit on a soft fade. Call enableEdgeToEdge() in MainActivity.
 *
 * MillesShell(tab, { tab = it }, miniPlayer = { MiniPlayer(...) }) { inset ->
 *     when (tab) { Home -> HomeScreen(..., bottomInset = inset) ... }
 * }
 */
@Composable
fun MillesShell(
    tab: MillesTab,
    onTab: (MillesTab) -> Unit,
    miniPlayer: (@Composable () -> Unit)? = null,
    content: @Composable (bottomInset: Dp) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Milles.Black)) {
        content(if (miniPlayer != null) 184.dp else 120.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to Color(0xE6050505), 1f to Color(0xFF050505)))
                .padding(top = 28.dp)
        ) {
            miniPlayer?.let { it(); Spacer(Modifier.height(10.dp)) }
            FloatingNav(tab, onTab)
        }
    }
}

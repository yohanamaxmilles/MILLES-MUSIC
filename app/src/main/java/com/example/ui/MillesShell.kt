package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.FloatingNav
import com.example.ui.components.MillesTab
import com.example.ui.theme.LocalMillesPalette

/**
 * Hosts every main screen. Content scrolls UNDER the floating mini player + nav,
 * which sit on a soft fade. Call enableEdgeToEdge() in MainActivity.
 */
@Composable
fun MillesShell(
    tab: MillesTab,
    onTab: (MillesTab) -> Unit,
    miniPlayer: (@Composable () -> Unit)? = null,
    content: @Composable (bottomInset: Dp) -> Unit
) {
    val palette = LocalMillesPalette.current
    val fadeMid = palette.background.copy(alpha = 0.90f)
    val fadeEnd = palette.background
    Box(Modifier.fillMaxSize().background(palette.background)) {
        content(if (miniPlayer != null) 184.dp else 120.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to fadeMid, 1f to fadeEnd))
                .padding(top = 28.dp)
        ) {
            miniPlayer?.let {
                it()
                Spacer(Modifier.height(10.dp))
            }
            FloatingNav(tab, onTab)
        }
    }
}

package com.milles.music.ui

/** UI models. Map your existing data classes / ViewModel state to these. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: Any?,          // URL, Uri or drawable resource id
    val durationMs: Long
)

data class LyricLine(val timeMs: Long, val text: String)

data class PlayerUiState(
    val track: Track,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val isLiked: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val quality: String = "High",
    val source: String = "",                    // e.g. "Afrobeats Top Hits"
    val queue: List<Track> = emptyList(),
    val related: List<Track> = emptyList(),
    val lyrics: List<LyricLine> = emptyList()   // empty = "Lyrics not available"
)

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/** Playlist / album / artist / genre tile. */
data class Collection(val id: String, val title: String, val subtitle: String = "", val coverUrl: Any? = null)

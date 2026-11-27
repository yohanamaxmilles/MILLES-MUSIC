package com.example.ui

import com.example.model.ProviderId

/** UI models mapped from MillesMusicViewModel and live provider responses. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val coverUrl: Any?,
    val durationMs: Long,
    val album: String = "",
    val attribution: String = "",
    val licenseUrl: String? = null,
    val providerId: ProviderId = ProviderId.JAMENDO,
    val providerLabel: String = providerId.displayName,
    val isVideoEmbedOnly: Boolean = false
)

data class LyricLine(val timeMs: Long, val text: String)

data class PlayerUiState(
    val track: Track,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackError: String? = null,
    val positionMs: Long = 0,
    val isLiked: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val repeatOne: Boolean = false,
    val quality: String = "MP3 320",
    val source: String = "",
    val queue: List<Track> = emptyList(),
    val related: List<Track> = emptyList(),
    val lyrics: List<LyricLine> = emptyList()
)

fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(s / 60, s % 60)
}

/** Playlist / album / artist / genre tile. */
data class Collection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val coverUrl: Any? = null,
    val providerId: ProviderId = ProviderId.JAMENDO
)

package com.example.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Converts a resolved, authorised stream into a Media3 item.
 * Source adapters must resolve the stream URL before calling this factory.
 * Never pass a YouTube page URL or an artwork URL as an audio stream.
 */
data class ResolvedMusicTrack(
    val id: String,
    val title: String,
    val artist: String,
    val streamUri: Uri,
    val artworkUri: Uri? = null,
    val mimeType: String? = null
)

object MillesMediaItemFactory {
    fun from(track: ResolvedMusicTrack): MediaItem {
        require(track.id.isNotBlank()) { "Track ID must not be blank" }
        require(track.title.isNotBlank()) { "Track title must not be blank" }
        require(track.streamUri.scheme == "https" || track.streamUri.scheme == "http") {
            "Stream URI must use HTTP or HTTPS"
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setArtworkUri(track.artworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUri)
            .setMimeType(track.mimeType)
            .setMediaMetadata(metadata)
            .build()
    }
}

package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ProviderId(val id: String, val displayName: String) {
    JAMENDO("jamendo", "Jamendo"),
    AUDIUS("audius", "Audius"),
    YOUTUBE("youtube", "YouTube"),
    SOUNDCLOUD("soundcloud", "SoundCloud"),
    SPOTIFY("spotify", "Spotify")
}

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    AUTO("Auto")
}

enum class AudioQuality(val title: String, val subtitle: String, val shortLabel: String) {
    STANDARD("Standard", "AAC · 160 kbps", "Standard"),
    HIGH("High", "Lossless · 24-bit / 48 kHz", "High"),
    STUDIO("Studio Master", "Hi-Res · 24-bit / 192 kHz", "Master")
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

@Immutable
data class ArtworkDescriptor(
    @DrawableRes val imageRes: Int? = null,
    val primaryColor: Color = Color(0xFF2B2D24),
    val secondaryColor: Color = Color(0xFF171813),
    val accentColor: Color = Color(0xFFC6F24E),
    val patternVariant: Int = 0
)

@Immutable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSeconds: Int,
    val artwork: ArtworkDescriptor,
    val coverUrl: String? = null,
    val streamUrl: String? = null,
    val artistId: String? = null,
    val albumId: String? = null,
    val releaseDate: String? = null,
    val licenseUrl: String? = null,
    val shareUrl: String? = null,
    val attributionLabel: String = "Jamendo Streaming",
    val isCommercialLicensed: Boolean = false,
    val isTrending: Boolean = false,
    val isNewRelease: Boolean = false,
    val isUpbeat: Boolean = false,
    val providerId: ProviderId = ProviderId.JAMENDO,
    val providerTrackId: String = id,
    val originalProviderUrl: String? = shareUrl,
    val licenseType: String = attributionLabel,
    val territoryRestrictions: String? = null,
    val streamingPermitted: Boolean = !streamUrl.isNullOrBlank(),
    val isVideoEmbedOnly: Boolean = false
)

@Immutable
data class Artist(
    val id: String,
    val name: String,
    val genre: String,
    val subtitle: String,
    val artwork: ArtworkDescriptor,
    val imageUrl: String? = null,
    val joinDate: String? = null,
    val website: String? = null,
    val shareUrl: String? = null,
    val providerId: ProviderId = ProviderId.JAMENDO,
    val originalProviderUrl: String? = shareUrl
)

@Immutable
data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val releaseDate: String? = null,
    val genre: String,
    val trackIds: List<String> = emptyList(),
    val artwork: ArtworkDescriptor,
    val coverUrl: String? = null,
    val shareUrl: String? = null,
    val providerId: ProviderId = ProviderId.JAMENDO,
    val originalProviderUrl: String? = shareUrl
)

@Immutable
data class Playlist(
    val id: String,
    val title: String,
    val subtitle: String,
    val trackIds: List<String>,
    val artwork: ArtworkDescriptor,
    val displayedSongCount: Int = trackIds.size,
    val isUserCreated: Boolean = false,
    val providerId: ProviderId = ProviderId.JAMENDO,
    val coverUrl: String? = null,
    val originalProviderUrl: String? = null
)

@Immutable
data class GenreCategory(
    val id: String,
    val name: String,
    val subtitle: String,
    val artwork: ArtworkDescriptor,
    val trackIds: List<String>
)

sealed interface SecondaryDestination {
    data object Profile : SecondaryDestination
    data object ThemeSettings : SecondaryDestination
    data class PlaylistDetail(val playlistId: String) : SecondaryDestination
    data class GenreDetail(val genreId: String) : SecondaryDestination
    data class ArtistDetail(val artistId: String) : SecondaryDestination
    data class AlbumDetail(val albumId: String) : SecondaryDestination
    data class CollectionOverview(val title: String, val trackIds: List<String>) : SecondaryDestination
}

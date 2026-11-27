package com.example.provider

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.Playlist
import com.example.model.ProviderId
import com.example.model.Track
import com.example.model.TrackSource
import com.example.model.TrendMetrics

/**
 * Provider-independent track representation normalizing metadata, licensing,
 * attribution, and playback authorization across external providers.
 */
data class UnifiedTrack(
    val trackId: String,
    val providerId: ProviderId,
    val providerTrackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String = providerId.displayName,
    val durationSeconds: Int,
    val artworkUrl: String? = null,
    val streamUrl: String? = null,
    val originalProviderUrl: String? = null,
    val licenseType: String,
    val attributionInfo: String,
    val commercialUsePermitted: Boolean = false,
    val streamingPermitted: Boolean = false,
    val territoryRestrictions: String? = null,
    val isVideoEmbedOnly: Boolean = false,
    val artistId: String? = null,
    val albumId: String? = null,
    val releaseDate: String? = null,
    val licenseUrl: String? = null,
    val isTrending: Boolean = false,
    val isNewRelease: Boolean = false,
    val isrc: String? = null,
    val sources: List<TrackSource> = emptyList(),
    val trendMetrics: TrendMetrics? = null
) {
    fun resolvedSources(): List<TrackSource> =
        sources.ifEmpty {
            listOf(
                TrackSource(
                    providerId = providerId,
                    providerTrackId = providerTrackId,
                    streamUrl = if (streamingPermitted && !isVideoEmbedOnly) streamUrl else null,
                    originalProviderUrl = originalProviderUrl,
                    licenseType = licenseType,
                    attributionInfo = attributionInfo,
                    licenseUrl = licenseUrl,
                    commercialUsePermitted = commercialUsePermitted,
                    streamingPermitted = streamingPermitted && !isVideoEmbedOnly && !streamUrl.isNullOrBlank(),
                    territoryRestrictions = territoryRestrictions,
                    isVideoEmbedOnly = isVideoEmbedOnly,
                    isrc = isrc,
                    durationSeconds = durationSeconds,
                    album = album
                )
            )
        }
}

data class UnifiedArtist(
    val id: String,
    val providerId: ProviderId,
    val providerArtistId: String,
    val name: String,
    val genre: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val joinDate: String? = null,
    val website: String? = null,
    val originalProviderUrl: String? = null
)

data class UnifiedAlbum(
    val id: String,
    val providerId: ProviderId,
    val providerAlbumId: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val releaseDate: String? = null,
    val genre: String,
    val artworkUrl: String? = null,
    val originalProviderUrl: String? = null,
    val trackIds: List<String> = emptyList()
)

data class UnifiedPlaylist(
    val id: String,
    val providerId: ProviderId,
    val providerPlaylistId: String,
    val title: String,
    val subtitle: String,
    val artworkUrl: String? = null,
    val originalProviderUrl: String? = null,
    val trackIds: List<String> = emptyList(),
    val displayedSongCount: Int = trackIds.size
)

data class ProviderFailure(
    val providerId: ProviderId,
    val message: String,
    val isRateLimited: Boolean = false,
    val isUnconfigured: Boolean = false
)

/**
 * Common interface implemented independently by every music/discovery provider.
 */
interface MusicProvider {
    val providerId: ProviderId
    val displayName: String
    val isEnabled: Boolean

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<UnifiedTrack>>
    suspend fun getTrackDetails(trackId: String): Result<UnifiedTrack>
    suspend fun getArtistDetails(artistId: String): Result<Pair<UnifiedArtist?, List<UnifiedTrack>>>
    suspend fun getAlbumDetails(albumId: String): Result<Pair<UnifiedAlbum?, List<UnifiedTrack>>>
    suspend fun getPlaylists(query: String? = null, limit: Int = 12): Result<List<UnifiedPlaylist>>
    suspend fun getArtworkUrl(trackId: String): Result<String?>
    suspend fun getAuthorizedStreamUrl(trackId: String): Result<String?>
}

fun UnifiedTrack.toDomainTrack(index: Int = 0): Track {
    val fallbackArtRes = when (index % 4) {
        0 -> R.drawable.img_cover_midnight
        1 -> R.drawable.img_cover_afro
        2 -> R.drawable.img_cover_chill
        else -> R.drawable.img_hero_music
    }
    val accent = when (providerId) {
        ProviderId.JAMENDO -> Color(0xFFC6F24E)
        ProviderId.AUDIUS -> Color(0xFF9E7BFF)
        ProviderId.YOUTUBE -> Color(0xFFFF5A5F)
        ProviderId.SOUNDCLOUD -> Color(0xFFFF8A3D)
        ProviderId.SPOTIFY -> Color(0xFF1DB954)
    }
    return Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        durationSeconds = durationSeconds.coerceAtLeast(1),
        artwork = ArtworkDescriptor(
            imageRes = fallbackArtRes,
            primaryColor = Color(0xFF242B1E),
            secondaryColor = Color(0xFF151611),
            accentColor = accent
        ),
        coverUrl = artworkUrl,
        streamUrl = if (streamingPermitted && !isVideoEmbedOnly) streamUrl else null,
        artistId = artistId,
        albumId = albumId,
        releaseDate = releaseDate,
        licenseUrl = licenseUrl,
        shareUrl = originalProviderUrl,
        attributionLabel = attributionInfo,
        isCommercialLicensed = commercialUsePermitted,
        isTrending = isTrending,
        isNewRelease = isNewRelease,
        isUpbeat = index % 2 == 0,
        providerId = providerId,
        providerTrackId = providerTrackId,
        originalProviderUrl = originalProviderUrl,
        licenseType = licenseType,
        territoryRestrictions = territoryRestrictions,
        streamingPermitted = streamingPermitted && !isVideoEmbedOnly && !streamUrl.isNullOrBlank(),
        isVideoEmbedOnly = isVideoEmbedOnly,
        isrc = isrc,
        sources = resolvedSources(),
        trendMetrics = trendMetrics
    )
}

fun UnifiedArtist.toDomainArtist(index: Int = 0): Artist {
    val fallbackArtRes = when (index % 3) {
        0 -> R.drawable.img_cover_midnight
        1 -> R.drawable.img_cover_afro
        else -> R.drawable.img_cover_chill
    }
    return Artist(
        id = id,
        name = name,
        genre = genre,
        subtitle = subtitle,
        artwork = ArtworkDescriptor(imageRes = fallbackArtRes),
        imageUrl = artworkUrl,
        joinDate = joinDate,
        website = website,
        shareUrl = originalProviderUrl,
        providerId = providerId,
        originalProviderUrl = originalProviderUrl
    )
}

fun UnifiedAlbum.toDomainAlbum(index: Int = 0): Album {
    val fallbackArtRes = when (index % 3) {
        0 -> R.drawable.img_cover_chill
        1 -> R.drawable.img_cover_afro
        else -> R.drawable.img_cover_midnight
    }
    return Album(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        releaseDate = releaseDate,
        genre = genre,
        trackIds = trackIds,
        artwork = ArtworkDescriptor(imageRes = fallbackArtRes),
        coverUrl = artworkUrl,
        shareUrl = originalProviderUrl,
        providerId = providerId,
        originalProviderUrl = originalProviderUrl
    )
}

fun UnifiedPlaylist.toDomainPlaylist(index: Int = 0): Playlist {
    val fallbackArtRes = when (index % 3) {
        0 -> R.drawable.img_cover_afro
        1 -> R.drawable.img_cover_chill
        else -> R.drawable.img_cover_midnight
    }
    return Playlist(
        id = id,
        title = title,
        subtitle = subtitle,
        trackIds = trackIds,
        artwork = ArtworkDescriptor(imageRes = fallbackArtRes),
        displayedSongCount = displayedSongCount,
        isUserCreated = false,
        providerId = providerId,
        coverUrl = artworkUrl,
        originalProviderUrl = originalProviderUrl
    )
}

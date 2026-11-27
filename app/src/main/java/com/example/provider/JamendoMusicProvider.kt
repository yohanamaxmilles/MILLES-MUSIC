package com.example.provider

import com.example.data.JamendoRepository
import com.example.model.ProviderId
import com.example.model.Track

/**
 * Preserves the existing JamendoRepository catalog, licensing, and Media3 streaming integration
 * while exposing it through the unified MusicProvider interface.
 */
class JamendoMusicProvider(
    private val jamendoRepository: JamendoRepository
) : MusicProvider {

    override val providerId: ProviderId = ProviderId.JAMENDO
    override val displayName: String = "Jamendo"
    override val isEnabled: Boolean
        get() = jamendoRepository.isConfigured

    override suspend fun searchTracks(query: String, limit: Int): Result<List<UnifiedTrack>> =
        jamendoRepository.searchTracks(query = query, limit = limit).map { tracks ->
            tracks.map { it.toUnifiedTrack() }
        }

    override suspend fun getTrackDetails(trackId: String): Result<UnifiedTrack> {
        val rawId = trackId.removePrefix("jam_")
        val tracks = jamendoRepository.searchTracks(query = rawId, limit = 10)
            .getOrElse { return Result.failure(it) }
        val matched = tracks.firstOrNull { it.id == trackId || it.id == "jam_$rawId" }
            ?: return Result.failure(IllegalStateException("Track not found on Jamendo."))
        return Result.success(matched.toUnifiedTrack())
    }

    override suspend fun getArtistDetails(artistId: String): Result<Pair<UnifiedArtist?, List<UnifiedTrack>>> {
        val tracksResult = jamendoRepository.fetchTracksByArtist(artistId)
        if (tracksResult.isFailure) {
            return Result.failure(tracksResult.exceptionOrNull() ?: IllegalStateException("Unable to load Jamendo artist."))
        }
        val tracks = tracksResult.getOrNull().orEmpty().map { it.toUnifiedTrack() }
        val first = tracks.firstOrNull()
        val artist = first?.let {
            UnifiedArtist(
                id = artistId,
                providerId = ProviderId.JAMENDO,
                providerArtistId = artistId.removePrefix("jam_art_"),
                name = it.artist,
                genre = it.genre,
                subtitle = "Jamendo Independent Artist",
                artworkUrl = it.artworkUrl,
                originalProviderUrl = it.originalProviderUrl
            )
        }
        return Result.success(artist to tracks)
    }

    override suspend fun getAlbumDetails(albumId: String): Result<Pair<UnifiedAlbum?, List<UnifiedTrack>>> {
        val tracksResult = jamendoRepository.fetchTracksByAlbum(albumId)
        if (tracksResult.isFailure) {
            return Result.failure(tracksResult.exceptionOrNull() ?: IllegalStateException("Unable to load Jamendo album."))
        }
        val tracks = tracksResult.getOrNull().orEmpty().map { it.toUnifiedTrack() }
        val first = tracks.firstOrNull()
        val album = first?.let {
            UnifiedAlbum(
                id = albumId,
                providerId = ProviderId.JAMENDO,
                providerAlbumId = albumId.removePrefix("jam_alb_"),
                title = it.album,
                artist = it.artist,
                artistId = it.artistId,
                releaseDate = it.releaseDate,
                genre = it.genre,
                artworkUrl = it.artworkUrl,
                originalProviderUrl = it.originalProviderUrl,
                trackIds = tracks.map { track -> track.trackId }
            )
        }
        return Result.success(album to tracks)
    }

    override suspend fun getPlaylists(query: String?, limit: Int): Result<List<UnifiedPlaylist>> {
        val albumsResult = jamendoRepository.fetchAlbums(
            limit = limit,
            nameSearch = query?.takeIf { it.isNotBlank() }
        )
        return albumsResult.map { albums ->
            albums.map { album ->
                UnifiedPlaylist(
                    id = "jam_pl_${album.id.removePrefix("jam_alb_")}",
                    providerId = ProviderId.JAMENDO,
                    providerPlaylistId = album.id.removePrefix("jam_alb_"),
                    title = album.title,
                    subtitle = "Jamendo · ${album.artist}",
                    artworkUrl = album.coverUrl,
                    originalProviderUrl = album.originalProviderUrl,
                    trackIds = album.trackIds,
                    displayedSongCount = album.trackIds.size
                )
            }
        }
    }

    override suspend fun getArtworkUrl(trackId: String): Result<String?> =
        getTrackDetails(trackId).map { it.artworkUrl }

    override suspend fun getAuthorizedStreamUrl(trackId: String): Result<String?> =
        getTrackDetails(trackId).map { track ->
            if (track.streamingPermitted) track.streamUrl else null
        }

    private fun Track.toUnifiedTrack(): UnifiedTrack = UnifiedTrack(
        trackId = id,
        providerId = ProviderId.JAMENDO,
        providerTrackId = id.removePrefix("jam_"),
        title = title,
        artist = artist,
        album = album,
        genre = genre,
        durationSeconds = durationSeconds,
        artworkUrl = coverUrl,
        streamUrl = streamUrl,
        originalProviderUrl = shareUrl,
        licenseType = licenseType,
        attributionInfo = attributionLabel,
        commercialUsePermitted = isCommercialLicensed,
        streamingPermitted = !streamUrl.isNullOrBlank(),
        territoryRestrictions = null,
        isVideoEmbedOnly = false,
        artistId = artistId,
        albumId = albumId,
        releaseDate = releaseDate,
        licenseUrl = licenseUrl,
        isTrending = isTrending,
        isNewRelease = isNewRelease
    )
}

package com.example.provider

import com.example.model.ProviderId

/**
 * Prepared SoundCloud provider adapter.
 *
 * Remains disabled (`isEnabled = false`) until official SoundCloud OAuth client credentials
 * and API approval are granted. Web scraping or unauthorized stream extraction is strictly omitted.
 */
class SoundCloudMusicProvider : MusicProvider {

    override val providerId: ProviderId = ProviderId.SOUNDCLOUD
    override val displayName: String = "SoundCloud"
    override val isEnabled: Boolean = false

    private fun disabledFailure(): IllegalStateException =
        IllegalStateException("SoundCloud provider is inactive awaiting official API approval.")

    override suspend fun searchTracks(query: String, limit: Int): Result<List<UnifiedTrack>> =
        Result.failure(disabledFailure())

    override suspend fun getTrackDetails(trackId: String): Result<UnifiedTrack> =
        Result.failure(disabledFailure())

    override suspend fun getArtistDetails(artistId: String): Result<Pair<UnifiedArtist?, List<UnifiedTrack>>> =
        Result.failure(disabledFailure())

    override suspend fun getAlbumDetails(albumId: String): Result<Pair<UnifiedAlbum?, List<UnifiedTrack>>> =
        Result.failure(disabledFailure())

    override suspend fun getPlaylists(query: String?, limit: Int): Result<List<UnifiedPlaylist>> =
        Result.failure(disabledFailure())

    override suspend fun getArtworkUrl(trackId: String): Result<String?> =
        Result.failure(disabledFailure())

    override suspend fun getAuthorizedStreamUrl(trackId: String): Result<String?> =
        Result.failure(disabledFailure())
}

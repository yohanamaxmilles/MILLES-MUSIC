package com.example.provider

import com.example.model.ProviderId

/**
 * Metadata-only boundary stub for Spotify.
 *
 * Audio extraction, downloading, stream proxying, and unapproved playback are strictly prohibited.
 * Remains disabled (`isEnabled = false`) until official Spotify Web API client credentials and
 * permitted playback integration are explicitly provisioned.
 */
class SpotifyMetadataProvider : MusicProvider {

    override val providerId: ProviderId = ProviderId.SPOTIFY
    override val displayName: String = "Spotify"
    override val isEnabled: Boolean = false

    private fun policyError(operation: String): UnsupportedOperationException =
        UnsupportedOperationException(
            "Spotify adapter is metadata-only and inactive ($operation blocked: audio extraction or streaming is prohibited)."
        )

    override suspend fun searchTracks(query: String, limit: Int): Result<List<UnifiedTrack>> =
        Result.failure(policyError("searchTracks"))

    override suspend fun getTrackDetails(trackId: String): Result<UnifiedTrack> =
        Result.failure(policyError("getTrackDetails"))

    override suspend fun getArtistDetails(artistId: String): Result<Pair<UnifiedArtist?, List<UnifiedTrack>>> =
        Result.failure(policyError("getArtistDetails"))

    override suspend fun getAlbumDetails(albumId: String): Result<Pair<UnifiedAlbum?, List<UnifiedTrack>>> =
        Result.failure(policyError("getAlbumDetails"))

    override suspend fun getPlaylists(query: String?, limit: Int): Result<List<UnifiedPlaylist>> =
        Result.failure(policyError("getPlaylists"))

    override suspend fun getArtworkUrl(trackId: String): Result<String?> =
        Result.failure(policyError("getArtworkUrl"))

    override suspend fun getAuthorizedStreamUrl(trackId: String): Result<String?> =
        Result.failure(policyError("getAuthorizedStreamUrl"))
}

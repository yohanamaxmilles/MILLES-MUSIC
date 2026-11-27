package com.example.provider

import com.example.data.JamendoRepository
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ProviderId
import com.example.model.Track
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class UnifiedSearchResult(
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val providerFailures: List<ProviderFailure> = emptyList()
)

/**
 * Coordinates searches across enabled music providers concurrently, isolates provider errors,
 * and deduplicates overlapping audio entries while preserving official YouTube video items.
 */
class UnifiedMusicCatalogService(
    private val jamendoRepository: JamendoRepository,
    val jamendoProvider: JamendoMusicProvider,
    val audiusProvider: AudiusMusicProvider,
    val youTubeProvider: YouTubeMusicProvider,
    val soundCloudProvider: SoundCloudMusicProvider = SoundCloudMusicProvider(),
    val spotifyProvider: SpotifyMetadataProvider = SpotifyMetadataProvider()
) {
    val allProviders: List<MusicProvider>
        get() = listOf(jamendoProvider, audiusProvider, youTubeProvider, soundCloudProvider, spotifyProvider)

    suspend fun search(
        query: String,
        providerFilter: ProviderId? = null
    ): UnifiedSearchResult = coroutineScope {
        val clean = query.trim()
        if (clean.isEmpty()) return@coroutineScope UnifiedSearchResult()

        val includeJamendo = providerFilter == null || providerFilter == ProviderId.JAMENDO
        val includeAudius = providerFilter == null || providerFilter == ProviderId.AUDIUS
        val includeYouTube = providerFilter == null || providerFilter == ProviderId.YOUTUBE

        val jamendoTracksJob = if (includeJamendo && jamendoProvider.isEnabled) {
            async { jamendoProvider.searchTracks(clean, limit = 20) }
        } else null
        val jamendoArtistsJob = if (includeJamendo && jamendoProvider.isEnabled) {
            async { jamendoRepository.fetchArtists(limit = 8, nameSearch = clean) }
        } else null
        val jamendoAlbumsJob = if (includeJamendo && jamendoProvider.isEnabled) {
            async { jamendoRepository.fetchAlbums(limit = 8, nameSearch = clean) }
        } else null

        val audiusTracksJob = if (includeAudius && audiusProvider.isEnabled) {
            async { audiusProvider.searchTracks(clean, limit = 20) }
        } else null
        val audiusArtistsJob = if (includeAudius && audiusProvider.isEnabled) {
            async { audiusProvider.searchArtists(clean, limit = 8) }
        } else null

        val youTubeJob = if (includeYouTube && youTubeProvider.isEnabled) {
            async { youTubeProvider.searchTracks(clean, limit = 12) }
        } else null

        val failures = mutableListOf<ProviderFailure>()

        if (includeJamendo && !jamendoProvider.isEnabled) {
            failures += ProviderFailure(
                providerId = ProviderId.JAMENDO,
                message = "Jamendo Client ID is not configured.",
                isUnconfigured = true
            )
        }
        if (includeYouTube && !youTubeProvider.isEnabled && providerFilter == ProviderId.YOUTUBE) {
            failures += ProviderFailure(
                providerId = ProviderId.YOUTUBE,
                message = "YouTube API key not configured. Add YOUTUBE_API_KEY in AI Studio Secrets.",
                isUnconfigured = true
            )
        }
        if (providerFilter == ProviderId.SOUNDCLOUD) {
            failures += ProviderFailure(
                providerId = ProviderId.SOUNDCLOUD,
                message = "SoundCloud integration awaits official API approval.",
                isUnconfigured = true
            )
        }
        if (providerFilter == ProviderId.SPOTIFY) {
            failures += ProviderFailure(
                providerId = ProviderId.SPOTIFY,
                message = "Spotify adapter is metadata-only and inactive.",
                isUnconfigured = true
            )
        }

        val jamendoResult = jamendoTracksJob?.await()
        val audiusResult = audiusTracksJob?.await()
        val youTubeResult = youTubeJob?.await()

        jamendoResult?.exceptionOrNull()?.let { err ->
            failures += ProviderFailure(ProviderId.JAMENDO, err.message ?: "Jamendo search failed.")
        }
        audiusResult?.exceptionOrNull()?.let { err ->
            val rateLimited = err is AudiusMusicProvider.AudiusApiException && err.isRateLimited
            failures += ProviderFailure(
                providerId = ProviderId.AUDIUS,
                message = err.message ?: "Audius search failed.",
                isRateLimited = rateLimited
            )
        }
        youTubeResult?.exceptionOrNull()?.let { err ->
            failures += ProviderFailure(
                providerId = ProviderId.YOUTUBE,
                message = err.message ?: "YouTube search failed.",
                isUnconfigured = err is YouTubeMusicProvider.YouTubeUnconfiguredException
            )
        }

        val jamendoUnified = jamendoResult?.getOrNull().orEmpty()
        val audiusUnified = audiusResult?.getOrNull().orEmpty()
        val youTubeUnified = youTubeResult?.getOrNull().orEmpty()

        val mergedAudio = deduplicateAudioTracks(
            interleave(jamendoUnified, audiusUnified)
        )
        val mergedAll = interleaveWithVideos(mergedAudio, youTubeUnified)
            .mapIndexed { idx, unified -> unified.toDomainTrack(index = idx) }

        val jamendoArtists = jamendoArtistsJob?.await()?.getOrNull().orEmpty()
        val audiusArtists = audiusArtistsJob?.await()?.getOrNull().orEmpty()
            .mapIndexed { idx, artist -> artist.toDomainArtist(index = idx) }
        val mergedArtists = (jamendoArtists + audiusArtists).distinctBy { it.id }

        val mergedAlbums = jamendoAlbumsJob?.await()?.getOrNull().orEmpty()

        UnifiedSearchResult(
            tracks = mergedAll,
            artists = mergedArtists,
            albums = mergedAlbums,
            providerFailures = failures
        )
    }

    private fun interleave(first: List<UnifiedTrack>, second: List<UnifiedTrack>): List<UnifiedTrack> {
        val result = ArrayList<UnifiedTrack>(first.size + second.size)
        val max = maxOf(first.size, second.size)
        for (i in 0 until max) {
            if (i < first.size) result += first[i]
            if (i < second.size) result += second[i]
        }
        return result
    }

    private fun interleaveWithVideos(audio: List<UnifiedTrack>, videos: List<UnifiedTrack>): List<UnifiedTrack> {
        if (videos.isEmpty()) return audio
        if (audio.isEmpty()) return videos
        val result = ArrayList<UnifiedTrack>(audio.size + videos.size)
        var vIdx = 0
        audio.forEachIndexed { index, track ->
            result += track
            if ((index + 1) % 3 == 0 && vIdx < videos.size) {
                result += videos[vIdx++]
            }
        }
        while (vIdx < videos.size) {
            result += videos[vIdx++]
        }
        return result
    }

    private fun deduplicateAudioTracks(tracks: List<UnifiedTrack>): List<UnifiedTrack> {
        val seenKeys = HashSet<String>()
        val result = mutableListOf<UnifiedTrack>()
        for (track in tracks) {
            if (track.isVideoEmbedOnly) {
                result += track
                continue
            }
            val key = "${normalizeKey(track.title)}|${normalizeKey(track.artist)}"
            if (seenKeys.add(key)) {
                result += track
            }
        }
        return result
    }

    private fun normalizeKey(text: String): String =
        text.lowercase().replace(Regex("[^a-z0-9]"), "")
}

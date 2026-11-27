package com.example.provider

import com.example.data.JamendoRepository
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.JamendoGenres
import com.example.model.Playlist
import com.example.model.ProviderId
import com.example.model.Track
import com.example.R
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class UnifiedSearchResult(
    val tracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val providerFailures: List<ProviderFailure> = emptyList(),
    val hasMore: Boolean = false
)

data class UnifiedHomeCatalog(
    val trendingNow: List<Track> = emptyList(),
    val popularTracks: List<Track> = emptyList(),
    val recommendedTracks: List<Track> = emptyList(),
    val newReleases: List<Track> = emptyList(),
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val providerFailures: List<ProviderFailure> = emptyList()
)

data class UnifiedPagedTracksResult(
    val tracks: List<Track> = emptyList(),
    val providerFailures: List<ProviderFailure> = emptyList(),
    val hasMore: Boolean = false
)

/**
 * Single unified music aggregation service coordinating Jamendo, Audius, YouTube (embed-only),
 * and prepared adapters concurrently with TTL caching, pagination, cross-provider deduplication,
 * and strict provider fault isolation.
 */
class UnifiedMusicCatalogService(
    private val jamendoRepository: JamendoRepository,
    val jamendoProvider: JamendoMusicProvider,
    val audiusProvider: AudiusMusicProvider,
    val youTubeProvider: YouTubeMusicProvider,
    val soundCloudProvider: SoundCloudMusicProvider = SoundCloudMusicProvider(),
    val spotifyProvider: SpotifyMetadataProvider = SpotifyMetadataProvider(),
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    private val logger = Logger.getLogger("UnifiedMusicCatalogService")

    private data class CacheEntry<T>(val value: T, val timestampMs: Long)

    private val homeCache = ConcurrentHashMap<String, CacheEntry<UnifiedHomeCatalog>>()
    private val searchCache = ConcurrentHashMap<String, CacheEntry<UnifiedSearchResult>>()
    private val genreCache = ConcurrentHashMap<String, CacheEntry<UnifiedPagedTracksResult>>()

    val allProviders: List<MusicProvider>
        get() = listOf(jamendoProvider, audiusProvider, youTubeProvider, soundCloudProvider, spotifyProvider)

    suspend fun loadHomeCatalog(
        preferredGenreTag: String? = null,
        forceRefresh: Boolean = false
    ): UnifiedHomeCatalog {
        val cacheKey = preferredGenreTag?.trim()?.lowercase().orEmpty()
        if (!forceRefresh) {
            homeCache[cacheKey]?.takeIf { !isExpired(it.timestampMs) }?.let { return it.value }
        }

        val catalog = coroutineScope {
            val jamTrendingJob = if (jamendoProvider.isEnabled) {
                async { jamendoProvider.fetchTrendingCharts(limit = 20, offset = 0) }
            } else null
            val jamPopularJob = if (jamendoProvider.isEnabled) {
                async { jamendoProvider.fetchPopularTracks(limit = 20, offset = 0) }
            } else null
            val jamRecommendedJob = if (jamendoProvider.isEnabled) {
                async { jamendoProvider.fetchRecommendedTracks(genreTag = preferredGenreTag, limit = 16, offset = 0) }
            } else null
            val jamNewReleasesJob = if (jamendoProvider.isEnabled) {
                async { jamendoProvider.fetchNewReleases(limit = 16, offset = 0) }
            } else null
            val jamRecentAlbumsJob = if (jamendoProvider.isEnabled) {
                async { jamendoRepository.fetchAlbums(limit = 12, order = "releasedate_desc") }
            } else null
            val jamArtistsJob = if (jamendoProvider.isEnabled) {
                async { jamendoRepository.fetchArtists(limit = 20) }
            } else null
            val jamAlbumsJob = if (jamendoProvider.isEnabled) {
                async { jamendoRepository.fetchAlbums(limit = 20, order = "popularity_week") }
            } else null

            val audiusTrendingWeekJob = if (audiusProvider.isEnabled) {
                async { audiusProvider.fetchTrendingTracks(limit = 20, offset = 0, time = "week") }
            } else null
            val audiusTrendingMonthJob = if (audiusProvider.isEnabled) {
                async { audiusProvider.fetchTrendingTracks(limit = 20, offset = 0, time = "month") }
            } else null
            val audiusNewReleasesJob = if (audiusProvider.isEnabled) {
                async { audiusProvider.fetchNewReleases(limit = 16, offset = 0) }
            } else null
            val audiusRecommendedJob = if (audiusProvider.isEnabled) {
                async {
                    val genre = preferredGenreTag?.takeIf { it.isNotBlank() }
                    if (genre != null) audiusProvider.fetchTracksByGenre(genre, limit = 16)
                    else audiusProvider.fetchTrendingTracks(limit = 16, time = "allTime")
                }
            } else null
            val audiusPlaylistsJob = if (audiusProvider.isEnabled) {
                async { audiusProvider.getPlaylists(limit = 12) }
            } else null

            val failures = mutableListOf<ProviderFailure>()
            if (!jamendoProvider.isEnabled) {
                failures += ProviderFailure(
                    providerId = ProviderId.JAMENDO,
                    message = "Add JAMENDO_CLIENT_ID in AI Studio Secrets to include Jamendo tracks.",
                    isUnconfigured = true
                )
            }

            val jamTrendingRes = jamTrendingJob?.await()
            val jamPopularRes = jamPopularJob?.await()
            val jamRecommendedRes = jamRecommendedJob?.await()
            val jamNewReleasesRes = jamNewReleasesJob?.await()
            val jamRecentAlbumsRes = jamRecentAlbumsJob?.await()
            val jamArtistsRes = jamArtistsJob?.await()
            val jamAlbumsRes = jamAlbumsJob?.await()

            val audiusWeekRes = audiusTrendingWeekJob?.await()
            val audiusMonthRes = audiusTrendingMonthJob?.await()
            val audiusNewRes = audiusNewReleasesJob?.await()
            val audiusRecRes = audiusRecommendedJob?.await()
            val audiusPlaylistsRes = audiusPlaylistsJob?.await()

            recordFailure(
                ProviderId.JAMENDO,
                jamTrendingRes?.exceptionOrNull()
                    ?: jamPopularRes?.exceptionOrNull()
                    ?: jamRecommendedRes?.exceptionOrNull()
                    ?: jamNewReleasesRes?.exceptionOrNull(),
                failures
            )
            recordFailure(
                ProviderId.AUDIUS,
                audiusWeekRes?.exceptionOrNull()
                    ?: audiusMonthRes?.exceptionOrNull()
                    ?: audiusNewRes?.exceptionOrNull()
                    ?: audiusRecRes?.exceptionOrNull(),
                failures
            )

            val jamTrending = jamTrendingRes?.getOrNull().orEmpty()
            val audiusWeek = audiusWeekRes?.getOrNull().orEmpty()

            // Transparent cross-provider Trends Now ranking using real chart ranks, stats, and freshness
            val trendingBatches = buildMap {
                if (jamTrending.isNotEmpty()) put(ProviderId.JAMENDO, jamTrending)
                if (audiusWeek.isNotEmpty()) put(ProviderId.AUDIUS, audiusWeek)
            }
            val unifiedTrending = TrendingRankingEngine.rankUnifiedTrending(trendingBatches, timeProvider())
                .mapIndexed { idx, u -> u.toDomainTrack(index = idx) }

            val jamPopular = jamPopularRes?.getOrNull().orEmpty()
            val audiusMonth = audiusMonthRes?.getOrNull().orEmpty()
            val unifiedPopular = TrackDeduplicator.deduplicate(interleave(jamPopular, audiusMonth))
                .mapIndexed { idx, u -> u.toDomainTrack(index = idx) }

            val jamRecommended = jamRecommendedRes?.getOrNull().orEmpty()
            val audiusRecommended = audiusRecRes?.getOrNull().orEmpty()
            val unifiedRecommended = TrackDeduplicator.deduplicate(interleave(jamRecommended, audiusRecommended))
                .mapIndexed { idx, u -> u.toDomainTrack(index = idx) }

            val jamNew = jamNewReleasesRes?.getOrNull().orEmpty()
            val audiusNew = audiusNewRes?.getOrNull().orEmpty()
            val unifiedNewReleases = TrackDeduplicator.deduplicate(interleave(jamNew, audiusNew))
                .sortedByDescending { it.releaseDate.orEmpty() }
                .mapIndexed { idx, u -> u.toDomainTrack(index = idx) }

            val jamRecentAlbums = jamRecentAlbumsRes?.getOrNull().orEmpty()
            val jamAlbums = jamAlbumsRes?.getOrNull().orEmpty()
            val audiusPlaylists = audiusPlaylistsRes?.getOrNull().orEmpty()
                .mapIndexed { idx, u -> u.toDomainPlaylist(index = idx) }
            val audiusPlaylistsAsAlbums = audiusPlaylists.map { pl ->
                Album(
                    id = pl.id,
                    title = pl.title,
                    artist = pl.subtitle.removePrefix("Audius · "),
                    genre = "Audius Collection",
                    trackIds = pl.trackIds,
                    artwork = pl.artwork,
                    coverUrl = pl.coverUrl,
                    shareUrl = pl.originalProviderUrl,
                    providerId = ProviderId.AUDIUS,
                    originalProviderUrl = pl.originalProviderUrl
                )
            }
            val unifiedAlbums = interleaveGeneric(jamAlbums, audiusPlaylistsAsAlbums).distinctBy { it.id }
            val unifiedRecentAlbums = interleaveGeneric(jamRecentAlbums, audiusPlaylistsAsAlbums.take(6)).distinctBy { it.id }

            val jamArtists = jamArtistsRes?.getOrNull().orEmpty()
            val audiusArtistsFromCharts = (audiusWeek + audiusMonth)
                .mapNotNull { track ->
                    val artId = track.artistId ?: return@mapNotNull null
                    Artist(
                        id = artId,
                        name = track.artist,
                        genre = track.genre,
                        subtitle = "Audius Creator",
                        artwork = ArtworkDescriptor(imageRes = R.drawable.img_cover_midnight),
                        imageUrl = track.artworkUrl,
                        shareUrl = track.originalProviderUrl,
                        providerId = ProviderId.AUDIUS,
                        originalProviderUrl = track.originalProviderUrl
                    )
                }
                .distinctBy { it.id }
            val unifiedArtists = interleaveGeneric(jamArtists, audiusArtistsFromCharts).distinctBy { it.id }

            UnifiedHomeCatalog(
                trendingNow = unifiedTrending,
                popularTracks = unifiedPopular,
                recommendedTracks = unifiedRecommended,
                newReleases = unifiedNewReleases,
                recentlyAddedAlbums = unifiedRecentAlbums,
                artists = unifiedArtists,
                albums = unifiedAlbums,
                playlists = audiusPlaylists,
                providerFailures = failures
            )
        }

        val hasTransientFailure = catalog.providerFailures.any { !it.isUnconfigured }
        val hasContent = catalog.trendingNow.isNotEmpty() || catalog.popularTracks.isNotEmpty() || catalog.newReleases.isNotEmpty()
        if (hasContent && !hasTransientFailure) {
            homeCache[cacheKey] = CacheEntry(catalog, timeProvider())
        }
        return catalog
    }

    suspend fun loadMoreTrending(page: Int, pageSize: Int = 20): UnifiedPagedTracksResult = coroutineScope {
        val offset = page.coerceAtLeast(0) * pageSize
        val jamJob = if (jamendoProvider.isEnabled) async { jamendoProvider.fetchTrendingCharts(limit = pageSize, offset = offset) } else null
        val audiusJob = if (audiusProvider.isEnabled) async { audiusProvider.fetchTrendingTracks(limit = pageSize, offset = offset, time = "week") } else null

        val failures = mutableListOf<ProviderFailure>()
        val jamRes = jamJob?.await()
        val audiusRes = audiusJob?.await()
        recordFailure(ProviderId.JAMENDO, jamRes?.exceptionOrNull(), failures)
        recordFailure(ProviderId.AUDIUS, audiusRes?.exceptionOrNull(), failures)

        val jamTracks = jamRes?.getOrNull().orEmpty()
        val audiusTracks = audiusRes?.getOrNull().orEmpty()
        val batches = buildMap {
            if (jamTracks.isNotEmpty()) put(ProviderId.JAMENDO, jamTracks)
            if (audiusTracks.isNotEmpty()) put(ProviderId.AUDIUS, audiusTracks)
        }
        val ranked = TrendingRankingEngine.rankUnifiedTrending(batches, timeProvider())
            .mapIndexed { idx, u -> u.toDomainTrack(index = offset + idx) }
        UnifiedPagedTracksResult(
            tracks = ranked,
            providerFailures = failures,
            hasMore = jamTracks.size >= pageSize || audiusTracks.size >= pageSize
        )
    }

    suspend fun loadTracksByGenre(
        genreId: String,
        genreName: String,
        page: Int = 0,
        pageSize: Int = 20,
        forceRefresh: Boolean = false
    ): UnifiedPagedTracksResult {
        val cacheKey = "$genreId:$page"
        if (!forceRefresh) {
            genreCache[cacheKey]?.takeIf { !isExpired(it.timestampMs) }?.let { return it.value }
        }
        val offset = page.coerceAtLeast(0) * pageSize
        val jamendoTag = JamendoGenres.jamendoTagForGenre(genreId, genreName)

        val result = coroutineScope {
            val jamJob = if (jamendoProvider.isEnabled) {
                async { jamendoProvider.fetchTracksByGenre(jamendoTag, limit = pageSize, offset = offset) }
            } else null
            val audiusJob = if (audiusProvider.isEnabled) {
                async { audiusProvider.fetchTracksByGenre(genreName, limit = pageSize, offset = offset) }
            } else null

            val failures = mutableListOf<ProviderFailure>()
            val jamRes = jamJob?.await()
            val audiusRes = audiusJob?.await()
            recordFailure(ProviderId.JAMENDO, jamRes?.exceptionOrNull(), failures)
            recordFailure(ProviderId.AUDIUS, audiusRes?.exceptionOrNull(), failures)

            val jamTracks = jamRes?.getOrNull().orEmpty()
            val audiusTracks = audiusRes?.getOrNull().orEmpty()
            val merged = TrackDeduplicator.deduplicate(interleave(jamTracks, audiusTracks))
                .mapIndexed { idx, u -> u.toDomainTrack(index = offset + idx) }

            UnifiedPagedTracksResult(
                tracks = merged,
                providerFailures = failures,
                hasMore = jamTracks.size >= pageSize || audiusTracks.size >= pageSize
            )
        }

        if (result.tracks.isNotEmpty()) {
            genreCache[cacheKey] = CacheEntry(result, timeProvider())
        }
        return result
    }

    suspend fun loadRelatedTracks(track: Track, limit: Int = 10): List<Track> = coroutineScope {
        val genreQuery = track.genre.takeIf { it.isNotBlank() } ?: return@coroutineScope emptyList()
        val jamJob = if (jamendoProvider.isEnabled) {
            async {
                val tag = JamendoGenres.jamendoTagForGenre("", genreQuery)
                jamendoProvider.fetchTracksByGenre(tag, limit = limit)
            }
        } else null
        val audiusJob = if (audiusProvider.isEnabled) {
            async { audiusProvider.fetchTracksByGenre(genreQuery, limit = limit) }
        } else null

        val jamTracks = jamJob?.await()?.getOrNull().orEmpty()
        val audiusTracks = audiusJob?.await()?.getOrNull().orEmpty()

        TrackDeduplicator.deduplicate(interleave(jamTracks, audiusTracks))
            .mapIndexed { idx, u -> u.toDomainTrack(index = idx) }
            .filter { it.id != track.id && it.streamingPermitted }
            .take(limit)
    }

    suspend fun search(
        query: String,
        providerFilter: ProviderId? = null,
        page: Int = 0,
        pageSize: Int = 20,
        forceRefresh: Boolean = false
    ): UnifiedSearchResult = coroutineScope {
        val clean = query.trim()
        if (clean.isEmpty()) return@coroutineScope UnifiedSearchResult()

        val cacheKey = "${clean.lowercase()}|${providerFilter?.id ?: "all"}|$page"
        if (!forceRefresh) {
            searchCache[cacheKey]?.takeIf { !isExpired(it.timestampMs) }?.let { return@coroutineScope it.value }
        }

        val offset = page.coerceAtLeast(0) * pageSize
        val includeJamendo = providerFilter == null || providerFilter == ProviderId.JAMENDO
        val includeAudius = providerFilter == null || providerFilter == ProviderId.AUDIUS
        val includeYouTube = providerFilter == null || providerFilter == ProviderId.YOUTUBE

        val jamendoTracksJob = if (includeJamendo && jamendoProvider.isEnabled) {
            async { jamendoProvider.searchTracks(clean, limit = pageSize, offset = offset) }
        } else null
        val jamendoArtistsJob = if (includeJamendo && jamendoProvider.isEnabled && page == 0) {
            async { jamendoRepository.fetchArtists(limit = 8, nameSearch = clean) }
        } else null
        val jamendoAlbumsJob = if (includeJamendo && jamendoProvider.isEnabled && page == 0) {
            async { jamendoRepository.fetchAlbums(limit = 8, nameSearch = clean) }
        } else null

        val audiusTracksJob = if (includeAudius && audiusProvider.isEnabled) {
            async { audiusProvider.searchTracks(clean, limit = pageSize, offset = offset) }
        } else null
        val audiusArtistsJob = if (includeAudius && audiusProvider.isEnabled && page == 0) {
            async { audiusProvider.searchArtists(clean, limit = 8) }
        } else null

        val youTubeJob = if (includeYouTube && youTubeProvider.isEnabled && page == 0) {
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

        recordFailure(ProviderId.JAMENDO, jamendoResult?.exceptionOrNull(), failures)
        recordFailure(ProviderId.AUDIUS, audiusResult?.exceptionOrNull(), failures)
        youTubeResult?.exceptionOrNull()?.let { err ->
            logger.log(Level.WARNING, "Provider YOUTUBE failure: ${err.message}")
            failures += ProviderFailure(
                providerId = ProviderId.YOUTUBE,
                message = err.message ?: "YouTube search failed.",
                isUnconfigured = err is YouTubeMusicProvider.YouTubeUnconfiguredException
            )
        }

        val jamendoUnified = jamendoResult?.getOrNull().orEmpty()
        val audiusUnified = audiusResult?.getOrNull().orEmpty()
        val youTubeUnified = youTubeResult?.getOrNull().orEmpty()

        // Deduplicate audio across providers (preserving all matched sources) and rank best matches first
        val mergedAudio = TrackDeduplicator.deduplicate(interleave(jamendoUnified, audiusUnified))
            .sortedByDescending { relevanceScore(it, clean) }
        val mergedAll = interleaveWithVideos(mergedAudio, youTubeUnified)
            .mapIndexed { idx, unified -> unified.toDomainTrack(index = offset + idx) }

        val jamendoArtists = jamendoArtistsJob?.await()?.getOrNull().orEmpty()
        val audiusArtists = audiusArtistsJob?.await()?.getOrNull().orEmpty()
            .mapIndexed { idx, artist -> artist.toDomainArtist(index = idx) }
        val mergedArtists = interleaveGeneric(jamendoArtists, audiusArtists).distinctBy { it.id }

        val mergedAlbums = jamendoAlbumsJob?.await()?.getOrNull().orEmpty()

        val result = UnifiedSearchResult(
            tracks = mergedAll,
            artists = mergedArtists,
            albums = mergedAlbums,
            providerFailures = failures,
            hasMore = jamendoUnified.size >= pageSize || audiusUnified.size >= pageSize
        )
        if (result.tracks.isNotEmpty() || result.artists.isNotEmpty() || result.albums.isNotEmpty()) {
            searchCache[cacheKey] = CacheEntry(result, timeProvider())
        }
        result
    }

    fun clearCaches() {
        homeCache.clear()
        searchCache.clear()
        genreCache.clear()
    }

    private fun relevanceScore(track: UnifiedTrack, query: String): Int {
        val q = query.trim().lowercase()
        val title = track.title.lowercase()
        val artist = track.artist.lowercase()
        var score = 0
        if (title == q) score += 100
        else if (title.startsWith(q)) score += 70
        else if (title.contains(q)) score += 45
        if (artist == q) score += 80
        else if (artist.startsWith(q)) score += 55
        else if (artist.contains(q)) score += 35
        if (track.resolvedSources().size > 1) score += 15
        if (track.streamingPermitted) score += 10
        return score
    }

    private fun recordFailure(
        providerId: ProviderId,
        error: Throwable?,
        failures: MutableList<ProviderFailure>
    ) {
        if (error == null) return
        logger.log(Level.WARNING, "Provider ${providerId.id} failure: ${error.message}")
        val rateLimited = error is AudiusMusicProvider.AudiusApiException && error.isRateLimited
        failures += ProviderFailure(
            providerId = providerId,
            message = error.message ?: "${providerId.displayName} request failed.",
            isRateLimited = rateLimited
        )
    }

    private fun isExpired(timestampMs: Long): Boolean =
        (timeProvider() - timestampMs) > cacheTtlMs

    private fun interleave(first: List<UnifiedTrack>, second: List<UnifiedTrack>): List<UnifiedTrack> {
        val result = ArrayList<UnifiedTrack>(first.size + second.size)
        val max = maxOf(first.size, second.size)
        for (i in 0 until max) {
            if (i < first.size) result += first[i]
            if (i < second.size) result += second[i]
        }
        return result
    }

    private fun <T> interleaveGeneric(first: List<T>, second: List<T>): List<T> {
        val result = ArrayList<T>(first.size + second.size)
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

    companion object {
        const val DEFAULT_CACHE_TTL_MS = 180_000L
    }
}

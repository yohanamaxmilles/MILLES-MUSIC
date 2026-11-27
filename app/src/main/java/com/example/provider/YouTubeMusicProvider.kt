package com.example.provider

import com.example.BuildConfig
import com.example.model.ProviderId
import com.example.network.YouTubeApiFactory
import com.example.network.YouTubeApiService
import com.example.network.YouTubeRegionRestrictionDto
import com.example.network.YouTubeSearchItemDto
import com.example.network.YouTubeVideoItemDto
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

/**
 * Official YouTube Data API v3 discovery provider.
 *
 * Strictly complies with YouTube Terms of Service:
 * - Only queries embeddable music videos (`type=video`, `videoCategoryId=10`, `videoEmbeddable=true`).
 * - NEVER returns downloadable or Media3-streamable audio URLs (`streamUrl = null`, `streamingPermitted = false`).
 * - Playback is restricted exclusively to the official YouTube embedded player (`isVideoEmbedOnly = true`).
 */
class YouTubeMusicProvider(
    private val apiService: YouTubeApiService = YouTubeApiFactory.create(),
    private val apiKey: String = BuildConfig.YOUTUBE_API_KEY
) : MusicProvider {

    override val providerId: ProviderId = ProviderId.YOUTUBE
    override val displayName: String = "YouTube"
    override val isEnabled: Boolean
        get() = apiKey.isNotBlank() && apiKey != PLACEHOLDER_API_KEY

    override suspend fun searchTracks(query: String, limit: Int): Result<List<UnifiedTrack>> {
        val clean = query.trim()
        if (clean.isEmpty()) return Result.success(emptyList())
        if (!isEnabled) {
            return Result.failure(YouTubeUnconfiguredException("YouTube Data API key is not configured."))
        }
        return executeQuery {
            val searchResp = apiService.searchMusicVideos(
                maxResults = limit.coerceIn(1, 20),
                query = clean,
                apiKey = apiKey
            )
            val items = searchResp.items.filter { !it.id?.videoId.isNullOrBlank() }
            if (items.isEmpty()) return@executeQuery emptyList()

            val ids = items.mapNotNull { it.id?.videoId }.distinct()
            val detailsById = runCatching {
                apiService.getVideoDetails(
                    videoIds = ids.joinToString(","),
                    apiKey = apiKey
                ).items.associateBy { it.id }
            }.getOrDefault(emptyMap())

            items.mapNotNull { item ->
                val videoId = item.id?.videoId ?: return@mapNotNull null
                val detail = detailsById[videoId]
                if (detail != null && detail.status?.embeddable == false) {
                    return@mapNotNull null
                }
                toUnifiedTrack(videoId = videoId, searchItem = item, detail = detail)
            }
        }
    }

    override suspend fun getTrackDetails(trackId: String): Result<UnifiedTrack> {
        if (!isEnabled) {
            return Result.failure(YouTubeUnconfiguredException("YouTube Data API key is not configured."))
        }
        val videoId = trackId.removePrefix(TRACK_PREFIX)
        return executeQuery {
            val detail = apiService.getVideoDetails(videoIds = videoId, apiKey = apiKey)
                .items.firstOrNull()
                ?: throw IllegalStateException("Video unavailable on YouTube.")
            if (detail.status?.embeddable == false) {
                throw IllegalStateException("This YouTube video does not allow embedded playback.")
            }
            toUnifiedTrack(videoId = videoId, searchItem = null, detail = detail)
        }
    }

    override suspend fun getArtistDetails(artistId: String): Result<Pair<UnifiedArtist?, List<UnifiedTrack>>> =
        Result.failure(UnsupportedOperationException("Use YouTube search to browse channels and videos."))

    override suspend fun getAlbumDetails(albumId: String): Result<Pair<UnifiedAlbum?, List<UnifiedTrack>>> =
        Result.failure(UnsupportedOperationException("YouTube provides video playback via official embed only."))

    override suspend fun getPlaylists(query: String?, limit: Int): Result<List<UnifiedPlaylist>> =
        Result.success(emptyList())

    override suspend fun getArtworkUrl(trackId: String): Result<String?> =
        getTrackDetails(trackId).map { it.artworkUrl }

    override suspend fun getAuthorizedStreamUrl(trackId: String): Result<String?> =
        Result.failure(
            UnsupportedOperationException(
                "YouTube audio stream extraction is prohibited. Play via the official YouTube embedded player."
            )
        )

    private fun toUnifiedTrack(
        videoId: String,
        searchItem: YouTubeSearchItemDto?,
        detail: YouTubeVideoItemDto?
    ): UnifiedTrack {
        val snippet = detail?.snippet ?: searchItem?.snippet
        val title = decodeBasicEntities(snippet?.title.orEmpty()).ifBlank { "YouTube Music Video" }
        val channel = decodeBasicEntities(snippet?.channelTitle.orEmpty()).ifBlank { "YouTube Channel" }
        val durationSec = parseIso8601DurationSeconds(detail?.contentDetails?.duration)
        val thumb = snippet?.thumbnails?.bestUrl()
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val territoryNote = formatTerritoryRestriction(detail?.contentDetails?.regionRestriction)

        return UnifiedTrack(
            trackId = "$TRACK_PREFIX$videoId",
            providerId = ProviderId.YOUTUBE,
            providerTrackId = videoId,
            title = title,
            artist = channel,
            album = "YouTube Video",
            genre = "Music Video",
            durationSeconds = durationSec.coerceAtLeast(1),
            artworkUrl = thumb,
            streamUrl = null, // Never populate raw stream URL for YouTube
            originalProviderUrl = watchUrl,
            licenseType = "YouTube Standard License · Official Embed Only",
            attributionInfo = "YouTube · Channel: $channel",
            commercialUsePermitted = false,
            streamingPermitted = false, // Must open official embedded player, never Media3
            territoryRestrictions = territoryNote,
            isVideoEmbedOnly = true,
            artistId = snippet?.channelId?.takeIf { it.isNotBlank() }?.let { "yt_ch_$it" },
            releaseDate = snippet?.publishedAt,
            licenseUrl = watchUrl
        )
    }

    private suspend inline fun <T> executeQuery(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (http: HttpException) {
            val msg = when (http.code()) {
                403, 429 -> "YouTube API quota or access restriction (HTTP ${http.code()})."
                400 -> "YouTube API key or request was rejected (HTTP 400)."
                else -> "YouTube API unavailable (HTTP ${http.code()})."
            }
            Result.failure(IllegalStateException(msg))
        } catch (_: IOException) {
            Result.failure(IllegalStateException("Unable to reach YouTube. Check network connection."))
        } catch (ise: IllegalStateException) {
            Result.failure(ise)
        } catch (_: Throwable) {
            // Never leak raw URL or API key query parameter in error messages.
            Result.failure(IllegalStateException("Unable to search YouTube."))
        }

    private fun formatTerritoryRestriction(restriction: YouTubeRegionRestrictionDto?): String? {
        if (restriction == null) return null
        val allowed = restriction.allowed?.takeIf { it.isNotEmpty() }
        val blocked = restriction.blocked?.takeIf { it.isNotEmpty() }
        return when {
            allowed != null -> "Available only in: ${allowed.take(5).joinToString(", ")}" +
                if (allowed.size > 5) " (+${allowed.size - 5})" else ""
            blocked != null -> "Restricted in: ${blocked.take(5).joinToString(", ")}" +
                if (blocked.size > 5) " (+${blocked.size - 5})" else ""
            else -> null
        }
    }

    private fun decodeBasicEntities(raw: String): String =
        raw.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

    class YouTubeUnconfiguredException(message: String) : IllegalStateException(message)

    companion object {
        const val PLACEHOLDER_API_KEY = "MY_YOUTUBE_API_KEY"
        const val TRACK_PREFIX = "yt_"

        fun parseIso8601DurationSeconds(isoDuration: String?): Int {
            if (isoDuration.isNullOrBlank()) return 180
            val regex = Regex("^PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?$")
            val match = regex.matchEntire(isoDuration.trim()) ?: return 180
            val hours = match.groupValues[1].toIntOrNull() ?: 0
            val minutes = match.groupValues[2].toIntOrNull() ?: 0
            val seconds = match.groupValues[3].toIntOrNull() ?: 0
            val total = hours * 3600 + minutes * 60 + seconds
            return if (total > 0) total else 180
        }
    }
}

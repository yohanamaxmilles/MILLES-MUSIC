package com.example.provider

import com.example.model.ProviderId
import com.example.model.TrackSource
import com.example.model.TrendMetrics
import com.example.network.AudiusApiFactory
import com.example.network.AudiusApiService
import com.example.network.AudiusPlaylistDto
import com.example.network.AudiusTrackDto
import com.example.network.AudiusUserDto
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

class AudiusMusicProvider(
    private val apiService: AudiusApiService = AudiusApiFactory.create()
) : MusicProvider {

    private val logger = Logger.getLogger("AudiusMusicProvider")

    override val providerId: ProviderId = ProviderId.AUDIUS
    override val displayName: String = "Audius"
    override val isEnabled: Boolean = true

    suspend fun fetchTrendingTracks(
        limit: Int = 20,
        offset: Int = 0,
        time: String = "week",
        genre: String? = null
    ): Result<List<UnifiedTrack>> =
        executeQuery {
            val response = apiService.getTrendingTracks(
                time = time,
                limit = limit,
                offset = offset,
                genre = genre
            )
            val chartLabel = when (time) {
                "month" -> "Audius Monthly Chart"
                "allTime" -> "Audius All-Time Chart"
                else -> "Audius Trending"
            }
            response.data.mapIndexedNotNull { index, dto ->
                dto.toUnifiedTrackOrNull(
                    isTrending = true,
                    chartRank = offset + index + 1,
                    chartLabel = chartLabel
                )
            }
        }

    suspend fun fetchNewReleases(limit: Int = 16, offset: Int = 0): Result<List<UnifiedTrack>> =
        executeQuery {
            val response = apiService.getTrendingTracks(
                time = "week",
                limit = (limit * 2).coerceAtMost(40),
                offset = offset
            )
            response.data
                .mapNotNull { dto -> dto.toUnifiedTrackOrNull(isNewRelease = true) }
                .sortedByDescending { it.releaseDate.orEmpty() }
                .take(limit)
        }

    suspend fun fetchTracksByGenre(genre: String, limit: Int = 20, offset: Int = 0): Result<List<UnifiedTrack>> =
        executeQuery {
            val audiusGenre = mapAudiusGenre(genre)
            val trending = runCatching {
                apiService.getTrendingTracks(
                    time = "week",
                    limit = limit,
                    offset = offset,
                    genre = audiusGenre
                ).data.mapNotNull { it.toUnifiedTrackOrNull() }
            }.getOrDefault(emptyList())

            if (trending.isNotEmpty()) {
                trending
            } else {
                apiService.searchTracks(query = genre, limit = limit, offset = offset)
                    .data.mapNotNull { it.toUnifiedTrackOrNull() }
            }
        }

    suspend fun searchArtists(query: String, limit: Int = 10): Result<List<UnifiedArtist>> {
        val clean = query.trim()
        if (clean.isEmpty()) return Result.success(emptyList())
        return executeQuery {
            val response = apiService.searchUsers(query = clean, limit = limit)
            response.data
                .filter { it.isAvailable != false && it.isDeactivated != true }
                .map { it.toUnifiedArtist() }
        }
    }

    override suspend fun searchTracks(query: String, limit: Int): Result<List<UnifiedTrack>> =
        searchTracks(query = query, limit = limit, offset = 0)

    suspend fun searchTracks(query: String, limit: Int, offset: Int): Result<List<UnifiedTrack>> {
        val clean = query.trim()
        if (clean.isEmpty()) return Result.success(emptyList())
        return executeQuery {
            val response = apiService.searchTracks(query = clean, limit = limit, offset = offset)
            response.data.mapNotNull { it.toUnifiedTrackOrNull() }
        }
    }

    override suspend fun getTrackDetails(trackId: String): Result<UnifiedTrack> {
        val rawId = trackId.removePrefix(TRACK_PREFIX)
        return executeQuery {
            val dto = apiService.getTrack(trackId = rawId).data
                ?: throw IllegalStateException("Track unavailable on Audius.")
            dto.toUnifiedTrackOrNull()
                ?: throw IllegalStateException("This Audius track is not streamable or is unavailable.")
        }
    }

    override suspend fun getArtistDetails(artistId: String): Result<Pair<UnifiedArtist?, List<UnifiedTrack>>> {
        val rawId = artistId.removePrefix(ARTIST_PREFIX)
        return executeQuery {
            val user = apiService.getUser(userId = rawId).data
            val tracks = apiService.getUserTracks(userId = rawId).data
                .mapNotNull { it.toUnifiedTrackOrNull() }
            user?.toUnifiedArtist() to tracks
        }
    }

    override suspend fun getAlbumDetails(albumId: String): Result<Pair<UnifiedAlbum?, List<UnifiedTrack>>> {
        val rawId = albumId.removePrefix(PLAYLIST_PREFIX).removePrefix(ALBUM_PREFIX)
        return executeQuery {
            val playlist = apiService.getPlaylist(playlistId = rawId).data
            val tracks = apiService.getPlaylistTracks(playlistId = rawId).data
                .mapNotNull { it.toUnifiedTrackOrNull() }
            val album = playlist?.let {
                UnifiedAlbum(
                    id = "$ALBUM_PREFIX${it.id}",
                    providerId = ProviderId.AUDIUS,
                    providerAlbumId = it.id,
                    title = it.playlistName.ifBlank { "Audius Collection" },
                    artist = it.user?.name?.ifBlank { it.user.handle } ?: "Audius Creator",
                    artistId = it.user?.id?.let { uid -> "$ARTIST_PREFIX$uid" },
                    releaseDate = it.releaseDate,
                    genre = "Audius",
                    artworkUrl = it.artwork?.bestUrl(),
                    originalProviderUrl = buildPlaylistPermalink(it),
                    trackIds = tracks.map { t -> t.trackId }
                )
            }
            album to tracks
        }
    }

    override suspend fun getPlaylists(query: String?, limit: Int): Result<List<UnifiedPlaylist>> =
        executeQuery {
            val response = apiService.getTrendingPlaylists(limit = limit)
            response.data
                .filter { it.isDelete != true && it.isPrivate != true && it.isStreamGated != true }
                .let { list ->
                    val q = query?.trim().orEmpty()
                    if (q.isEmpty()) list else list.filter {
                        it.playlistName.contains(q, ignoreCase = true) ||
                            (it.user?.name?.contains(q, ignoreCase = true) == true)
                    }
                }
                .map { it.toUnifiedPlaylist() }
        }

    override suspend fun getArtworkUrl(trackId: String): Result<String?> =
        getTrackDetails(trackId).map { it.artworkUrl }

    override suspend fun getAuthorizedStreamUrl(trackId: String): Result<String?> =
        getTrackDetails(trackId).mapCatching { track ->
            if (!track.streamingPermitted || track.streamUrl.isNullOrBlank()) {
                throw IllegalStateException("Streaming is restricted or unavailable for this Audius track.")
            }
            track.streamUrl
        }

    private suspend inline fun <T> executeQuery(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (http: HttpException) {
            logger.log(Level.WARNING, "Audius HTTP error: ${http.code()}")
            val msg = when (http.code()) {
                429 -> "Audius rate limit reached. Please retry in a moment."
                403, 404, 410 -> "Requested content is unavailable on Audius."
                else -> "Audius service error (HTTP ${http.code()})."
            }
            Result.failure(AudiusApiException(msg, isRateLimited = http.code() == 429))
        } catch (io: IOException) {
            logger.log(Level.WARNING, "Audius network error: ${io.javaClass.simpleName}")
            Result.failure(AudiusApiException("Unable to reach Audius. Check network connection."))
        } catch (ise: IllegalStateException) {
            logger.log(Level.WARNING, "Audius state error: ${ise.message}")
            Result.failure(ise)
        } catch (err: Throwable) {
            logger.log(Level.WARNING, "Audius unexpected error: ${err.javaClass.simpleName}")
            Result.failure(AudiusApiException("Unable to load data from Audius."))
        }

    private fun AudiusTrackDto.toUnifiedTrackOrNull(
        isTrending: Boolean = false,
        isNewRelease: Boolean = false,
        chartRank: Int? = null,
        chartLabel: String? = null
    ): UnifiedTrack? {
        if (isDelete == true || isAvailable == false || isUnlisted == true) return null
        val permitted = isStreamable != false && isStreamGated != true && (access?.stream != false)
        val authorizedStream = if (permitted) {
            AudiusApiService.streamEndpointUrl(id).takeIf { id.isNotBlank() }
                ?: stream?.url?.takeIf { it.isNotBlank() }
        } else null

        val artistDisplay = user?.name?.takeIf { it.isNotBlank() }
            ?: user?.handle?.takeIf { it.isNotBlank() }
            ?: "Audius Artist"
        val handlePart = user?.handle?.takeIf { it.isNotBlank() }?.let { " (@$it)" }.orEmpty()

        val rawLicense = license?.trim()?.takeIf { it.isNotBlank() }
        val normalizedLicense = rawLicense?.lowercase().orEmpty()
        val commercialAllowed = rawLicense != null && (
            normalizedLicense.contains("cc0") ||
                normalizedLicense.contains("public domain") ||
                (normalizedLicense.contains("cc by") && !normalizedLicense.contains("nc") && !normalizedLicense.contains("noncommercial"))
            )

        val licenseLabel = rawLicense ?: "Audius Creator Stream · Non-commercial unless licensed"
        val attributionText = "Audius · $artistDisplay$handlePart · Attribution required"
        val sourceUrl = permalink?.takeIf { it.isNotBlank() }?.let { path ->
            if (path.startsWith("http")) path else "https://audius.co$path"
        } ?: "https://audius.co"

        val cleanIsrc = isrc?.trim()?.takeIf { it.isNotBlank() }
        val cleanAlbum = genre?.takeIf { it.isNotBlank() } ?: "Audius Single"
        val cleanDuration = duration.coerceAtLeast(1)

        val hasMetrics = chartRank != null || playCount != null || favoriteCount != null || repostCount != null
        val metrics = if (hasMetrics) {
            TrendMetrics(
                providerRank = chartRank,
                listensOrPlays = playCount,
                downloads = null,
                favorites = favoriteCount,
                reposts = repostCount,
                playlistAdds = null,
                sourceChartLabel = chartLabel
            )
        } else null

        val source = TrackSource(
            providerId = ProviderId.AUDIUS,
            providerTrackId = id,
            streamUrl = authorizedStream,
            originalProviderUrl = sourceUrl,
            licenseType = licenseLabel,
            attributionInfo = attributionText,
            licenseUrl = sourceUrl,
            commercialUsePermitted = commercialAllowed,
            streamingPermitted = permitted && !authorizedStream.isNullOrBlank(),
            territoryRestrictions = null,
            isVideoEmbedOnly = false,
            isrc = cleanIsrc,
            durationSeconds = cleanDuration,
            album = cleanAlbum
        )

        return UnifiedTrack(
            trackId = "$TRACK_PREFIX$id",
            providerId = ProviderId.AUDIUS,
            providerTrackId = id,
            title = title.ifBlank { "Untitled Track" },
            artist = artistDisplay,
            album = cleanAlbum,
            genre = genre?.takeIf { it.isNotBlank() } ?: "Electronic",
            durationSeconds = cleanDuration,
            artworkUrl = artwork?.bestUrl() ?: user?.profilePicture?.bestUrl(),
            streamUrl = authorizedStream,
            originalProviderUrl = sourceUrl,
            licenseType = licenseLabel,
            attributionInfo = attributionText,
            commercialUsePermitted = commercialAllowed,
            streamingPermitted = permitted && !authorizedStream.isNullOrBlank(),
            territoryRestrictions = null,
            isVideoEmbedOnly = false,
            artistId = user?.id?.takeIf { it.isNotBlank() }?.let { "$ARTIST_PREFIX$it" },
            albumId = null,
            releaseDate = releaseDate ?: createdAt,
            licenseUrl = sourceUrl,
            isTrending = isTrending,
            isNewRelease = isNewRelease,
            isrc = cleanIsrc,
            sources = listOf(source),
            trendMetrics = metrics
        )
    }

    private fun mapAudiusGenre(genre: String): String = when (genre.trim().lowercase()) {
        "hip hop", "hiphop", "hip-hop" -> "Hip-Hop/Rap"
        "r&b", "rnb", "r&b/soul" -> "R&B/Soul"
        "lo-fi", "lofi" -> "Lo-Fi"
        else -> genre.trim()
    }

    private fun AudiusUserDto.toUnifiedArtist(): UnifiedArtist {
        val display = name.ifBlank { handle.ifBlank { "Audius Artist" } }
        val profileUrl = handle.takeIf { it.isNotBlank() }?.let { "https://audius.co/$it" } ?: "https://audius.co"
        val subtitleText = buildString {
            append("Audius")
            if (handle.isNotBlank()) append(" · @$handle")
            if (followerCount > 0) append(" · $followerCount followers")
        }
        return UnifiedArtist(
            id = "$ARTIST_PREFIX$id",
            providerId = ProviderId.AUDIUS,
            providerArtistId = id,
            name = display,
            genre = "Audius Artist",
            subtitle = subtitleText,
            artworkUrl = profilePicture?.bestUrl(),
            website = website?.takeIf { it.isNotBlank() },
            originalProviderUrl = profileUrl
        )
    }

    private fun AudiusPlaylistDto.toUnifiedPlaylist(): UnifiedPlaylist = UnifiedPlaylist(
        id = "$PLAYLIST_PREFIX$id",
        providerId = ProviderId.AUDIUS,
        providerPlaylistId = id,
        title = playlistName.ifBlank { "Audius Playlist" },
        subtitle = "Audius · ${user?.name?.ifBlank { user.handle } ?: "Creator Playlist"}",
        artworkUrl = artwork?.bestUrl(),
        originalProviderUrl = buildPlaylistPermalink(this)
    )

    private fun buildPlaylistPermalink(dto: AudiusPlaylistDto): String {
        val rawPermalink = dto.permalink?.takeIf { it.isNotBlank() }
        if (rawPermalink != null) {
            return if (rawPermalink.startsWith("http")) rawPermalink else "https://audius.co$rawPermalink"
        }
        val handle = dto.user?.handle?.takeIf { it.isNotBlank() }
        val slug = dto.slug?.takeIf { it.isNotBlank() }
        return if (handle != null && slug != null) {
            "https://audius.co/$handle/playlist/$slug"
        } else {
            "https://audius.co"
        }
    }

    class AudiusApiException(
        message: String,
        val isRateLimited: Boolean = false
    ) : IllegalStateException(message)

    companion object {
        const val TRACK_PREFIX = "aud_"
        const val ARTIST_PREFIX = "aud_art_"
        const val ALBUM_PREFIX = "aud_alb_"
        const val PLAYLIST_PREFIX = "aud_pl_"
    }
}

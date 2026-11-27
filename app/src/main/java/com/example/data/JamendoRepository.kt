package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.Track
import com.example.model.TrackSource
import com.example.model.TrendMetrics
import com.example.network.JamendoAlbumDto
import com.example.network.JamendoApiFactory
import com.example.network.JamendoApiService
import com.example.network.JamendoArtistDto
import com.example.network.JamendoHeadersDto
import com.example.network.JamendoTrackDto
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

class JamendoRepository(
    private val apiService: JamendoApiService = JamendoApiFactory.create(),
    clientId: String = BuildConfig.JAMENDO_CLIENT_ID
) {
    private val logger = Logger.getLogger("JamendoRepository")
    private val normalizedClientId: String = normalizeCredential(clientId)

    val isConfigured: Boolean
        get() = normalizedClientId.isNotBlank() &&
            normalizedClientId != PLACEHOLDER_CLIENT_ID &&
            !normalizedClientId.equals("null", ignoreCase = true)

    suspend fun fetchTrendingCharts(limit: Int = 20, offset: Int = 0): Result<List<Track>> =
        executeQuery {
            val buzzTracks = try {
                val buzzResponse = apiService.getTracks(
                    clientId = normalizedClientId,
                    limit = limit,
                    offset = offset.takeIf { it > 0 },
                    order = "buzzrate"
                )
                validateHeaders(buzzResponse.headers)
                buzzResponse.results.mapIndexedNotNull { index, dto ->
                    dto.toDomainTrack(
                        index = index,
                        isTrending = true,
                        chartRank = offset + index + 1,
                        chartLabel = "Jamendo Buzz"
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                emptyList()
            }

            if (buzzTracks.isNotEmpty() || offset > 0) {
                buzzTracks
            } else {
                val weekResponse = apiService.getTracks(
                    clientId = normalizedClientId,
                    limit = limit,
                    order = "popularity_week"
                )
                validateHeaders(weekResponse.headers)
                weekResponse.results.mapIndexedNotNull { index, dto ->
                    dto.toDomainTrack(
                        index = index,
                        isTrending = true,
                        chartRank = index + 1,
                        chartLabel = "Jamendo Weekly Chart"
                    )
                }
            }
        }

    suspend fun fetchPopularTracks(limit: Int = 20, offset: Int = 0): Result<List<Track>> =
        executeQuery {
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                order = "popularity_week"
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(
                    index = index,
                    isTrending = true,
                    chartRank = offset + index + 1,
                    chartLabel = "Jamendo Weekly Chart"
                )
            }
        }

    suspend fun fetchRecommendedTracks(
        genreTag: String? = null,
        limit: Int = 15,
        offset: Int = 0
    ): Result<List<Track>> =
        executeQuery {
            val cleanTag = genreTag?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                order = "popularity_month",
                tags = cleanTag,
                featured = if (cleanTag == null) 1 else null
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }

    suspend fun fetchNewReleases(limit: Int = 16, offset: Int = 0): Result<List<Track>> =
        executeQuery {
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                order = "releasedate_desc"
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index, isNewRelease = true)
            }
        }

    suspend fun searchTracks(query: String, limit: Int = 25, offset: Int = 0): Result<List<Track>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return Result.success(emptyList())
        return executeQuery {
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                search = cleanQuery
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }
    }

    suspend fun fetchTracksByGenre(genreTag: String, limit: Int = 20, offset: Int = 0): Result<List<Track>> {
        val normalizedTag = genreTag.trim().lowercase().replace("&", "").replace(" ", "")
        return executeQuery {
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                order = "popularity_week",
                tags = normalizedTag
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }
    }

    suspend fun fetchTracksByArtist(artistId: String, limit: Int = 25): Result<List<Track>> =
        executeQuery {
            val rawId = artistId.removePrefix("jam_art_")
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                order = "popularity_total",
                artistId = rawId
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }

    suspend fun fetchTracksByAlbum(albumId: String, limit: Int = 30): Result<List<Track>> =
        executeQuery {
            val rawId = albumId.removePrefix("jam_alb_")
            val response = apiService.getTracks(
                clientId = normalizedClientId,
                limit = limit,
                albumId = rawId
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }

    suspend fun fetchArtists(
        limit: Int = 20,
        offset: Int = 0,
        nameSearch: String? = null
    ): Result<List<Artist>> =
        executeQuery {
            val response = apiService.getArtists(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                order = if (nameSearch.isNullOrBlank()) "popularity_week" else null,
                nameSearch = nameSearch?.trim()?.takeIf { it.isNotEmpty() }
            )
            validateHeaders(response.headers)
            response.results.mapIndexed { index, dto ->
                dto.toDomainArtist(index)
            }
        }

    suspend fun fetchAlbums(
        limit: Int = 20,
        offset: Int = 0,
        order: String = "popularity_week",
        nameSearch: String? = null
    ): Result<List<Album>> =
        executeQuery {
            val response = apiService.getAlbums(
                clientId = normalizedClientId,
                limit = limit,
                offset = offset.takeIf { it > 0 },
                order = if (nameSearch.isNullOrBlank()) order else null,
                nameSearch = nameSearch?.trim()?.takeIf { it.isNotEmpty() }
            )
            validateHeaders(response.headers)
            response.results.mapIndexed { index, dto ->
                dto.toDomainAlbum(index)
            }
        }

    private fun validateHeaders(headers: JamendoHeadersDto?) {
        if (headers != null && headers.status.equals("failed", ignoreCase = true)) {
            val code = headers.code ?: -1
            if (code == 5) {
                throw IllegalStateException("Jamendo rejected the configured Client ID (error code 5).")
            }
            throw IllegalStateException("Jamendo catalog returned error code $code.")
        }
    }

    private suspend inline fun <T> executeQuery(block: () -> T): Result<T> {
        if (!isConfigured) {
            return Result.failure(IllegalStateException("Jamendo Client ID is not configured."))
        }
        return try {
            Result.success(block())
        } catch (ce: CancellationException) {
            throw ce
        } catch (http: HttpException) {
            logger.log(Level.WARNING, "Jamendo HTTP error: ${http.code()}")
            Result.failure(IllegalStateException("Jamendo catalog unavailable (HTTP ${http.code()})."))
        } catch (io: IOException) {
            logger.log(Level.WARNING, "Jamendo network error: ${io.javaClass.simpleName}")
            Result.failure(IllegalStateException("Unable to reach Jamendo. Check network connection."))
        } catch (ise: IllegalStateException) {
            val safeMsg = ise.message
                ?.takeIf { normalizedClientId.isBlank() || !it.contains(normalizedClientId) }
                ?: "Unable to load data from Jamendo."
            logger.log(Level.WARNING, "Jamendo state error: $safeMsg")
            Result.failure(IllegalStateException(safeMsg))
        } catch (err: Throwable) {
            // Never propagate raw exception details or request URLs containing client_id.
            logger.log(Level.WARNING, "Jamendo unexpected error: ${err.javaClass.simpleName}")
            Result.failure(IllegalStateException("Unable to load data from Jamendo."))
        }
    }

    private fun JamendoTrackDto.toDomainTrack(
        index: Int,
        isTrending: Boolean = false,
        isNewRelease: Boolean = false,
        chartRank: Int? = null,
        chartLabel: String? = null
    ): Track? {
        val stream = audio?.takeIf { it.isNotBlank() } ?: return null
        val primaryGenre = musicinfo?.tags?.genres
            ?.firstOrNull { it.isNotBlank() }
            ?.replaceFirstChar { it.uppercase() }
            ?: "Jamendo"

        val fallbackArtRes = when (index % 4) {
            0 -> R.drawable.img_cover_midnight
            1 -> R.drawable.img_cover_afro
            2 -> R.drawable.img_cover_chill
            else -> R.drawable.img_hero_music
        }

        val resolvedCover = albumImage?.takeIf { it.isNotBlank() }
            ?: image?.takeIf { it.isNotBlank() }

        val licenseInfo = parseLicenseAttribution(licenseCcUrl)
        val cleanIsrc = isrc?.trim()?.takeIf { it.isNotBlank() }
        val cleanAlbum = albumName.ifBlank { "Single" }
        val cleanShareUrl = shareUrl?.takeIf { it.isNotBlank() }
        val cleanLicenseUrl = licenseCcUrl?.takeIf { it.isNotBlank() }
        val cleanDuration = duration.coerceAtLeast(1)

        val hasRealStats = stats != null && (
            stats.rateListenedTotal != null ||
                stats.rateDownloadsTotal != null ||
                stats.favorited != null ||
                stats.playlisted != null
            )
        val metrics = if (isTrending || chartRank != null || hasRealStats) {
            TrendMetrics(
                providerRank = chartRank,
                listensOrPlays = stats?.rateListenedTotal,
                downloads = stats?.rateDownloadsTotal,
                favorites = stats?.favorited ?: stats?.likes,
                reposts = null,
                playlistAdds = stats?.playlisted,
                sourceChartLabel = chartLabel
            )
        } else null

        val source = TrackSource(
            providerId = com.example.model.ProviderId.JAMENDO,
            providerTrackId = id,
            streamUrl = stream,
            originalProviderUrl = cleanShareUrl,
            licenseType = licenseInfo.label,
            attributionInfo = licenseInfo.label,
            licenseUrl = cleanLicenseUrl,
            commercialUsePermitted = licenseInfo.isCommercialAllowed,
            streamingPermitted = true,
            territoryRestrictions = null,
            isVideoEmbedOnly = false,
            isrc = cleanIsrc,
            durationSeconds = cleanDuration,
            album = cleanAlbum
        )

        return Track(
            id = "jam_$id",
            title = name.ifBlank { "Untitled Track" },
            artist = artistName.ifBlank { "Independent Artist" },
            album = cleanAlbum,
            genre = primaryGenre,
            durationSeconds = cleanDuration,
            artwork = ArtworkDescriptor(
                imageRes = fallbackArtRes,
                primaryColor = Color(0xFF242B1E),
                secondaryColor = Color(0xFF151611),
                accentColor = if (index % 2 == 0) Color(0xFFC6F24E) else Color(0xFF8B6CF0)
            ),
            coverUrl = resolvedCover,
            streamUrl = stream,
            artistId = artistId?.takeIf { it.isNotBlank() }?.let { "jam_art_$it" },
            albumId = albumId?.takeIf { it.isNotBlank() }?.let { "jam_alb_$it" },
            releaseDate = releaseDate?.takeIf { it.isNotBlank() },
            licenseUrl = cleanLicenseUrl,
            shareUrl = cleanShareUrl,
            attributionLabel = licenseInfo.label,
            isCommercialLicensed = licenseInfo.isCommercialAllowed,
            isTrending = isTrending,
            isNewRelease = isNewRelease,
            isUpbeat = index % 2 == 0,
            providerId = com.example.model.ProviderId.JAMENDO,
            providerTrackId = id,
            originalProviderUrl = cleanShareUrl,
            licenseType = licenseInfo.label,
            streamingPermitted = true,
            isVideoEmbedOnly = false,
            isrc = cleanIsrc,
            sources = listOf(source),
            trendMetrics = metrics
        )
    }

    private fun JamendoArtistDto.toDomainArtist(index: Int): Artist {
        val fallbackArtRes = when (index % 3) {
            0 -> R.drawable.img_cover_midnight
            1 -> R.drawable.img_cover_afro
            else -> R.drawable.img_cover_chill
        }
        val subtitleText = joinDate?.takeIf { it.isNotBlank() }
            ?.let { "On Jamendo since ${it.take(4)}" }
            ?: "Jamendo Independent Artist"
        return Artist(
            id = "jam_art_$id",
            name = name.ifBlank { "Independent Artist" },
            genre = "Jamendo Artist",
            subtitle = subtitleText,
            artwork = ArtworkDescriptor(imageRes = fallbackArtRes),
            imageUrl = image?.takeIf { it.isNotBlank() },
            joinDate = joinDate?.takeIf { it.isNotBlank() },
            website = website?.takeIf { it.isNotBlank() },
            shareUrl = shareUrl?.takeIf { it.isNotBlank() }
        )
    }

    private fun JamendoAlbumDto.toDomainAlbum(index: Int): Album {
        val fallbackArtRes = when (index % 3) {
            0 -> R.drawable.img_cover_chill
            1 -> R.drawable.img_cover_afro
            else -> R.drawable.img_cover_midnight
        }
        return Album(
            id = "jam_alb_$id",
            title = name.ifBlank { "Untitled Album" },
            artist = artistName.ifBlank { "Independent Artist" },
            artistId = artistId?.takeIf { it.isNotBlank() }?.let { "jam_art_$it" },
            releaseDate = releaseDate?.takeIf { it.isNotBlank() },
            genre = "Jamendo Album",
            trackIds = emptyList(),
            artwork = ArtworkDescriptor(imageRes = fallbackArtRes),
            coverUrl = image?.takeIf { it.isNotBlank() },
            shareUrl = shareUrl?.takeIf { it.isNotBlank() }
        )
    }

    private data class LicenseInfo(
        val label: String,
        val isCommercialAllowed: Boolean
    )

    private fun parseLicenseAttribution(licenseCcUrl: String?): LicenseInfo {
        val url = licenseCcUrl?.lowercase().orEmpty()
        if (url.isBlank()) {
            return LicenseInfo(
                label = "Jamendo Personal Stream · Verify license before commercial use",
                isCommercialAllowed = false
            )
        }
        val licenseCode = when {
            "by-nc-nd" in url -> "CC BY-NC-ND"
            "by-nc-sa" in url -> "CC BY-NC-SA"
            "by-nc" in url -> "CC BY-NC"
            "by-nd" in url -> "CC BY-ND"
            "by-sa" in url -> "CC BY-SA"
            "/by/" in url -> "CC BY"
            "publicdomain" in url || "/zero/" in url -> "CC0 / Public Domain"
            else -> "Creative Commons"
        }
        val nonCommercial = "-nc" in url || licenseCode.contains("NC")
        val suffix = if (nonCommercial) "Non-commercial streaming only · Attribution required"
        else "Attribution required"
        return LicenseInfo(
            label = "$licenseCode · $suffix",
            isCommercialAllowed = !nonCommercial
        )
    }

    companion object {
        const val PLACEHOLDER_CLIENT_ID = "MY_JAMENDO_CLIENT_ID"

        fun normalizeCredential(raw: String?): String {
            val trimmed = raw?.trim().orEmpty()
            return if (trimmed.length >= 2 &&
                ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                    (trimmed.startsWith("'") && trimmed.endsWith("'")))
            ) {
                trimmed.substring(1, trimmed.length - 1).trim()
            } else {
                trimmed
            }
        }
    }
}

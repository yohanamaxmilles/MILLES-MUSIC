package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import com.example.R
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.Track
import com.example.network.JamendoAlbumDto
import com.example.network.JamendoApiFactory
import com.example.network.JamendoApiService
import com.example.network.JamendoArtistDto
import com.example.network.JamendoHeadersDto
import com.example.network.JamendoTrackDto
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

class JamendoRepository(
    private val apiService: JamendoApiService = JamendoApiFactory.create(),
    private val clientId: String = BuildConfig.JAMENDO_CLIENT_ID
) {
    val isConfigured: Boolean
        get() = clientId.isNotBlank() && clientId != PLACEHOLDER_CLIENT_ID

    suspend fun fetchPopularTracks(limit: Int = 20): Result<List<Track>> =
        executeQuery {
            val response = apiService.getTracks(
                clientId = clientId,
                limit = limit,
                order = "popularity_week"
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index, isTrending = true)
            }
        }

    suspend fun fetchRecommendedTracks(genreTag: String? = null, limit: Int = 15): Result<List<Track>> =
        executeQuery {
            val cleanTag = genreTag?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            val response = apiService.getTracks(
                clientId = clientId,
                limit = limit,
                order = "popularity_month",
                tags = cleanTag,
                featured = if (cleanTag == null) 1 else null
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }

    suspend fun fetchNewReleases(limit: Int = 16): Result<List<Track>> =
        executeQuery {
            val response = apiService.getTracks(
                clientId = clientId,
                limit = limit,
                order = "releasedate_desc"
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index, isNewRelease = true)
            }
        }

    suspend fun searchTracks(query: String, limit: Int = 25): Result<List<Track>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return Result.success(emptyList())
        return executeQuery {
            val response = apiService.getTracks(
                clientId = clientId,
                limit = limit,
                search = cleanQuery
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }
    }

    suspend fun fetchTracksByGenre(genreTag: String, limit: Int = 20): Result<List<Track>> {
        val normalizedTag = genreTag.trim().lowercase().replace("&", "").replace(" ", "")
        return executeQuery {
            val response = apiService.getTracks(
                clientId = clientId,
                limit = limit,
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
                clientId = clientId,
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
                clientId = clientId,
                limit = limit,
                albumId = rawId
            )
            validateHeaders(response.headers)
            response.results.mapIndexedNotNull { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }

    suspend fun fetchArtists(limit: Int = 20, nameSearch: String? = null): Result<List<Artist>> =
        executeQuery {
            val response = apiService.getArtists(
                clientId = clientId,
                limit = limit,
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
        order: String = "popularity_week",
        nameSearch: String? = null
    ): Result<List<Album>> =
        executeQuery {
            val response = apiService.getAlbums(
                clientId = clientId,
                limit = limit,
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
            throw IllegalStateException("Jamendo catalog returned error code ${headers.code ?: -1}.")
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
            Result.failure(IllegalStateException("Jamendo catalog unavailable (HTTP ${http.code()})."))
        } catch (_: IOException) {
            Result.failure(IllegalStateException("Unable to reach Jamendo. Check network connection."))
        } catch (ise: IllegalStateException) {
            Result.failure(ise)
        } catch (_: Throwable) {
            // Never propagate raw exception details or request URLs containing client_id.
            Result.failure(IllegalStateException("Unable to load data from Jamendo."))
        }
    }

    private fun JamendoTrackDto.toDomainTrack(
        index: Int,
        isTrending: Boolean = false,
        isNewRelease: Boolean = false
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

        return Track(
            id = "jam_$id",
            title = name.ifBlank { "Untitled Track" },
            artist = artistName.ifBlank { "Independent Artist" },
            album = albumName.ifBlank { "Single" },
            genre = primaryGenre,
            durationSeconds = duration.coerceAtLeast(1),
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
            licenseUrl = licenseCcUrl?.takeIf { it.isNotBlank() },
            shareUrl = shareUrl?.takeIf { it.isNotBlank() },
            attributionLabel = licenseInfo.label,
            isCommercialLicensed = licenseInfo.isCommercialAllowed,
            isTrending = isTrending,
            isNewRelease = isNewRelease,
            isUpbeat = index % 2 == 0
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
    }
}

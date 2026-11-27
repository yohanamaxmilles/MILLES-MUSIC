package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.BuildConfig
import com.example.R
import com.example.model.ArtworkDescriptor
import com.example.model.Track
import com.example.network.JamendoApiFactory
import com.example.network.JamendoApiService
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

    suspend fun fetchTrendingTracks(limit: Int = 16): Result<List<Track>> =
        executeQuery {
            apiService.getTracks(
                clientId = clientId,
                limit = limit,
                order = "popularity_week"
            ).results.mapIndexed { index, dto ->
                dto.toDomainTrack(index = index, isTrending = true, isNewRelease = index < 4)
            }
        }

    suspend fun fetchNewReleases(limit: Int = 12): Result<List<Track>> =
        executeQuery {
            apiService.getTracks(
                clientId = clientId,
                limit = limit,
                order = "releasedate_desc"
            ).results.mapIndexed { index, dto ->
                dto.toDomainTrack(index = index, isTrending = false, isNewRelease = true)
            }
        }

    suspend fun searchTracks(query: String, limit: Int = 20): Result<List<Track>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return Result.success(emptyList())
        return executeQuery {
            apiService.getTracks(
                clientId = clientId,
                limit = limit,
                search = cleanQuery
            ).results.mapIndexed { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }
    }

    suspend fun fetchTracksByGenre(genreTag: String, limit: Int = 15): Result<List<Track>> {
        val normalizedTag = genreTag.trim().lowercase().replace("&", "").replace(" ", "")
        return executeQuery {
            apiService.getTracks(
                clientId = clientId,
                limit = limit,
                order = "popularity_total",
                tags = normalizedTag
            ).results.mapIndexed { index, dto ->
                dto.toDomainTrack(index = index)
            }
        }
    }

    private suspend inline fun executeQuery(block: () -> List<Track>): Result<List<Track>> {
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
        } catch (_: Throwable) {
            // Never propagate raw exception details or request URLs containing client_id.
            Result.failure(IllegalStateException("Unable to load tracks from Jamendo."))
        }
    }

    private fun JamendoTrackDto.toDomainTrack(
        index: Int,
        isTrending: Boolean = false,
        isNewRelease: Boolean = false
    ): Track {
        val primaryGenre = musicinfo?.tags?.genres
            ?.firstOrNull { it.isNotBlank() }
            ?.replaceFirstChar { it.uppercase() }
            ?: "Electronic"

        val fallbackArtRes = when (index % 4) {
            0 -> R.drawable.img_cover_midnight
            1 -> R.drawable.img_cover_afro
            2 -> R.drawable.img_cover_chill
            else -> R.drawable.img_hero_music
        }

        val resolvedCover = albumImage?.takeIf { it.isNotBlank() }
            ?: image?.takeIf { it.isNotBlank() }

        return Track(
            id = "jam_$id",
            title = name.ifBlank { "Untitled Track" },
            artist = artistName.ifBlank { "Independent Artist" },
            album = albumName.ifBlank { "Jamendo Single" },
            genre = primaryGenre,
            durationSeconds = duration.coerceAtLeast(1),
            artwork = ArtworkDescriptor(
                imageRes = fallbackArtRes,
                primaryColor = Color(0xFF242B1E),
                secondaryColor = Color(0xFF151611),
                accentColor = if (index % 2 == 0) Color(0xFFC6F24E) else Color(0xFF8B6CF0)
            ),
            coverUrl = resolvedCover,
            streamUrl = audio?.takeIf { it.isNotBlank() },
            isTrending = isTrending,
            isNewRelease = isNewRelease,
            isUpbeat = index % 2 == 0
        )
    }

    companion object {
        const val PLACEHOLDER_CLIENT_ID = "MY_JAMENDO_CLIENT_ID"
    }
}

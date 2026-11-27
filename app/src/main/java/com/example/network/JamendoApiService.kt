package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class JamendoTracksResponse(
    val headers: JamendoHeadersDto? = null,
    val results: List<JamendoTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoArtistsResponse(
    val headers: JamendoHeadersDto? = null,
    val results: List<JamendoArtistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoAlbumsResponse(
    val headers: JamendoHeadersDto? = null,
    val results: List<JamendoAlbumDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoHeadersDto(
    val status: String? = null,
    val code: Int? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "results_count") val resultsCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class JamendoTrackStatsDto(
    @Json(name = "rate_listened_total") val rateListenedTotal: Long? = null,
    @Json(name = "rate_downloads_total") val rateDownloadsTotal: Long? = null,
    val playlisted: Long? = null,
    val favorited: Long? = null,
    val likes: Long? = null
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    val id: String,
    val name: String,
    val duration: Int = 0,
    @Json(name = "artist_id") val artistId: String? = null,
    @Json(name = "artist_name") val artistName: String = "",
    @Json(name = "album_id") val albumId: String? = null,
    @Json(name = "album_name") val albumName: String = "",
    @Json(name = "releasedate") val releaseDate: String? = null,
    @Json(name = "album_image") val albumImage: String? = null,
    val image: String? = null,
    val audio: String? = null,
    @Json(name = "shareurl") val shareUrl: String? = null,
    @Json(name = "license_ccurl") val licenseCcUrl: String? = null,
    @Json(name = "prourl") val proUrl: String? = null,
    val isrc: String? = null,
    val stats: JamendoTrackStatsDto? = null,
    val musicinfo: JamendoMusicInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class JamendoArtistDto(
    val id: String,
    val name: String,
    val website: String? = null,
    @Json(name = "joindate") val joinDate: String? = null,
    val image: String? = null,
    @Json(name = "shareurl") val shareUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoAlbumDto(
    val id: String,
    val name: String,
    @Json(name = "releasedate") val releaseDate: String? = null,
    @Json(name = "artist_id") val artistId: String? = null,
    @Json(name = "artist_name") val artistName: String = "",
    val image: String? = null,
    @Json(name = "shareurl") val shareUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoMusicInfoDto(
    val tags: JamendoTagsDto? = null
)

@JsonClass(generateAdapter = true)
data class JamendoTagsDto(
    val genres: List<String> = emptyList()
)

interface JamendoApiService {

    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int? = null,
        @Query("audioformat") audioFormat: String = "mp32",
        @Query(value = "include", encoded = true) include: String = "musicinfo+licenses+stats",
        @Query("order") order: String? = null,
        @Query("boost") boost: String? = null,
        @Query("search") search: String? = null,
        @Query("tags") tags: String? = null,
        @Query("featured") featured: Int? = null,
        @Query("artist_id") artistId: String? = null,
        @Query("album_id") albumId: String? = null
    ): JamendoTracksResponse

    @GET("artists/")
    suspend fun getArtists(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int? = null,
        @Query("order") order: String? = "popularity_week",
        @Query("namesearch") nameSearch: String? = null,
        @Query("hasimage") hasImage: Boolean? = true
    ): JamendoArtistsResponse

    @GET("albums/")
    suspend fun getAlbums(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int? = null,
        @Query("order") order: String? = "popularity_week",
        @Query("namesearch") nameSearch: String? = null,
        @Query("artist_id") artistId: String? = null
    ): JamendoAlbumsResponse
}

object JamendoApiFactory {
    private const val BASE_URL = "https://api.jamendo.com/v3.0/"

    fun create(): JamendoApiService {
        // Deliberately omit HTTP URL/body logging so client_id query parameters are never logged.
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder().build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JamendoApiService::class.java)
    }
}

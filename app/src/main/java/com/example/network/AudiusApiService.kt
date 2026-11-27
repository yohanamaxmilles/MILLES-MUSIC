package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class AudiusTracksResponse(
    val data: List<AudiusTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusSingleTrackResponse(
    val data: AudiusTrackDto? = null
)

@JsonClass(generateAdapter = true)
data class AudiusUsersResponse(
    val data: List<AudiusUserDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusSingleUserResponse(
    val data: AudiusUserDto? = null
)

@JsonClass(generateAdapter = true)
data class AudiusPlaylistsResponse(
    val data: List<AudiusPlaylistDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AudiusSinglePlaylistResponse(
    val data: AudiusPlaylistDto? = null
)

@JsonClass(generateAdapter = true)
data class AudiusArtworkDto(
    @Json(name = "150x150") val small: String? = null,
    @Json(name = "480x480") val medium: String? = null,
    @Json(name = "1000x1000") val large: String? = null
) {
    fun bestUrl(): String? =
        medium?.takeIf { it.isNotBlank() }
            ?: large?.takeIf { it.isNotBlank() }
            ?: small?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class AudiusStreamDto(
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusAccessDto(
    val stream: Boolean? = null,
    val download: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AudiusUserDto(
    val id: String,
    val name: String = "",
    val handle: String = "",
    val bio: String? = null,
    @Json(name = "follower_count") val followerCount: Int = 0,
    @Json(name = "track_count") val trackCount: Int = 0,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "is_available") val isAvailable: Boolean? = true,
    @Json(name = "is_deactivated") val isDeactivated: Boolean? = false,
    @Json(name = "profile_picture") val profilePicture: AudiusArtworkDto? = null,
    val website: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusTrackDto(
    val id: String,
    val title: String = "",
    val duration: Int = 0,
    val genre: String? = null,
    val mood: String? = null,
    val permalink: String? = null,
    val license: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "is_available") val isAvailable: Boolean? = true,
    @Json(name = "is_delete") val isDelete: Boolean? = false,
    @Json(name = "is_unlisted") val isUnlisted: Boolean? = false,
    @Json(name = "is_streamable") val isStreamable: Boolean? = true,
    @Json(name = "is_stream_gated") val isStreamGated: Boolean? = false,
    val artwork: AudiusArtworkDto? = null,
    val stream: AudiusStreamDto? = null,
    val access: AudiusAccessDto? = null,
    val user: AudiusUserDto? = null
)

@JsonClass(generateAdapter = true)
data class AudiusPlaylistDto(
    val id: String,
    @Json(name = "playlist_name") val playlistName: String = "",
    val description: String? = null,
    @Json(name = "is_album") val isAlbum: Boolean = false,
    @Json(name = "is_delete") val isDelete: Boolean? = false,
    @Json(name = "is_private") val isPrivate: Boolean? = false,
    @Json(name = "is_stream_gated") val isStreamGated: Boolean? = false,
    @Json(name = "release_date") val releaseDate: String? = null,
    val slug: String? = null,
    val permalink: String? = null,
    val artwork: AudiusArtworkDto? = null,
    val user: AudiusUserDto? = null
)

interface AudiusApiService {

    @GET("tracks/trending")
    suspend fun getTrendingTracks(
        @Query("time") time: String = "week",
        @Query("limit") limit: Int = 20,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusTracksResponse

    @GET("tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusTracksResponse

    @GET("tracks/{trackId}")
    suspend fun getTrack(
        @Path("trackId") trackId: String,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusSingleTrackResponse

    @GET("users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("limit") limit: Int = 10,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusUsersResponse

    @GET("users/{userId}")
    suspend fun getUser(
        @Path("userId") userId: String,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusSingleUserResponse

    @GET("users/{userId}/tracks")
    suspend fun getUserTracks(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 25,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusTracksResponse

    @GET("playlists/trending")
    suspend fun getTrendingPlaylists(
        @Query("time") time: String = "week",
        @Query("limit") limit: Int = 12,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusPlaylistsResponse

    @GET("playlists/{playlistId}")
    suspend fun getPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusSinglePlaylistResponse

    @GET("playlists/{playlistId}/tracks")
    suspend fun getPlaylistTracks(
        @Path("playlistId") playlistId: String,
        @Query("app_name") appName: String = APP_NAME
    ): AudiusTracksResponse

    companion object {
        const val BASE_URL = "https://api.audius.co/v1/"
        const val APP_NAME = "MillesMusic"

        fun streamEndpointUrl(trackId: String): String =
            "${BASE_URL}tracks/$trackId/stream?app_name=$APP_NAME"
    }
}

object AudiusApiFactory {
    fun create(): AudiusApiService {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder().build()

        return Retrofit.Builder()
            .baseUrl(AudiusApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AudiusApiService::class.java)
    }
}

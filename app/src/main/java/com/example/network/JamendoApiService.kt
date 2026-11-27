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
data class JamendoHeadersDto(
    val status: String? = null,
    val code: Int? = null,
    @Json(name = "error_message") val errorMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    val id: String,
    val name: String,
    val duration: Int = 180,
    @Json(name = "artist_name") val artistName: String = "Independent Artist",
    @Json(name = "album_name") val albumName: String = "Jamendo Release",
    @Json(name = "album_image") val albumImage: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val musicinfo: JamendoMusicInfoDto? = null
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
        @Query("audioformat") audioFormat: String = "mp32",
        @Query("include") include: String = "musicinfo",
        @Query("order") order: String? = null,
        @Query("search") search: String? = null,
        @Query("tags") tags: String? = null
    ): JamendoTracksResponse
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

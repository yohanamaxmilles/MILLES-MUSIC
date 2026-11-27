package com.example.network

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItemDto(
    val id: YouTubeVideoIdDto? = null,
    val snippet: YouTubeSnippetDto? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoIdDto(
    val kind: String? = null,
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideosResponse(
    val items: List<YouTubeVideoItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoItemDto(
    val id: String,
    val snippet: YouTubeSnippetDto? = null,
    val contentDetails: YouTubeContentDetailsDto? = null,
    val status: YouTubeVideoStatusDto? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSnippetDto(
    val publishedAt: String? = null,
    val channelId: String? = null,
    val title: String = "",
    val description: String? = null,
    val channelTitle: String = "",
    val thumbnails: YouTubeThumbnailsDto? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailsDto(
    val default: YouTubeThumbnailDto? = null,
    val medium: YouTubeThumbnailDto? = null,
    val high: YouTubeThumbnailDto? = null,
    val standard: YouTubeThumbnailDto? = null,
    val maxres: YouTubeThumbnailDto? = null
) {
    fun bestUrl(): String? =
        high?.url?.takeIf { it.isNotBlank() }
            ?: medium?.url?.takeIf { it.isNotBlank() }
            ?: standard?.url?.takeIf { it.isNotBlank() }
            ?: maxres?.url?.takeIf { it.isNotBlank() }
            ?: default?.url?.takeIf { it.isNotBlank() }
}

@JsonClass(generateAdapter = true)
data class YouTubeThumbnailDto(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeContentDetailsDto(
    val duration: String? = null,
    val regionRestriction: YouTubeRegionRestrictionDto? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeRegionRestrictionDto(
    val allowed: List<String>? = null,
    val blocked: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoStatusDto(
    val embeddable: Boolean? = true,
    val privacyStatus: String? = null
)

interface YouTubeApiService {

    @GET("search")
    suspend fun searchMusicVideos(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoCategoryId") videoCategoryId: String = "10",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("maxResults") maxResults: Int = 12,
        @Query("q") query: String,
        @Query("key") apiKey: String = YouTubeApiFactory.configuredApiKey()
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,contentDetails,status",
        @Query("id") videoIds: String,
        @Query("key") apiKey: String = YouTubeApiFactory.configuredApiKey()
    ): YouTubeVideosResponse

    companion object {
        const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
}

object YouTubeApiFactory {
    const val PLACEHOLDER_API_KEY = "MY_YOUTUBE_API_KEY"

    fun normalizeApiKey(raw: String?): String {
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

    fun configuredApiKey(): String = normalizeApiKey(BuildConfig.YOUTUBE_API_KEY)

    fun isConfigured(apiKey: String = configuredApiKey()): Boolean {
        val normalized = normalizeApiKey(apiKey)
        return normalized.isNotBlank() &&
            normalized != PLACEHOLDER_API_KEY &&
            !normalized.equals("null", ignoreCase = true)
    }

    fun create(): YouTubeApiService {
        // Deliberately omit HTTP URL/body logging so API key query parameters are never logged.
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder().build()

        return Retrofit.Builder()
            .baseUrl(YouTubeApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeApiService::class.java)
    }
}

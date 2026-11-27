package com.example.music

import android.net.Uri
import com.example.model.ArtworkDescriptor
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Jamendo's read-only catalogue adapter.
 * The returned audio URL is the provider's stream field, not a page URL or an extracted URL.
 */
class JamendoMusicSource(
    private val clientId: String
) {
    suspend fun searchTracks(query: String, limit: Int = 30): List<Track> =
        withContext(Dispatchers.IO) {
            require(clientId.isNotBlank()) { "Jamendo Client ID is missing" }
            val requestUrl = Uri.parse("https://api.jamendo.com/v3.0/tracks/")
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("format", "json")
                .appendQueryParameter("limit", limit.coerceIn(1, 50).toString())
                .appendQueryParameter("search", query.trim())
                .appendQueryParameter("audioformat", "mp32")
                .appendQueryParameter("include", "licenses")
                .appendQueryParameter("type", "single albumtrack")
                .build()
                .toString()

            val connection = (URL(requestUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "MILLES-MUSIC-Android")
            }

            try {
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                if (status !in 200..299) {
                    throw IOException("Jamendo request failed (HTTP $status)")
                }

                val root = JSONObject(body)
                val headers = root.optJSONObject("headers")
                if (headers?.optString("status") == "error") {
                    throw IOException(headers.optString("error_message", "Jamendo returned an API error"))
                }

                val results = root.optJSONArray("results") ?: return@withContext emptyList()
                buildList {
                    for (index in 0 until results.length()) {
                        val item = results.optJSONObject(index) ?: continue
                        val audioUrl = item.optString("audio").takeIf { it.startsWith("https://") || it.startsWith("http://") }
                            ?: continue
                        val trackId = item.optString("id").takeIf(String::isNotBlank) ?: continue
                        val title = item.optString("name", "Unknown track")
                        val artist = item.optString("artist_name", "Unknown artist")
                        val image = item.optString("image").takeIf { it.startsWith("https://") || it.startsWith("http://") }
                        val duration = item.optInt("duration", 0).coerceAtLeast(0)
                        add(
                            Track(
                                id = "jamendo_$trackId",
                                title = title,
                                artist = artist,
                                album = item.optString("album_name", "Single"),
                                genre = "Independent",
                                durationSeconds = duration,
                                artwork = ArtworkDescriptor(imageUrl = image),
                                streamUrl = audioUrl,
                                providerName = "Jamendo",
                                providerTrackUrl = item.optString("shareurl").takeIf(String::isNotBlank),
                                licenseUrl = item.optString("license_ccurl").takeIf(String::isNotBlank)
                            )
                        )
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
}

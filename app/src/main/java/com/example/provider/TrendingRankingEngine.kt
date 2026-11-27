package com.example.provider

import com.example.model.ProviderId
import com.example.model.TrendMetrics
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.ln

/**
 * Transparent cross-provider trending ranker for "Trends Now".
 *
 * Combines real provider chart tracks (Jamendo buzzrate/weekly charts + Audius trending)
 * using only genuine returned rank, engagement counters, and release freshness — never
 * fabricating or extrapolating missing statistics.
 */
object TrendingRankingEngine {

    private const val WEIGHT_CHART_RANK = 0.45
    private const val WEIGHT_ENGAGEMENT = 0.35
    private const val WEIGHT_FRESHNESS = 0.20
    private const val MULTI_PROVIDER_CONSENSUS_BONUS = 0.15

    fun rankUnifiedTrending(
        providerBatches: Map<ProviderId, List<UnifiedTrack>>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<UnifiedTrack> {
        val scoredPerProvider = providerBatches.flatMap { (_, tracks) ->
            scoreProviderBatch(tracks, nowMillis)
        }
        if (scoredPerProvider.isEmpty()) return emptyList()

        // Group/deduplicate recordings across providers while combining real trend scores
        val deduplicated = TrackDeduplicator.deduplicate(scoredPerProvider).map { track ->
            val uniqueTrendingProviders = track.resolvedSources().map { it.providerId }.distinct().size
            val consensusBoost = if (uniqueTrendingProviders > 1) MULTI_PROVIDER_CONSENSUS_BONUS else 0.0
            val existingMetrics = track.trendMetrics
            val finalScore = (existingMetrics?.trendScore ?: 0.0) + consensusBoost
            track.copy(
                isTrending = true,
                trendMetrics = existingMetrics?.copy(
                    trendScore = finalScore,
                    sourceChartLabel = existingMetrics.sourceChartLabel
                        ?: track.resolvedSources().joinToString(" · ") { it.providerId.displayName }
                )
            )
        }

        return deduplicated.sortedByDescending { it.trendMetrics?.trendScore ?: 0.0 }
    }

    private fun scoreProviderBatch(
        tracks: List<UnifiedTrack>,
        nowMillis: Long
    ): List<UnifiedTrack> {
        if (tracks.isEmpty()) return emptyList()

        // Compute log-scaled raw engagement per track only from metrics actually returned by the provider
        val rawEngagements = tracks.map { track ->
            val m = track.trendMetrics
            val listensLog = m?.listensOrPlays?.takeIf { it > 0 }?.let { ln(1.0 + it.toDouble()) } ?: 0.0
            val downloadsLog = m?.downloads?.takeIf { it > 0 }?.let { ln(1.0 + it.toDouble()) * 1.25 } ?: 0.0
            val favoritesLog = m?.favorites?.takeIf { it > 0 }?.let { ln(1.0 + it.toDouble()) * 1.15 } ?: 0.0
            val repostsLog = m?.reposts?.takeIf { it > 0 }?.let { ln(1.0 + it.toDouble()) * 1.30 } ?: 0.0
            val playlistLog = m?.playlistAdds?.takeIf { it > 0 }?.let { ln(1.0 + it.toDouble()) * 1.10 } ?: 0.0
            listensLog + downloadsLog + favoritesLog + repostsLog + playlistLog
        }
        val maxEngagement = rawEngagements.maxOrNull()?.takeIf { it > 0.0 } ?: 1.0

        return tracks.mapIndexed { index, track ->
            val existing = track.trendMetrics
            val rank = existing?.providerRank ?: (index + 1)
            val rankScore = 1.0 / (1.0 + 0.12 * (rank - 1).coerceAtLeast(0))
            val engagementNorm = (rawEngagements[index] / maxEngagement).coerceIn(0.0, 1.0)
            val freshness = computeFreshnessScore(track.releaseDate, nowMillis)

            val composite = (WEIGHT_CHART_RANK * rankScore) +
                (WEIGHT_ENGAGEMENT * engagementNorm) +
                (WEIGHT_FRESHNESS * freshness)

            val updatedMetrics = (existing ?: TrendMetrics(providerRank = rank)).copy(
                providerRank = rank,
                trendScore = composite,
                freshnessBoost = freshness
            )

            track.copy(
                isTrending = true,
                trendMetrics = updatedMetrics
            )
        }
    }

    fun computeFreshnessScore(releaseDate: String?, nowMillis: Long): Double {
        val releaseMillis = parseReleaseDateMillis(releaseDate) ?: return 0.0
        val ageDays = ((nowMillis - releaseMillis).coerceAtLeast(0L) / 86_400_000.0)
        return when {
            ageDays <= 7.0 -> 1.0
            ageDays <= 30.0 -> 0.75
            ageDays <= 90.0 -> 0.45
            ageDays <= 365.0 -> 0.20
            else -> 0.05
        }
    }

    private fun parseReleaseDateMillis(raw: String?): Long? {
        val clean = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = true
                }.parse(clean)?.time
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}

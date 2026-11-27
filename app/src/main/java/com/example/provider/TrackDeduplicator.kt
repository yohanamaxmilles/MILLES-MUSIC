package com.example.provider

import com.example.model.TrackSource
import com.example.model.TrendMetrics
import kotlin.math.abs

/**
 * Cross-provider duplicate track detector and source merger.
 *
 * Matches identical recordings across audio providers using:
 * 1. ISRC when available.
 * 2. Normalized base title + version qualifiers + primary artist + duration + album metadata.
 *
 * Strictly prevents false merges across remixes, live recordings, covers, acoustic/instrumental
 * versions, or alternate edits, and preserves all valid [TrackSource] references on the merged track.
 */
object TrackDeduplicator {

    private val VERSION_QUALIFIER_TOKENS = listOf(
        "remix", "rmx", "vip", "rework", "bootleg", "flip",
        "live", "session", "concert",
        "acoustic", "unplugged",
        "cover", "karaoke", "tribute",
        "instrumental", "acapella", "a capella",
        "demo", "remaster", "remastered",
        "sped up", "slowed", "reverb", "nightcore",
        "extended", "club mix", "dub", "radio edit", "alternate", "alt version"
    )

    private val GENERIC_ALBUM_LABELS = setOf(
        "", "single", "audius single", "jamendo", "unknown", "unknown album", "ep"
    )

    data class TitleFingerprint(
        val baseTitle: String,
        val versionQualifiers: Set<String>
    )

    fun deduplicate(tracks: List<UnifiedTrack>): List<UnifiedTrack> {
        if (tracks.size <= 1) return tracks
        val merged = ArrayList<UnifiedTrack>(tracks.size)
        val seenVideoKeys = HashSet<String>()

        for (candidate in tracks) {
            if (candidate.isVideoEmbedOnly) {
                val videoKey = "${candidate.providerId.id}:${candidate.providerTrackId}"
                if (seenVideoKeys.add(videoKey)) {
                    merged += candidate
                }
                continue
            }

            val existingIdx = merged.indexOfFirst { existing ->
                !existing.isVideoEmbedOnly && isSameRecording(existing, candidate)
            }
            if (existingIdx >= 0) {
                merged[existingIdx] = mergeTracks(merged[existingIdx], candidate)
            } else {
                merged += candidate.copy(sources = candidate.resolvedSources())
            }
        }
        return merged
    }

    fun isSameRecording(first: UnifiedTrack, second: UnifiedTrack): Boolean {
        if (first.isVideoEmbedOnly || second.isVideoEmbedOnly) return false

        // Same exact provider + track ID is trivially the same recording
        if (first.providerId == second.providerId && first.providerTrackId == second.providerTrackId) {
            return true
        }

        val fp1 = extractTitleFingerprint(first.title)
        val fp2 = extractTitleFingerprint(second.title)

        // Never merge different remixes, live versions, covers, or alternate editions
        if (fp1.versionQualifiers != fp2.versionQualifiers) {
            return false
        }

        // 1. ISRC match (strongest identifier when exposed by providers)
        val isrc1 = normalizeIsrc(first.isrc)
        val isrc2 = normalizeIsrc(second.isrc)
        if (isrc1 != null && isrc2 != null) {
            if (isrc1 == isrc2) {
                val durationDiff = abs(first.durationSeconds - second.durationSeconds)
                return first.durationSeconds <= 1 || second.durationSeconds <= 1 || durationDiff <= 10
            }
            // Different explicit ISRCs represent distinct recordings
            return false
        }

        // 2. Normalized title must match
        if (fp1.baseTitle.isBlank() || fp1.baseTitle != fp2.baseTitle) {
            return false
        }

        // 3. Normalized artist name must match
        if (!artistsMatch(first.artist, second.artist)) {
            return false
        }

        // 4. Duration similarity check
        if (!durationsMatch(first.durationSeconds, second.durationSeconds)) {
            return false
        }

        // 5. Album compatibility check (do not merge conflicting distinct studio albums)
        return albumsCompatible(first, second)
    }

    fun mergeTracks(primary: UnifiedTrack, secondary: UnifiedTrack): UnifiedTrack {
        val primaryIsPlayable = !primary.isVideoEmbedOnly && primary.streamingPermitted && !primary.streamUrl.isNullOrBlank()
        val combinedSources = (primary.resolvedSources() + secondary.resolvedSources())
            .distinctBy { it.sourceKey }
            .sortedWith(sourcePriorityComparator(primary.providerId))

        val preferredSource = if (primaryIsPlayable) {
            combinedSources.firstOrNull {
                it.providerId == primary.providerId && !it.isVideoEmbedOnly && it.streamingPermitted && !it.streamUrl.isNullOrBlank()
            } ?: combinedSources.first()
        } else {
            combinedSources.firstOrNull {
                !it.isVideoEmbedOnly && it.streamingPermitted && !it.streamUrl.isNullOrBlank()
            } ?: combinedSources.first()
        }

        val mergedMetrics = mergeTrendMetrics(primary.trendMetrics, secondary.trendMetrics, combinedSources)

        return primary.copy(
            providerId = preferredSource.providerId,
            providerTrackId = preferredSource.providerTrackId,
            streamUrl = preferredSource.streamUrl ?: primary.streamUrl ?: secondary.streamUrl,
            originalProviderUrl = preferredSource.originalProviderUrl ?: primary.originalProviderUrl ?: secondary.originalProviderUrl,
            licenseType = preferredSource.licenseType.ifBlank { primary.licenseType },
            attributionInfo = preferredSource.attributionInfo.ifBlank { primary.attributionInfo },
            licenseUrl = preferredSource.licenseUrl ?: primary.licenseUrl ?: secondary.licenseUrl,
            commercialUsePermitted = preferredSource.commercialUsePermitted,
            streamingPermitted = combinedSources.any { !it.isVideoEmbedOnly && it.streamingPermitted && !it.streamUrl.isNullOrBlank() },
            artworkUrl = primary.artworkUrl ?: secondary.artworkUrl,
            album = pickRicherAlbum(primary, secondary),
            genre = primary.genre.ifBlank { secondary.genre },
            releaseDate = primary.releaseDate ?: secondary.releaseDate,
            isrc = normalizeIsrc(primary.isrc) ?: normalizeIsrc(secondary.isrc),
            isTrending = primary.isTrending || secondary.isTrending,
            isNewRelease = primary.isNewRelease || secondary.isNewRelease,
            sources = combinedSources,
            trendMetrics = mergedMetrics
        )
    }

    fun extractTitleFingerprint(rawTitle: String): TitleFingerprint {
        val lower = rawTitle.lowercase().trim()
        val qualifiers = LinkedHashSet<String>()

        // Inspect parenthetical, bracketed, and dash-separated suffixes for version markers
        val segmentRegex = Regex("""[\(\[\-–—]([^\)\]]+)[\)\]]?""")
        for (match in segmentRegex.findAll(lower)) {
            val segment = match.groupValues[1].trim()
            for (token in VERSION_QUALIFIER_TOKENS) {
                if (Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(segment)) {
                    qualifiers += normalizeQualifierSegment(segment, token)
                }
            }
        }
        // Also detect trailing qualifier words directly in title
        for (token in VERSION_QUALIFIER_TOKENS) {
            if (Regex("""\b${Regex.escape(token)}\b""").containsMatchIn(lower)) {
                qualifiers += canonicalQualifierFamily(token)
            }
        }

        // Strip featured artist clauses and bracketed/dashed qualifiers we already captured
        val withoutFeat = lower
            .replace(Regex("""[\(\[]\s*(feat\.?|ft\.?|featuring)\b[^\)\]]*[\)\]]"""), " ")
            .replace(Regex("""\b(feat\.?|ft\.?|featuring)\b.*$"""), " ")
            .replace(Regex("""[\(\[][^\)\]]*[\)\]]"""), " ")
            .replace(Regex("""\s+[-–—]\s+.*$"""), " ")

        val normalizedBase = normalizeAlphaNum(withoutFeat)
        return TitleFingerprint(baseTitle = normalizedBase, versionQualifiers = qualifiers)
    }

    private fun normalizeQualifierSegment(segment: String, token: String): String {
        val family = canonicalQualifierFamily(token)
        val cleanSeg = normalizeAlphaNum(segment)
        return if (cleanSeg.isNotBlank() && family == "remix") "remix:$cleanSeg" else family
    }

    private fun canonicalQualifierFamily(token: String): String = when (token) {
        "remix", "rmx", "rework", "bootleg", "flip", "vip", "club mix", "dub" -> "remix"
        "live", "session", "concert" -> "live"
        "acoustic", "unplugged" -> "acoustic"
        "cover", "karaoke", "tribute" -> "cover"
        "instrumental" -> "instrumental"
        "acapella", "a capella" -> "acapella"
        "remaster", "remastered" -> "remaster"
        "sped up", "slowed", "reverb", "nightcore" -> "tempo_alt"
        "extended", "radio edit" -> "edit"
        else -> token
    }

    private fun artistsMatch(firstArtist: String, secondArtist: String): Boolean {
        val a1 = normalizePrimaryArtist(firstArtist)
        val a2 = normalizePrimaryArtist(secondArtist)
        if (a1.isBlank() || a2.isBlank()) return false
        if (a1 == a2) return true
        // Allow match if one artist string includes collaborating artists ("Artist A & Artist B" vs "Artist A")
        val tokens1 = splitArtistNames(firstArtist)
        val tokens2 = splitArtistNames(secondArtist)
        return tokens1.isNotEmpty() && tokens2.isNotEmpty() && tokens1.first() == tokens2.first()
    }

    private fun splitArtistNames(artist: String): List<String> =
        artist.lowercase()
            .split(Regex("""\s*(?:,|&|\bx\b|\bfeat\.?\b|\bft\.?\b|\bfeaturing\b)\s*"""))
            .map { normalizeAlphaNum(it) }
            .filter { it.isNotBlank() }

    private fun normalizePrimaryArtist(artist: String): String =
        splitArtistNames(artist).firstOrNull().orEmpty()

    private fun durationsMatch(d1: Int, d2: Int): Boolean {
        if (d1 <= 1 || d2 <= 1) return true
        val maxDur = maxOf(d1, d2)
        val tolerance = if (maxDur > 180) 8 else 5
        return abs(d1 - d2) <= tolerance
    }

    private fun albumsCompatible(first: UnifiedTrack, second: UnifiedTrack): Boolean {
        val alb1 = first.album.trim().lowercase()
        val alb2 = second.album.trim().lowercase()
        val gen1 = first.genre.trim().lowercase()
        val gen2 = second.genre.trim().lowercase()
        val generic1 = alb1 in GENERIC_ALBUM_LABELS || alb1 == gen1
        val generic2 = alb2 in GENERIC_ALBUM_LABELS || alb2 == gen2
        if (generic1 || generic2) return true
        val norm1 = normalizeAlphaNum(alb1)
        val norm2 = normalizeAlphaNum(alb2)
        return norm1 == norm2 || norm1.contains(norm2) || norm2.contains(norm1)
    }

    private fun pickRicherAlbum(first: UnifiedTrack, second: UnifiedTrack): String {
        val a1 = first.album.trim()
        val a2 = second.album.trim()
        val g1 = a1.lowercase() in GENERIC_ALBUM_LABELS || a1.equals(first.genre, ignoreCase = true)
        val g2 = a2.lowercase() in GENERIC_ALBUM_LABELS || a2.equals(second.genre, ignoreCase = true)
        return when {
            !g1 -> a1
            !g2 -> a2
            else -> a1.ifBlank { a2 }
        }
    }

    private fun mergeTrendMetrics(
        first: TrendMetrics?,
        second: TrendMetrics?,
        sources: List<TrackSource>
    ): TrendMetrics? {
        if (first == null && second == null) return null
        val bestRank = listOfNotNull(first?.providerRank, second?.providerRank).minOrNull()
        val chartLabels = listOfNotNull(first?.sourceChartLabel, second?.sourceChartLabel)
            .distinct()
            .joinToString(" · ")
            .ifBlank { sources.map { it.providerId.displayName }.distinct().joinToString(" · ") }

        return TrendMetrics(
            providerRank = bestRank,
            listensOrPlays = sumNullable(first?.listensOrPlays, second?.listensOrPlays),
            downloads = sumNullable(first?.downloads, second?.downloads),
            favorites = sumNullable(first?.favorites, second?.favorites),
            reposts = sumNullable(first?.reposts, second?.reposts),
            playlistAdds = sumNullable(first?.playlistAdds, second?.playlistAdds),
            trendScore = maxOf(first?.trendScore ?: 0.0, second?.trendScore ?: 0.0),
            freshnessBoost = maxOf(first?.freshnessBoost ?: 0.0, second?.freshnessBoost ?: 0.0),
            sourceChartLabel = chartLabels
        )
    }

    private fun sumNullable(a: Long?, b: Long?): Long? = when {
        a != null && b != null -> a + b
        a != null -> a
        else -> b
    }

    private fun sourcePriorityComparator(preferredProviderId: com.example.model.ProviderId): Comparator<TrackSource> = Comparator { a, b ->
        val aPlayable = !a.isVideoEmbedOnly && a.streamingPermitted && !a.streamUrl.isNullOrBlank()
        val bPlayable = !b.isVideoEmbedOnly && b.streamingPermitted && !b.streamUrl.isNullOrBlank()
        val aPreferred = a.providerId == preferredProviderId
        val bPreferred = b.providerId == preferredProviderId
        when {
            aPlayable != bPlayable -> if (aPlayable) -1 else 1
            aPreferred != bPreferred -> if (aPreferred) -1 else 1
            a.commercialUsePermitted != b.commercialUsePermitted -> if (a.commercialUsePermitted) -1 else 1
            else -> 0
        }
    }

    fun normalizeIsrc(isrc: String?): String? {
        val clean = isrc?.trim()?.uppercase()?.replace(Regex("[^A-Z0-9]"), "") ?: return null
        return clean.takeIf { it.length == 12 }
    }

    private fun normalizeAlphaNum(text: String): String =
        text.lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]"), "")
}

package com.example.playback

import com.example.model.ProviderId
import com.example.model.Track
import com.example.model.TrackSource

sealed interface PlaybackResolution {
    data class AudioPlayback(
        val resolvedTrack: Track,
        val activeSource: TrackSource,
        val remainingSources: List<TrackSource>
    ) : PlaybackResolution

    data class YouTubeEmbed(val track: Track) : PlaybackResolution

    data class Unavailable(val reason: String) : PlaybackResolution
}

/**
 * Automatically selects the best authorized playback source for a unified [Track]
 * and resolves compatible fallback sources when a stream fails — without ever prompting
 * the user to pick a provider or routing YouTube through Media3.
 */
object PlaybackRouter {

    fun resolveInitialPlayback(
        track: Track,
        attemptedSourceKeys: Set<String> = emptySet()
    ): PlaybackResolution {
        val audioCandidates = rankedAudioCandidates(track)
            .filterNot { attemptedSourceKeys.contains(it.sourceKey) }

        if (audioCandidates.isNotEmpty()) {
            val selected = audioCandidates.first()
            return PlaybackResolution.AudioPlayback(
                resolvedTrack = track.withActiveSource(selected),
                activeSource = selected,
                remainingSources = audioCandidates.drop(1)
            )
        }

        if (track.isVideoEmbedOnly || track.providerId == ProviderId.YOUTUBE) {
            return PlaybackResolution.YouTubeEmbed(track)
        }

        return PlaybackResolution.Unavailable(
            "Streaming is restricted or unavailable for ${track.title}."
        )
    }

    fun resolveFailover(
        track: Track,
        attemptedSourceKeys: Set<String>
    ): PlaybackResolution.AudioPlayback? {
        val nextCandidates = rankedAudioCandidates(track)
            .filterNot { attemptedSourceKeys.contains(it.sourceKey) }
        val next = nextCandidates.firstOrNull() ?: return null
        return PlaybackResolution.AudioPlayback(
            resolvedTrack = track.withActiveSource(next),
            activeSource = next,
            remainingSources = nextCandidates.drop(1)
        )
    }

    fun rankedAudioCandidates(track: Track): List<TrackSource> {
        return track.authorizedAudioSources()
            .sortedWith(
                compareByDescending<TrackSource> { it.providerId == track.providerId }
                    .thenByDescending { it.commercialUsePermitted }
                    .thenBy { providerPriority(it.providerId) }
            )
    }

    private fun providerPriority(providerId: ProviderId): Int = when (providerId) {
        ProviderId.JAMENDO -> 0
        ProviderId.AUDIUS -> 1
        ProviderId.SOUNDCLOUD -> 2
        ProviderId.SPOTIFY -> 3
        ProviderId.YOUTUBE -> 99
    }
}

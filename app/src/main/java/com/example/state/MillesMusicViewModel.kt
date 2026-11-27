package com.example.state

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioStreamPlayer
import com.example.audio.JamendoStreamPlayer
import com.example.data.JamendoRepository
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.AudioQuality
import com.example.model.GenreCategory
import com.example.model.Playlist
import com.example.model.RepeatMode
import com.example.model.SampleCatalog
import com.example.model.SecondaryDestination
import com.example.model.ThemeMode
import com.example.model.Track
import com.example.ui.PlayerUiState
import com.example.ui.components.MillesTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class MillesUiState(
    val activeTab: MillesTab = MillesTab.Home,
    val secondaryStack: List<SecondaryDestination> = emptyList(),
    val isNowPlayingOpen: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,

    // Playback state
    val currentTrack: Track = SampleCatalog.tracks.first(),
    val queue: List<Track> = SampleCatalog.tracks,
    val playbackSource: String = "Trending Now",
    val isPlaying: Boolean = false,
    val progressSeconds: Int = 45,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val audioQuality: AudioQuality = AudioQuality.HIGH,

    // Discovery & Library state
    val trendingTracks: List<Track> = SampleCatalog.tracks.filter { it.isTrending },
    val newReleases: List<Track> = SampleCatalog.tracks.filter { it.isNewRelease },
    val topCharts: List<Track> = SampleCatalog.tracks,
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val genreTracksByGenreId: Map<String, List<Track>> = emptyMap(),
    val favoriteTrackIds: Set<String> = SampleCatalog.initialFavoriteTrackIds,
    val userPlaylists: List<Playlist> = SampleCatalog.initialUserPlaylists,

    // UI overlays
    val isCreatePlaylistOpen: Boolean = false,
    val isAudioQualitySheetOpen: Boolean = false,
    val trackOptionsTarget: Track? = null,
    val profileDetailSheet: ProfileSheetType? = null,
    val statusBannerMessage: String? = null
) {
    val activeSecondary: SecondaryDestination?
        get() = secondaryStack.lastOrNull()

    val isCurrentTrackFavorite: Boolean
        get() = favoriteTrackIds.contains(currentTrack.id)

    val knownTracks: List<Track>
        get() = (
            listOf(currentTrack) +
                queue +
                trendingTracks +
                newReleases +
                topCharts +
                searchResults +
                genreTracksByGenreId.values.flatten() +
                SampleCatalog.tracks
            ).distinctBy { it.id }

    val favoriteTracks: List<Track>
        get() = knownTracks.filter { favoriteTrackIds.contains(it.id) }
}

enum class ProfileSheetType {
    SETTINGS,
    NOTIFICATIONS,
    ABOUT,
    HELP
}

class MillesMusicViewModel(
    private val jamendoRepository: JamendoRepository = JamendoRepository(),
    private val streamPlayer: AudioStreamPlayer = JamendoStreamPlayer()
) : ViewModel() {

    val isJamendoConfigured: Boolean
        get() = jamendoRepository.isConfigured

    private val _uiState = MutableStateFlow(MillesUiState())
    val uiState: StateFlow<MillesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var streamingTrackId: String? = null

    init {
        runPlaybackTicker()
        if (jamendoRepository.isConfigured) {
            loadJamendoCatalog()
        }
    }

    private fun loadJamendoCatalog() {
        viewModelScope.launch {
            coroutineScope {
                val trendingDeferred = async { jamendoRepository.fetchTrendingTracks() }
                val newReleasesDeferred = async { jamendoRepository.fetchNewReleases() }
                val trendingResult = trendingDeferred.await()
                val newReleasesResult = newReleasesDeferred.await()

                val liveTrending = trendingResult.getOrNull().orEmpty()
                val liveReleases = newReleasesResult.getOrNull().orEmpty()

                if (liveTrending.isNotEmpty() || liveReleases.isNotEmpty()) {
                    val combined = (liveTrending + liveReleases).distinctBy { it.id }
                    _uiState.update { state ->
                        val shouldReplaceInitialTrack =
                            !state.isPlaying && state.currentTrack.id == SampleCatalog.tracks.first().id
                        val nextCurrent = if (shouldReplaceInitialTrack && combined.isNotEmpty()) {
                            combined.first()
                        } else {
                            state.currentTrack
                        }
                        val nextProgress = if (shouldReplaceInitialTrack) 0 else state.progressSeconds
                        val nextQueue = if (shouldReplaceInitialTrack && combined.isNotEmpty()) {
                            combined
                        } else {
                            state.queue
                        }
                        state.copy(
                            trendingTracks = liveTrending.ifEmpty { state.trendingTracks },
                            newReleases = liveReleases.ifEmpty { state.newReleases },
                            topCharts = combined.ifEmpty { state.topCharts },
                            currentTrack = nextCurrent,
                            progressSeconds = nextProgress,
                            queue = nextQueue
                        )
                    }
                } else {
                    val failureMessage = trendingResult.exceptionOrNull()?.message
                        ?: newReleasesResult.exceptionOrNull()?.message
                    if (!failureMessage.isNullOrBlank()) {
                        showStatusBanner(failureMessage)
                    }
                }
            }
        }
    }

    private fun runPlaybackTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val snapshot = _uiState.value
                if (snapshot.isPlaying) {
                    val hasActiveStream = snapshot.currentTrack.streamUrl != null &&
                        streamingTrackId == snapshot.currentTrack.id
                    if (hasActiveStream) {
                        val playerSeconds = streamPlayer.currentPositionSeconds()
                        if (playerSeconds != null) {
                            _uiState.update {
                                it.copy(
                                    progressSeconds = playerSeconds.coerceIn(
                                        0,
                                        it.currentTrack.durationSeconds.coerceAtLeast(1)
                                    )
                                )
                            }
                        }
                    } else {
                        val nextSec = snapshot.progressSeconds + 1
                        if (nextSec >= snapshot.currentTrack.durationSeconds) {
                            advanceTrackOnCompletion()
                        } else {
                            _uiState.update { it.copy(progressSeconds = nextSec) }
                        }
                    }
                }
            }
        }
    }

    private fun startStreamingIfNeeded(track: Track) {
        val url = track.streamUrl
        if (url.isNullOrBlank()) {
            streamingTrackId = null
            streamPlayer.stopAndReset()
            return
        }
        streamingTrackId = track.id
        streamPlayer.playStream(
            url = url,
            onPrepared = { durationSeconds ->
                _uiState.update { state ->
                    if (state.currentTrack.id == track.id) {
                        state.copy(
                            currentTrack = state.currentTrack.copy(durationSeconds = durationSeconds),
                            isPlaying = true
                        )
                    } else state
                }
            },
            onCompletion = {
                if (_uiState.value.currentTrack.id == track.id) {
                    advanceTrackOnCompletion()
                }
            },
            onError = { message ->
                if (_uiState.value.currentTrack.id == track.id) {
                    streamingTrackId = null
                    _uiState.update { it.copy(isPlaying = false) }
                    showStatusBanner(message)
                }
            }
        )
    }

    private fun advanceTrackOnCompletion() {
        val state = _uiState.value
        if (state.repeatMode == RepeatMode.ONE) {
            _uiState.update { it.copy(progressSeconds = 0, isPlaying = true) }
            if (state.currentTrack.streamUrl != null) {
                startStreamingIfNeeded(state.currentTrack)
            }
            return
        }
        skipToNext(autoTriggered = true)
    }

    fun selectTab(tab: MillesTab) {
        _uiState.update {
            it.copy(
                activeTab = tab,
                secondaryStack = emptyList(),
                isNowPlayingOpen = false
            )
        }
    }

    fun navigateTo(destination: SecondaryDestination) {
        _uiState.update {
            it.copy(
                secondaryStack = it.secondaryStack + destination,
                isNowPlayingOpen = false
            )
        }
        if (destination is SecondaryDestination.GenreDetail) {
            loadGenreTracks(destination.genreId)
        }
    }

    private fun loadGenreTracks(genreId: String) {
        if (!jamendoRepository.isConfigured) return
        if (_uiState.value.genreTracksByGenreId.containsKey(genreId)) return
        val genre = findGenre(genreId) ?: return
        viewModelScope.launch {
            jamendoRepository.fetchTracksByGenre(genre.name).onSuccess { tracks ->
                if (tracks.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            genreTracksByGenreId = state.genreTracksByGenreId + (genreId to tracks)
                        )
                    }
                }
            }
        }
    }

    fun navigateBack(): Boolean {
        val state = _uiState.value
        return when {
            state.isAudioQualitySheetOpen -> {
                _uiState.update { it.copy(isAudioQualitySheetOpen = false) }
                true
            }
            state.trackOptionsTarget != null -> {
                _uiState.update { it.copy(trackOptionsTarget = null) }
                true
            }
            state.isCreatePlaylistOpen -> {
                _uiState.update { it.copy(isCreatePlaylistOpen = false) }
                true
            }
            state.profileDetailSheet != null -> {
                _uiState.update { it.copy(profileDetailSheet = null) }
                true
            }
            state.isNowPlayingOpen -> {
                _uiState.update { it.copy(isNowPlayingOpen = false) }
                true
            }
            state.secondaryStack.isNotEmpty() -> {
                _uiState.update { it.copy(secondaryStack = it.secondaryStack.dropLast(1)) }
                true
            }
            else -> false
        }
    }

    fun openNowPlaying() {
        _uiState.update { it.copy(isNowPlayingOpen = true) }
    }

    fun closeNowPlaying() {
        _uiState.update {
            it.copy(
                isNowPlayingOpen = false,
                isAudioQualitySheetOpen = false,
                trackOptionsTarget = null
            )
        }
    }

    fun togglePlayPause() {
        val snapshot = _uiState.value
        val nextPlaying = !snapshot.isPlaying
        _uiState.update { it.copy(isPlaying = nextPlaying) }
        val track = snapshot.currentTrack
        if (track.streamUrl != null) {
            if (nextPlaying) {
                if (streamingTrackId == track.id && streamPlayer.currentPositionSeconds() != null) {
                    streamPlayer.resume()
                } else {
                    startStreamingIfNeeded(track)
                }
            } else {
                streamPlayer.pause()
            }
        }
    }

    fun playTrack(
        track: Track,
        customQueue: List<Track>? = null,
        source: String? = null,
        openPlayer: Boolean = false
    ) {
        val prevTrackId = _uiState.value.currentTrack.id
        _uiState.update { state ->
            val fallbackQueue = state.topCharts.ifEmpty { SampleCatalog.tracks }
            val newQueue = customQueue ?: if (state.queue.any { it.id == track.id }) state.queue else fallbackQueue
            state.copy(
                currentTrack = track,
                queue = newQueue,
                playbackSource = source ?: track.album,
                isPlaying = true,
                progressSeconds = if (state.currentTrack.id == track.id) state.progressSeconds else 0,
                isNowPlayingOpen = openPlayer || state.isNowPlayingOpen
            )
        }
        if (track.id != prevTrackId || streamingTrackId != track.id) {
            startStreamingIfNeeded(track)
        } else if (track.streamUrl != null) {
            streamPlayer.resume()
        }
    }

    fun playHeroMix() {
        val mix = _uiState.value.trendingTracks.ifEmpty { SampleCatalog.tracks }
        playTrack(mix.first(), customQueue = mix, source = "Good Music, Every Day", openPlayer = true)
    }

    fun playTracksCollection(tracks: List<Track>, source: String, shuffled: Boolean, openPlayer: Boolean = false) {
        if (tracks.isEmpty()) return
        val ordered = if (shuffled) tracks.shuffled() else tracks
        _uiState.update { it.copy(isShuffleEnabled = shuffled) }
        playTrack(ordered.first(), customQueue = ordered, source = source, openPlayer = openPlayer)
    }

    fun seekTo(seconds: Int) {
        var clampedTarget = 0
        _uiState.update { state ->
            clampedTarget = seconds.coerceIn(0, state.currentTrack.durationSeconds)
            state.copy(progressSeconds = clampedTarget)
        }
        if (_uiState.value.currentTrack.streamUrl != null &&
            streamingTrackId == _uiState.value.currentTrack.id
        ) {
            streamPlayer.seekToMs(clampedTarget * 1000)
        }
    }

    fun seekMs(positionMs: Long) {
        seekTo((positionMs / 1000L).toInt())
    }

    fun skipToNext(autoTriggered: Boolean = false) {
        var nextTrackToPlay: Track? = null
        var stopped = false
        _uiState.update { state ->
            val list = state.queue.ifEmpty { state.topCharts.ifEmpty { SampleCatalog.tracks } }
            val currentIdx = list.indexOfFirst { it.id == state.currentTrack.id }
            val nextTrack = if (state.isShuffleEnabled && list.size > 1) {
                list.filter { it.id != state.currentTrack.id }.random()
            } else {
                val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % list.size
                if (autoTriggered && state.repeatMode == RepeatMode.OFF && currentIdx == list.lastIndex) {
                    stopped = true
                    return@update state.copy(isPlaying = false, progressSeconds = 0)
                }
                list[nextIdx]
            }
            nextTrackToPlay = nextTrack
            state.copy(currentTrack = nextTrack, progressSeconds = 0, isPlaying = true)
        }
        if (stopped) {
            streamingTrackId = null
            streamPlayer.stopAndReset()
        } else {
            nextTrackToPlay?.let(::startStreamingIfNeeded)
        }
    }

    fun skipToPrevious() {
        var prevTrackToPlay: Track? = null
        var restartOnly = false
        _uiState.update { state ->
            if (state.progressSeconds > 4) {
                restartOnly = true
                return@update state.copy(progressSeconds = 0)
            }
            val list = state.queue.ifEmpty { state.topCharts.ifEmpty { SampleCatalog.tracks } }
            val currentIdx = list.indexOfFirst { it.id == state.currentTrack.id }
            val prevIdx = if (currentIdx <= 0) list.lastIndex else currentIdx - 1
            prevTrackToPlay = list[prevIdx]
            state.copy(currentTrack = list[prevIdx], progressSeconds = 0, isPlaying = true)
        }
        if (restartOnly) {
            if (_uiState.value.currentTrack.streamUrl != null) {
                streamPlayer.seekToMs(0)
            }
        } else {
            prevTrackToPlay?.let(::startStreamingIfNeeded)
        }
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun cycleRepeatMode() {
        _uiState.update { state ->
            val next = when (state.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.OFF
                RepeatMode.ONE -> RepeatMode.OFF
            }
            state.copy(repeatMode = next)
        }
    }

    fun toggleFavorite(trackId: String) {
        var added = false
        _uiState.update { state ->
            val next = state.favoriteTrackIds.toMutableSet()
            if (next.contains(trackId)) {
                next.remove(trackId)
                added = false
            } else {
                next.add(trackId)
                added = true
            }
            state.copy(favoriteTrackIds = next)
        }
        showStatusBanner(if (added) "Added to Liked Songs" else "Removed from Liked Songs")
    }

    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(searchQuery = query, searchResults = emptyList(), isSearching = false) }
            return
        }

        if (!jamendoRepository.isConfigured) {
            val localMatches = filterKnownTracks(_uiState.value.knownTracks, trimmed)
            _uiState.update {
                it.copy(searchQuery = query, searchResults = localMatches, isSearching = false)
            }
            return
        }

        _uiState.update { it.copy(searchQuery = query, isSearching = true) }
        searchJob = viewModelScope.launch {
            delay(350L)
            jamendoRepository.searchTracks(trimmed).fold(
                onSuccess = { remoteTracks ->
                    _uiState.update { state ->
                        if (state.searchQuery.trim() == trimmed) {
                            state.copy(searchResults = remoteTracks, isSearching = false)
                        } else state
                    }
                },
                onFailure = { error ->
                    val fallbackMatches = filterKnownTracks(_uiState.value.knownTracks, trimmed)
                    _uiState.update { state ->
                        if (state.searchQuery.trim() == trimmed) {
                            state.copy(searchResults = fallbackMatches, isSearching = false)
                        } else state
                    }
                    error.message?.let(::showStatusBanner)
                }
            )
        }
    }

    private fun filterKnownTracks(tracks: List<Track>, query: String): List<Track> =
        tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true)
        }

    fun selectThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun toggleDynamicTheme(enabled: Boolean) {
        _uiState.update { state ->
            val mode = if (enabled) ThemeMode.AUTO else ThemeMode.DARK
            state.copy(themeMode = mode)
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        _uiState.update {
            it.copy(
                audioQuality = quality,
                isAudioQualitySheetOpen = false
            )
        }
        showStatusBanner("Audio quality set to ${quality.title}")
    }

    fun setAudioQualitySheetOpen(open: Boolean) {
        _uiState.update { it.copy(isAudioQualitySheetOpen = open) }
    }

    fun openTrackOptions(track: Track?) {
        _uiState.update { it.copy(trackOptionsTarget = track) }
    }

    fun setCreatePlaylistOpen(open: Boolean) {
        _uiState.update { it.copy(isCreatePlaylistOpen = open) }
    }

    fun createPlaylist(title: String, subtitle: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanSubtitle = subtitle.trim().ifEmpty { "Personal collection" }
        val newPlaylist = Playlist(
            id = "pl_user_${System.currentTimeMillis()}",
            title = cleanTitle,
            subtitle = cleanSubtitle,
            trackIds = listOf(_uiState.value.currentTrack.id),
            artwork = ArtworkDescriptor(
                imageRes = _uiState.value.currentTrack.artwork.imageRes,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF282B20),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF050505),
                accentColor = androidx.compose.ui.graphics.Color(0xFFC6F24E),
                patternVariant = (_uiState.value.userPlaylists.size % 6) + 1
            ),
            displayedSongCount = 1,
            isUserCreated = true
        )
        _uiState.update { state ->
            state.copy(
                userPlaylists = listOf(newPlaylist) + state.userPlaylists,
                isCreatePlaylistOpen = false
            )
        }
        showStatusBanner("Created playlist \"$cleanTitle\"")
    }

    fun addTrackToPlaylist(track: Track, playlistId: String) {
        var targetName = ""
        _uiState.update { state ->
            val updated = state.userPlaylists.map { pl ->
                if (pl.id == playlistId) {
                    targetName = pl.title
                    if (pl.trackIds.contains(track.id)) {
                        pl
                    } else {
                        pl.copy(
                            trackIds = pl.trackIds + track.id,
                            displayedSongCount = pl.displayedSongCount + 1
                        )
                    }
                } else pl
            }
            state.copy(userPlaylists = updated, trackOptionsTarget = null)
        }
        if (targetName.isNotEmpty()) {
            showStatusBanner("Added to $targetName")
        }
    }

    fun openProfileSheet(type: ProfileSheetType?) {
        _uiState.update { it.copy(profileDetailSheet = type) }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun showStatusBanner(message: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusBannerMessage = message) }
            delay(2200L)
            _uiState.update { state ->
                if (state.statusBannerMessage == message) {
                    state.copy(statusBannerMessage = null)
                } else state
            }
        }
    }

    fun findTrackById(id: String): Track? =
        _uiState.value.knownTracks.find { it.id == id }

    fun findTracksByIds(ids: List<String>): List<Track> =
        ids.mapNotNull(::findTrackById)

    fun findPlaylist(id: String): Playlist? =
        _uiState.value.userPlaylists.find { it.id == id }
            ?: SampleCatalog.trendingCollections.find { it.id == id }
            ?: SampleCatalog.madeForYouPlaylists.find { it.id == id }

    fun findGenre(id: String): GenreCategory? =
        SampleCatalog.genres.find { it.id == id }

    fun findArtist(id: String): Artist? =
        SampleCatalog.artists.find { it.id == id }

    fun findAlbum(id: String): Album? =
        SampleCatalog.albums.find { it.id == id }

    override fun onCleared() {
        streamPlayer.release()
        super.onCleared()
    }
}

/** Maps domain Track into the new UI Track model used by the /ui composables. */
fun Track.toUiTrack(): com.example.ui.Track = com.example.ui.Track(
    id = id,
    title = title,
    artist = artist,
    coverUrl = coverUrl ?: artwork.imageRes,
    durationMs = durationSeconds * 1000L
)

fun Playlist.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = title,
    subtitle = subtitle,
    coverUrl = artwork.imageRes
)

fun Album.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = title,
    subtitle = "$artist · $year",
    coverUrl = artwork.imageRes
)

fun Artist.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = name,
    subtitle = genre,
    coverUrl = artwork.imageRes
)

fun GenreCategory.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = name,
    subtitle = subtitle,
    coverUrl = artwork.imageRes
)

fun MillesUiState.toPlayerUiState(): PlayerUiState = PlayerUiState(
    track = currentTrack.toUiTrack(),
    isPlaying = isPlaying,
    positionMs = progressSeconds * 1000L,
    isLiked = isCurrentTrackFavorite,
    shuffle = isShuffleEnabled,
    repeat = repeatMode != RepeatMode.OFF,
    quality = audioQuality.shortLabel,
    source = playbackSource,
    queue = queue.map { it.toUiTrack() },
    related = (topCharts + trendingTracks + SampleCatalog.tracks)
        .distinctBy { it.id }
        .filter { it.id != currentTrack.id }
        .take(8)
        .map { it.toUiTrack() },
    lyrics = SampleCatalog.lyricsForTrack(currentTrack.id)
)

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

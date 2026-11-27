package com.example.state

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val searchQuery: String = "",
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

    val favoriteTracks: List<Track>
        get() = SampleCatalog.tracks.filter { favoriteTrackIds.contains(it.id) }
}

enum class ProfileSheetType {
    SETTINGS,
    NOTIFICATIONS,
    ABOUT,
    HELP
}

class MillesMusicViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MillesUiState())
    val uiState: StateFlow<MillesUiState> = _uiState.asStateFlow()

    init {
        runPlaybackTicker()
    }

    private fun runPlaybackTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val snapshot = _uiState.value
                if (snapshot.isPlaying) {
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

    private fun advanceTrackOnCompletion() {
        val state = _uiState.value
        if (state.repeatMode == RepeatMode.ONE) {
            _uiState.update { it.copy(progressSeconds = 0, isPlaying = true) }
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
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun playTrack(
        track: Track,
        customQueue: List<Track>? = null,
        source: String? = null,
        openPlayer: Boolean = false
    ) {
        _uiState.update { state ->
            val newQueue = customQueue ?: if (state.queue.any { it.id == track.id }) state.queue else SampleCatalog.tracks
            state.copy(
                currentTrack = track,
                queue = newQueue,
                playbackSource = source ?: track.album,
                isPlaying = true,
                progressSeconds = if (state.currentTrack.id == track.id) state.progressSeconds else 0,
                isNowPlayingOpen = openPlayer || state.isNowPlayingOpen
            )
        }
    }

    fun playHeroMix() {
        val mix = SampleCatalog.tracks.filter { it.isTrending }.ifEmpty { SampleCatalog.tracks }
        playTrack(mix.first(), customQueue = mix, source = "Good Music, Every Day", openPlayer = true)
    }

    fun playTracksCollection(tracks: List<Track>, source: String, shuffled: Boolean, openPlayer: Boolean = false) {
        if (tracks.isEmpty()) return
        val ordered = if (shuffled) tracks.shuffled() else tracks
        _uiState.update { it.copy(isShuffleEnabled = shuffled) }
        playTrack(ordered.first(), customQueue = ordered, source = source, openPlayer = openPlayer)
    }

    fun seekTo(seconds: Int) {
        _uiState.update { state ->
            val clamped = seconds.coerceIn(0, state.currentTrack.durationSeconds)
            state.copy(progressSeconds = clamped)
        }
    }

    fun seekMs(positionMs: Long) {
        seekTo((positionMs / 1000L).toInt())
    }

    fun skipToNext(autoTriggered: Boolean = false) {
        _uiState.update { state ->
            val list = state.queue.ifEmpty { SampleCatalog.tracks }
            val currentIdx = list.indexOfFirst { it.id == state.currentTrack.id }
            val nextTrack = if (state.isShuffleEnabled && list.size > 1) {
                list.filter { it.id != state.currentTrack.id }.random()
            } else {
                val nextIdx = if (currentIdx < 0) 0 else (currentIdx + 1) % list.size
                if (autoTriggered && state.repeatMode == RepeatMode.OFF && currentIdx == list.lastIndex) {
                    return@update state.copy(isPlaying = false, progressSeconds = 0)
                }
                list[nextIdx]
            }
            state.copy(currentTrack = nextTrack, progressSeconds = 0, isPlaying = true)
        }
    }

    fun skipToPrevious() {
        _uiState.update { state ->
            if (state.progressSeconds > 4) {
                return@update state.copy(progressSeconds = 0)
            }
            val list = state.queue.ifEmpty { SampleCatalog.tracks }
            val currentIdx = list.indexOfFirst { it.id == state.currentTrack.id }
            val prevIdx = if (currentIdx <= 0) list.lastIndex else currentIdx - 1
            state.copy(currentTrack = list[prevIdx], progressSeconds = 0, isPlaying = true)
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
        _uiState.update { it.copy(searchQuery = query) }
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
}

/** Maps domain Track into the new UI Track model used by the /ui composables. */
fun Track.toUiTrack(): com.example.ui.Track = com.example.ui.Track(
    id = id,
    title = title,
    artist = artist,
    coverUrl = artwork.imageRes,
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
    related = SampleCatalog.tracks.filter { it.id != currentTrack.id }.take(8).map { it.toUiTrack() },
    lyrics = SampleCatalog.lyricsForTrack(currentTrack.id)
)

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

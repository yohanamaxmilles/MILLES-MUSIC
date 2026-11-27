package com.example.state

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import android.net.Uri
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

    private var playbackController: MediaController? = null
    private var playerListener: Player.Listener? = null

    fun attachPlaybackController(controller: MediaController) {
        detachPlaybackController()
        playbackController = controller
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val track = mediaItem?.mediaId?.let { id ->
                    _uiState.value.queue.firstOrNull { it.id == id }
                        ?: SampleCatalog.trackById(id)
                } ?: return
                _uiState.update { it.copy(currentTrack = track, progressSeconds = 0) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _uiState.update { it.copy(isPlaying = false) }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _uiState.update { it.copy(isPlaying = false) }
                showStatusBanner("Playback error: ${error.errorCodeName}")
            }
        }
        playerListener = listener
        controller.addListener(listener)
        syncPlayerState()
        controller.shuffleModeEnabled = _uiState.value.isShuffleEnabled
        controller.repeatMode = repeatModeToPlayer(_uiState.value.repeatMode)
        viewModelScope.launch {
            while (playbackController === controller) {
                syncPlayerState()
                delay(500L)
            }
        }
    }

    fun detachPlaybackController() {
        val controller = playbackController ?: return
        playerListener?.let(controller::removeListener)
        playerListener = null
        playbackController = null
    }

    private fun syncPlayerState() {
        val player = playbackController ?: return
        val duration = player.duration.takeIf { it > 0L } ?: 0L
        _uiState.update { state ->
            state.copy(
                isPlaying = player.isPlaying,
                progressSeconds = (player.currentPosition / 1000L).toInt()
                    .coerceAtLeast(0)
                    .let { if (duration > 0L) it.coerceAtMost((duration / 1000L).toInt()) else it }
            )
        }
    }

    private fun repeatModeToPlayer(mode: RepeatMode): Int = when (mode) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private fun mediaItemFor(track: Track): MediaItem? {
        val stream = track.streamUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?: return null
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(Uri.parse(stream))
            .setMediaMetadata(metadata)
            .build()
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
        val player = playbackController
        if (player == null) {
            showStatusBanner("Connecting to the audio player")
            return
        }
        if (player.isPlaying) player.pause() else {
            if (player.currentMediaItem == null) {
                showStatusBanner("Choose a track with an authorised audio stream")
            } else player.play()
        }
    }

    fun playTrack(
        track: Track,
        customQueue: List<Track>? = null,
        source: String? = null,
        openPlayer: Boolean = false
    ) {
        val newQueue = customQueue ?: if (_uiState.value.queue.any { it.id == track.id }) {
            _uiState.value.queue
        } else SampleCatalog.tracks
        val controller = playbackController
        val item = mediaItemFor(track)
        if (controller != null) {
            if (item == null) {
                showStatusBanner("This catalogue entry has no authorised audio stream yet")
                _uiState.update { it.copy(isNowPlayingOpen = openPlayer || it.isNowPlayingOpen) }
                return
            }
            val playableItems = newQueue.mapNotNull(::mediaItemFor)
            val startIndex = playableItems.indexOfFirst { it.mediaId == track.id }.coerceAtLeast(0)
            controller.setMediaItems(playableItems, startIndex, 0L)
            controller.prepare()
            controller.play()
        } else {
            showStatusBanner("Audio player is not connected yet")
            return
        }
        _uiState.update { state ->
            state.copy(
                currentTrack = track,
                queue = newQueue,
                playbackSource = source ?: track.album,
                isPlaying = true,
                progressSeconds = 0,
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
        val player = playbackController
        if (player != null && player.currentMediaItem != null) {
            player.seekTo(seconds.coerceAtLeast(0) * 1000L)
        }
        _uiState.update { state ->
            val clamped = seconds.coerceIn(0, state.currentTrack.durationSeconds)
            state.copy(progressSeconds = clamped)
        }
    }

    fun seekMs(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        playbackController?.takeIf { it.currentMediaItem != null }?.seekTo(safePosition)
        _uiState.update { it.copy(progressSeconds = (safePosition / 1000L).toInt()) }
    }

    fun skipToNext(autoTriggered: Boolean = false) {
        val player = playbackController
        if (player?.currentMediaItem != null) {
            if (player.hasNextMediaItem()) player.seekToNextMediaItem()
            else if (player.repeatMode == Player.REPEAT_MODE_ALL) player.seekToNextMediaItem()
            else if (autoTriggered) player.pause()
            else showStatusBanner("End of queue")
            return
        }
        showStatusBanner("No playable tracks in the queue")
    }

    fun skipToPrevious() {
        val player = playbackController
        if (player?.currentMediaItem != null) {
            if (player.currentPosition > 4_000L) player.seekTo(0L)
            else if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
            else player.seekTo(0L)
            return
        }
        showStatusBanner("No playable track is loaded")
    }

    fun toggleShuffle() {
        val enabled = !_uiState.value.isShuffleEnabled
        playbackController?.shuffleModeEnabled = enabled
        _uiState.update { it.copy(isShuffleEnabled = enabled) }
    }

    fun cycleRepeatMode() {
        _uiState.update { state ->
            val next = when (state.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.OFF
                RepeatMode.ONE -> RepeatMode.OFF
            }
            playbackController?.repeatMode = repeatModeToPlayer(next)
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

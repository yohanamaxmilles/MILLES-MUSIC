package com.example.state

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.JamendoRepository
import com.example.model.Album
import com.example.model.Artist
import com.example.model.ArtworkDescriptor
import com.example.model.AudioQuality
import com.example.model.GenreCategory
import com.example.model.JamendoGenres
import com.example.model.Playlist
import com.example.model.ProviderId
import com.example.model.RepeatMode
import com.example.model.SecondaryDestination
import com.example.model.ThemeMode
import com.example.model.Track
import com.example.playback.MillesMediaController
import com.example.playback.PlaybackEngine
import com.example.playback.PlaybackEngineEvent
import com.example.provider.AudiusMusicProvider
import com.example.provider.JamendoMusicProvider
import com.example.provider.ProviderFailure
import com.example.provider.UnifiedMusicCatalogService
import com.example.provider.YouTubeMusicProvider
import com.example.provider.toDomainPlaylist
import com.example.provider.toDomainTrack
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

    // Playback state (driven by real Media3 MediaController events)
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val playbackSource: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackErrorMessage: String? = null,
    val progressSeconds: Int = 0,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val audioQuality: AudioQuality = AudioQuality.HIGH,

    // Dedicated official YouTube embedded player state (never routed through Media3)
    val activeYouTubeTrack: Track? = null,
    val isYouTubeConfigured: Boolean = false,

    // Live multi-provider discovery & catalog state
    val isCatalogLoading: Boolean = true,
    val catalogErrorMessage: String? = null,
    val popularTracks: List<Track> = emptyList(),
    val recommendedTracks: List<Track> = emptyList(),
    val newReleases: List<Track> = emptyList(),
    val audiusTrendingTracks: List<Track> = emptyList(),
    val audiusTrendingPlaylists: List<Playlist> = emptyList(),
    val recentlyAddedAlbums: List<Album> = emptyList(),
    val recentlyPlayedTracks: List<Track> = emptyList(),
    val relatedTracks: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),

    // Unified Search state
    val searchQuery: String = "",
    val searchProviderFilter: ProviderId? = null,
    val searchResults: List<Track> = emptyList(),
    val searchArtists: List<Artist> = emptyList(),
    val searchAlbums: List<Album> = emptyList(),
    val isSearching: Boolean = false,
    val searchErrorMessage: String? = null,
    val providerSearchNotices: List<ProviderFailure> = emptyList(),

    // Detail screen dynamic API state
    val genreTracksByGenreId: Map<String, List<Track>> = emptyMap(),
    val artistTracksByArtistId: Map<String, List<Track>> = emptyMap(),
    val albumTracksByAlbumId: Map<String, List<Track>> = emptyMap(),
    val detailLoadingKey: String? = null,
    val detailErrorMessage: String? = null,

    // User-authored library state (starts empty; populated only by real user actions)
    val favoriteTrackIds: Set<String> = emptySet(),
    val userPlaylists: List<Playlist> = emptyList(),

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
        get() = currentTrack != null && favoriteTrackIds.contains(currentTrack.id)

    val knownTracks: List<Track>
        get() = (
            listOfNotNull(currentTrack, activeYouTubeTrack) +
                queue +
                popularTracks +
                recommendedTracks +
                newReleases +
                audiusTrendingTracks +
                recentlyPlayedTracks +
                relatedTracks +
                searchResults +
                genreTracksByGenreId.values.flatten() +
                artistTracksByArtistId.values.flatten() +
                albumTracksByAlbumId.values.flatten()
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
    private val jamendoRepository: JamendoRepository,
    private val playbackEngine: PlaybackEngine,
    autoLoadCatalog: Boolean = true,
    private val audiusProvider: AudiusMusicProvider = AudiusMusicProvider(),
    private val youTubeProvider: YouTubeMusicProvider = YouTubeMusicProvider(),
    private val catalogService: UnifiedMusicCatalogService = UnifiedMusicCatalogService(
        jamendoRepository = jamendoRepository,
        jamendoProvider = JamendoMusicProvider(jamendoRepository),
        audiusProvider = audiusProvider,
        youTubeProvider = youTubeProvider
    )
) : ViewModel() {

    val isJamendoConfigured: Boolean
        get() = jamendoRepository.isConfigured

    private val _uiState = MutableStateFlow(
        MillesUiState(
            isYouTubeConfigured = youTubeProvider.isEnabled,
            isCatalogLoading = (jamendoRepository.isConfigured || audiusProvider.isEnabled) && autoLoadCatalog,
            catalogErrorMessage = if (!jamendoRepository.isConfigured && !audiusProvider.isEnabled) {
                "No music providers are currently configured."
            } else null
        )
    )
    val uiState: StateFlow<MillesUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var relatedJob: Job? = null

    init {
        observePlaybackEvents()
        runProgressObserver()
        if (autoLoadCatalog && (jamendoRepository.isConfigured || audiusProvider.isEnabled)) {
            loadMultiProviderCatalog()
        }
    }

    fun retryCatalogLoad() {
        if (!jamendoRepository.isConfigured && !audiusProvider.isEnabled) {
            _uiState.update {
                it.copy(
                    isCatalogLoading = false,
                    catalogErrorMessage = "No music providers are currently configured."
                )
            }
            return
        }
        loadMultiProviderCatalog()
    }

    private fun loadMultiProviderCatalog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCatalogLoading = true, catalogErrorMessage = null) }
            coroutineScope {
                val preferredGenreTag = _uiState.value.favoriteTracks.firstOrNull()?.genre
                    ?: _uiState.value.recentlyPlayedTracks.firstOrNull()?.genre

                val popularDeferred = if (jamendoRepository.isConfigured) {
                    async { jamendoRepository.fetchPopularTracks(limit = 20) }
                } else null
                val recommendedDeferred = if (jamendoRepository.isConfigured) {
                    async { jamendoRepository.fetchRecommendedTracks(genreTag = preferredGenreTag, limit = 15) }
                } else null
                val newReleasesDeferred = if (jamendoRepository.isConfigured) {
                    async { jamendoRepository.fetchNewReleases(limit = 16) }
                } else null
                val recentAlbumsDeferred = if (jamendoRepository.isConfigured) {
                    async { jamendoRepository.fetchAlbums(limit = 12, order = "releasedate_desc") }
                } else null
                val artistsDeferred = if (jamendoRepository.isConfigured) {
                    async { jamendoRepository.fetchArtists(limit = 20) }
                } else null
                val albumsDeferred = if (jamendoRepository.isConfigured) {
                    async { jamendoRepository.fetchAlbums(limit = 20, order = "popularity_week") }
                } else null

                val audiusTrendingDeferred = if (audiusProvider.isEnabled) {
                    async { audiusProvider.fetchTrendingTracks(limit = 18) }
                } else null
                val audiusPlaylistsDeferred = if (audiusProvider.isEnabled) {
                    async { audiusProvider.getPlaylists(limit = 10) }
                } else null

                val popularResult = popularDeferred?.await()
                val recommendedResult = recommendedDeferred?.await()
                val newReleasesResult = newReleasesDeferred?.await()
                val recentAlbumsResult = recentAlbumsDeferred?.await()
                val artistsResult = artistsDeferred?.await()
                val albumsResult = albumsDeferred?.await()
                val audiusTrendingResult = audiusTrendingDeferred?.await()
                val audiusPlaylistsResult = audiusPlaylistsDeferred?.await()

                val popular = popularResult?.getOrNull().orEmpty()
                val recommended = recommendedResult?.getOrNull().orEmpty()
                val newReleases = newReleasesResult?.getOrNull().orEmpty()
                val recentAlbums = recentAlbumsResult?.getOrNull().orEmpty()
                val artists = artistsResult?.getOrNull().orEmpty()
                val albums = albumsResult?.getOrNull().orEmpty()
                val audiusTrending = audiusTrendingResult?.getOrNull().orEmpty()
                    .mapIndexed { idx, unified -> unified.toDomainTrack(index = idx) }
                val audiusPlaylists = audiusPlaylistsResult?.getOrNull().orEmpty()
                    .mapIndexed { idx, unified -> unified.toDomainPlaylist(index = idx) }

                val allTracksEmpty = popular.isEmpty() &&
                    recommended.isEmpty() &&
                    newReleases.isEmpty() &&
                    audiusTrending.isEmpty()

                val firstError = popularResult?.exceptionOrNull()?.message
                    ?: newReleasesResult?.exceptionOrNull()?.message
                    ?: recommendedResult?.exceptionOrNull()?.message
                    ?: audiusTrendingResult?.exceptionOrNull()?.message

                _uiState.update { state ->
                    state.copy(
                        isCatalogLoading = false,
                        catalogErrorMessage = if (allTracksEmpty) {
                            firstError ?: "No tracks returned from music providers."
                        } else null,
                        popularTracks = popular,
                        recommendedTracks = recommended,
                        newReleases = newReleases,
                        audiusTrendingTracks = audiusTrending,
                        audiusTrendingPlaylists = audiusPlaylists,
                        recentlyAddedAlbums = recentAlbums,
                        artists = artists,
                        albums = albums
                    )
                }
            }
        }
    }

    private fun observePlaybackEvents() {
        viewModelScope.launch {
            playbackEngine.events.collect { event ->
                when (event) {
                    is PlaybackEngineEvent.IsPlayingChanged -> {
                        _uiState.update { state ->
                            state.copy(
                                isPlaying = event.isPlaying,
                                playbackErrorMessage = if (event.isPlaying) null else state.playbackErrorMessage
                            )
                        }
                    }

                    is PlaybackEngineEvent.BufferingChanged -> {
                        _uiState.update { it.copy(isBuffering = event.isBuffering) }
                    }

                    is PlaybackEngineEvent.TrackTransitioned -> {
                        val matched = findTrackById(event.trackId)
                        _uiState.update { state ->
                            val target = (matched ?: state.currentTrack)?.let { track ->
                                if (event.durationSeconds != null && event.durationSeconds > 0) {
                                    track.copy(durationSeconds = event.durationSeconds)
                                } else track
                            }
                            val updatedHistory = if (target != null) {
                                (listOf(target) + state.recentlyPlayedTracks.filter { it.id != target.id }).take(20)
                            } else state.recentlyPlayedTracks
                            state.copy(
                                currentTrack = target,
                                progressSeconds = if (state.currentTrack?.id == target?.id) state.progressSeconds else 0,
                                recentlyPlayedTracks = updatedHistory,
                                playbackErrorMessage = null
                            )
                        }
                        if (matched != null) {
                            loadRelatedTracksFor(matched)
                        }
                    }

                    is PlaybackEngineEvent.ShuffleChanged -> {
                        _uiState.update { it.copy(isShuffleEnabled = event.enabled) }
                    }

                    is PlaybackEngineEvent.RepeatChanged -> {
                        _uiState.update { it.copy(repeatMode = event.mode) }
                    }

                    is PlaybackEngineEvent.PlaybackError -> {
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                playbackErrorMessage = event.message
                            )
                        }
                        showStatusBanner(event.message)
                    }

                    PlaybackEngineEvent.QueueEnded -> {
                        _uiState.update {
                            it.copy(
                                isPlaying = false,
                                isBuffering = false,
                                progressSeconds = 0
                            )
                        }
                    }
                }
            }
        }
    }

    private fun runProgressObserver() {
        viewModelScope.launch {
            while (true) {
                delay(500L)
                val snapshot = _uiState.value
                if (snapshot.isPlaying && snapshot.currentTrack != null) {
                    val posSeconds = playbackEngine.currentPositionSeconds()
                    val durSeconds = playbackEngine.currentDurationSeconds()
                    _uiState.update { state ->
                        val updatedTrack = if (durSeconds != null && state.currentTrack != null &&
                            state.currentTrack.durationSeconds != durSeconds
                        ) {
                            state.currentTrack.copy(durationSeconds = durSeconds)
                        } else {
                            state.currentTrack
                        }
                        val maxDur = (updatedTrack?.durationSeconds ?: 1).coerceAtLeast(1)
                        state.copy(
                            currentTrack = updatedTrack,
                            progressSeconds = posSeconds.coerceIn(0, maxDur)
                        )
                    }
                }
            }
        }
    }

    private fun loadRelatedTracksFor(track: Track) {
        if (track.providerId == ProviderId.YOUTUBE || track.isVideoEmbedOnly) return
        relatedJob?.cancel()
        relatedJob = viewModelScope.launch {
            when (track.providerId) {
                ProviderId.AUDIUS -> {
                    audiusProvider.searchTracks(track.genre, limit = 10).onSuccess { unified ->
                        val filtered = unified
                            .mapIndexed { idx, u -> u.toDomainTrack(index = idx) }
                            .filter { it.id != track.id && it.streamingPermitted }
                            .take(8)
                        _uiState.update { it.copy(relatedTracks = filtered) }
                    }
                }
                ProviderId.JAMENDO -> {
                    if (!jamendoRepository.isConfigured) return@launch
                    val tag = JamendoGenres.jamendoTagForGenre("", track.genre)
                    jamendoRepository.fetchTracksByGenre(tag, limit = 12).onSuccess { tracks ->
                        val filtered = tracks.filter { it.id != track.id }.take(8)
                        _uiState.update { it.copy(relatedTracks = filtered) }
                    }
                }
                else -> Unit
            }
        }
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
                isNowPlayingOpen = false,
                detailErrorMessage = null
            )
        }
        when (destination) {
            is SecondaryDestination.GenreDetail -> loadGenreTracks(destination.genreId)
            is SecondaryDestination.ArtistDetail -> loadArtistTracks(destination.artistId)
            is SecondaryDestination.AlbumDetail -> loadAlbumTracks(destination.albumId)
            else -> Unit
        }
    }

    fun retrySecondaryLoad(destination: SecondaryDestination) {
        when (destination) {
            is SecondaryDestination.GenreDetail -> loadGenreTracks(destination.genreId, force = true)
            is SecondaryDestination.ArtistDetail -> loadArtistTracks(destination.artistId, force = true)
            is SecondaryDestination.AlbumDetail -> loadAlbumTracks(destination.albumId, force = true)
            else -> Unit
        }
    }

    private fun loadGenreTracks(genreId: String, force: Boolean = false) {
        if (!jamendoRepository.isConfigured) return
        if (!force && _uiState.value.genreTracksByGenreId.containsKey(genreId)) return
        val genre = findGenre(genreId) ?: return
        val tag = JamendoGenres.jamendoTagForGenre(genre.id, genre.name)
        viewModelScope.launch {
            _uiState.update { it.copy(detailLoadingKey = genreId, detailErrorMessage = null) }
            jamendoRepository.fetchTracksByGenre(tag).fold(
                onSuccess = { tracks ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoadingKey = if (state.detailLoadingKey == genreId) null else state.detailLoadingKey,
                            genreTracksByGenreId = state.genreTracksByGenreId + (genreId to tracks)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoadingKey = if (state.detailLoadingKey == genreId) null else state.detailLoadingKey,
                            detailErrorMessage = error.message ?: "Unable to load genre tracks."
                        )
                    }
                }
            )
        }
    }

    private fun loadArtistTracks(artistId: String, force: Boolean = false) {
        if (!force && _uiState.value.artistTracksByArtistId.containsKey(artistId)) return
        val isAudius = artistId.startsWith(AudiusMusicProvider.ARTIST_PREFIX)
        if (!isAudius && !jamendoRepository.isConfigured) return

        viewModelScope.launch {
            _uiState.update { it.copy(detailLoadingKey = artistId, detailErrorMessage = null) }
            val result = if (isAudius) {
                audiusProvider.getArtistDetails(artistId).map { (_, tracks) ->
                    tracks.mapIndexed { idx, u -> u.toDomainTrack(index = idx) }
                }
            } else {
                jamendoRepository.fetchTracksByArtist(artistId)
            }
            result.fold(
                onSuccess = { tracks ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoadingKey = if (state.detailLoadingKey == artistId) null else state.detailLoadingKey,
                            artistTracksByArtistId = state.artistTracksByArtistId + (artistId to tracks)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoadingKey = if (state.detailLoadingKey == artistId) null else state.detailLoadingKey,
                            detailErrorMessage = error.message ?: "Unable to load artist tracks."
                        )
                    }
                }
            )
        }
    }

    private fun loadAlbumTracks(albumId: String, force: Boolean = false) {
        if (!force && _uiState.value.albumTracksByAlbumId.containsKey(albumId)) return
        val isAudius = albumId.startsWith(AudiusMusicProvider.ALBUM_PREFIX) ||
            albumId.startsWith(AudiusMusicProvider.PLAYLIST_PREFIX)
        if (!isAudius && !jamendoRepository.isConfigured) return

        viewModelScope.launch {
            _uiState.update { it.copy(detailLoadingKey = albumId, detailErrorMessage = null) }
            val result = if (isAudius) {
                audiusProvider.getAlbumDetails(albumId).map { (_, tracks) ->
                    tracks.mapIndexed { idx, u -> u.toDomainTrack(index = idx) }
                }
            } else {
                jamendoRepository.fetchTracksByAlbum(albumId)
            }
            result.fold(
                onSuccess = { tracks ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoadingKey = if (state.detailLoadingKey == albumId) null else state.detailLoadingKey,
                            albumTracksByAlbumId = state.albumTracksByAlbumId + (albumId to tracks)
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            detailLoadingKey = if (state.detailLoadingKey == albumId) null else state.detailLoadingKey,
                            detailErrorMessage = error.message ?: "Unable to load album tracks."
                        )
                    }
                }
            )
        }
    }

    fun navigateBack(): Boolean {
        val state = _uiState.value
        return when {
            state.activeYouTubeTrack != null -> {
                _uiState.update { it.copy(activeYouTubeTrack = null) }
                true
            }
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
                _uiState.update {
                    it.copy(
                        secondaryStack = it.secondaryStack.dropLast(1),
                        detailLoadingKey = null,
                        detailErrorMessage = null
                    )
                }
                true
            }
            else -> false
        }
    }

    fun closeYouTubePlayer() {
        _uiState.update { it.copy(activeYouTubeTrack = null) }
    }

    fun openNowPlaying() {
        if (_uiState.value.currentTrack != null) {
            _uiState.update { it.copy(isNowPlayingOpen = true) }
        }
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
        val active = snapshot.currentTrack
        if (active == null) {
            playHeroMix()
            return
        }
        if (snapshot.isPlaying) {
            playbackEngine.pause()
        } else {
            if (playbackEngine.isPlaying() || snapshot.progressSeconds > 0) {
                playbackEngine.play()
            } else {
                val q = snapshot.queue.ifEmpty { listOf(active) }
                val idx = q.indexOfFirst { it.id == active.id }.coerceAtLeast(0)
                playbackEngine.setQueueAndPlay(q, idx)
            }
        }
    }

    fun playTrack(
        track: Track,
        customQueue: List<Track>? = null,
        source: String? = null,
        openPlayer: Boolean = false
    ) {
        // Strict YouTube policy isolation: open dedicated official WebView embed, pause Media3, never enqueue in Media3
        if (track.providerId == ProviderId.YOUTUBE || track.isVideoEmbedOnly) {
            playbackEngine.pause()
            _uiState.update { current ->
                val updatedHistory = (listOf(track) + current.recentlyPlayedTracks.filter { it.id != track.id }).take(20)
                current.copy(
                    activeYouTubeTrack = track,
                    isNowPlayingOpen = false,
                    isPlaying = false,
                    recentlyPlayedTracks = updatedHistory
                )
            }
            return
        }

        if (!track.streamingPermitted || track.streamUrl.isNullOrBlank()) {
            showStatusBanner("Streaming is restricted or unavailable for this ${track.providerId.displayName} track.")
            return
        }

        val state = _uiState.value
        val fallbackPool = state.popularTracks.ifEmpty { state.audiusTrendingTracks }.ifEmpty { listOf(track) }
        val audioQueue = (customQueue ?: fallbackPool)
            .filter { it.streamingPermitted && !it.isVideoEmbedOnly && !it.streamUrl.isNullOrBlank() }
        val targetQueue = if (audioQueue.any { it.id == track.id }) audioQueue else listOf(track) + audioQueue
        val startIndex = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _uiState.update { current ->
            val updatedHistory = (listOf(track) + current.recentlyPlayedTracks.filter { it.id != track.id }).take(20)
            current.copy(
                activeYouTubeTrack = null,
                currentTrack = track,
                queue = targetQueue,
                playbackSource = source ?: "${track.providerId.displayName} · ${track.album}",
                isBuffering = true,
                playbackErrorMessage = null,
                progressSeconds = if (current.currentTrack?.id == track.id) current.progressSeconds else 0,
                isNowPlayingOpen = openPlayer || current.isNowPlayingOpen,
                recentlyPlayedTracks = updatedHistory
            )
        }
        playbackEngine.setQueueAndPlay(targetQueue, startIndex)
        loadRelatedTracksFor(track)
    }

    fun playHeroMix() {
        val state = _uiState.value
        val mix = state.popularTracks
            .ifEmpty { state.newReleases }
            .ifEmpty { state.audiusTrendingTracks }
        if (mix.isEmpty()) {
            showStatusBanner("No audio tracks loaded yet.")
            return
        }
        val sourceLabel = if (state.popularTracks.isNotEmpty()) {
            "Popular on Jamendo"
        } else {
            "Trending on Audius"
        }
        playTrack(mix.first(), customQueue = mix, source = sourceLabel, openPlayer = true)
    }

    fun playTracksCollection(tracks: List<Track>, source: String, shuffled: Boolean, openPlayer: Boolean = false) {
        if (tracks.isEmpty()) return
        val audioTracks = tracks.filter { it.streamingPermitted && !it.isVideoEmbedOnly && !it.streamUrl.isNullOrBlank() }
        if (audioTracks.isEmpty()) {
            val firstVideo = tracks.firstOrNull { it.isVideoEmbedOnly || it.providerId == ProviderId.YOUTUBE }
            if (firstVideo != null) {
                playTrack(firstVideo)
            } else {
                showStatusBanner("No streamable audio tracks in this collection.")
            }
            return
        }
        val ordered = if (shuffled) audioTracks.shuffled() else audioTracks
        _uiState.update { it.copy(isShuffleEnabled = shuffled) }
        playbackEngine.setShuffleEnabled(shuffled)
        playTrack(ordered.first(), customQueue = ordered, source = source, openPlayer = openPlayer)
    }

    fun seekTo(seconds: Int) {
        val track = _uiState.value.currentTrack ?: return
        val clamped = seconds.coerceIn(0, track.durationSeconds.coerceAtLeast(1))
        _uiState.update { it.copy(progressSeconds = clamped) }
        playbackEngine.seekToMs(clamped * 1000L)
    }

    fun seekMs(positionMs: Long) {
        val track = _uiState.value.currentTrack ?: return
        val maxMs = track.durationSeconds.coerceAtLeast(1) * 1000L
        val clampedMs = positionMs.coerceIn(0L, maxMs)
        _uiState.update { it.copy(progressSeconds = (clampedMs / 1000L).toInt()) }
        playbackEngine.seekToMs(clampedMs)
    }

    fun skipToNext() {
        playbackEngine.seekToNext()
    }

    fun skipToPrevious() {
        playbackEngine.seekToPrevious()
    }

    fun toggleShuffle() {
        val next = !_uiState.value.isShuffleEnabled
        _uiState.update { it.copy(isShuffleEnabled = next) }
        playbackEngine.setShuffleEnabled(next)
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _uiState.update { it.copy(repeatMode = next) }
        playbackEngine.setRepeatMode(next)
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

    fun selectSearchProviderFilter(providerFilter: ProviderId?) {
        _uiState.update { it.copy(searchProviderFilter = providerFilter) }
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            updateSearchQuery(currentQuery)
        } else if (providerFilter == ProviderId.YOUTUBE && !youTubeProvider.isEnabled) {
            _uiState.update {
                it.copy(
                    providerSearchNotices = listOf(
                        ProviderFailure(
                            providerId = ProviderId.YOUTUBE,
                            message = "Add YOUTUBE_API_KEY in AI Studio Secrets to enable YouTube music video search.",
                            isUnconfigured = true
                        )
                    )
                )
            }
        } else {
            _uiState.update { it.copy(providerSearchNotices = emptyList()) }
        }
    }

    fun updateSearchQuery(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        val activeFilter = _uiState.value.searchProviderFilter
        if (trimmed.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchQuery = query,
                    searchResults = emptyList(),
                    searchArtists = emptyList(),
                    searchAlbums = emptyList(),
                    isSearching = false,
                    searchErrorMessage = null,
                    providerSearchNotices = emptyList()
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearching = true,
                searchErrorMessage = null,
                providerSearchNotices = emptyList()
            )
        }
        searchJob = viewModelScope.launch {
            delay(350L)
            val result = catalogService.search(query = trimmed, providerFilter = activeFilter)
            val allEmpty = result.tracks.isEmpty() && result.artists.isEmpty() && result.albums.isEmpty()
            val errorMsg = if (allEmpty && result.providerFailures.isNotEmpty()) {
                result.providerFailures.joinToString(" · ") { "${it.providerId.displayName}: ${it.message}" }
            } else null

            _uiState.update { state ->
                if (state.searchQuery.trim() == trimmed) {
                    state.copy(
                        searchResults = result.tracks,
                        searchArtists = result.artists,
                        searchAlbums = result.albums,
                        isSearching = false,
                        searchErrorMessage = errorMsg,
                        providerSearchNotices = result.providerFailures
                    )
                } else state
            }
        }
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
        showStatusBanner("Streaming preference: ${quality.title}")
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
        val cleanSubtitle = subtitle.trim().ifEmpty { "Personal playlist" }
        val activeTrack = _uiState.value.currentTrack
        val initialTracks = if (activeTrack != null) listOf(activeTrack.id) else emptyList()
        val newPlaylist = Playlist(
            id = "pl_user_${System.currentTimeMillis()}",
            title = cleanTitle,
            subtitle = cleanSubtitle,
            trackIds = initialTracks,
            artwork = ArtworkDescriptor(
                imageRes = activeTrack?.artwork?.imageRes,
                primaryColor = androidx.compose.ui.graphics.Color(0xFF282B20),
                secondaryColor = androidx.compose.ui.graphics.Color(0xFF050505),
                accentColor = androidx.compose.ui.graphics.Color(0xFFC6F24E),
                patternVariant = (_uiState.value.userPlaylists.size % 6) + 1
            ),
            displayedSongCount = initialTracks.size,
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
            delay(2500L)
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

    fun findGenre(id: String): GenreCategory? =
        JamendoGenres.genres.find { it.id == id }

    fun findArtist(id: String): Artist? =
        (_uiState.value.artists + _uiState.value.searchArtists).find { it.id == id }

    fun findAlbum(id: String): Album? =
        (_uiState.value.albums + _uiState.value.recentlyAddedAlbums + _uiState.value.searchAlbums)
            .find { it.id == id }

    override fun onCleared() {
        playbackEngine.release()
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val jamendoRepo = JamendoRepository()
                    val audius = AudiusMusicProvider()
                    val youtube = YouTubeMusicProvider()
                    val catalog = UnifiedMusicCatalogService(
                        jamendoRepository = jamendoRepo,
                        jamendoProvider = JamendoMusicProvider(jamendoRepo),
                        audiusProvider = audius,
                        youTubeProvider = youtube
                    )
                    return MillesMusicViewModel(
                        jamendoRepository = jamendoRepo,
                        playbackEngine = MillesMediaController(appContext),
                        autoLoadCatalog = true,
                        audiusProvider = audius,
                        youTubeProvider = youtube,
                        catalogService = catalog
                    ) as T
                }
            }
        }
    }
}

/** Maps domain Track into the UI Track model used by the /ui composables. */
fun Track.toUiTrack(): com.example.ui.Track = com.example.ui.Track(
    id = id,
    title = title,
    artist = artist,
    coverUrl = coverUrl ?: artwork.imageRes,
    durationMs = durationSeconds * 1000L,
    album = album,
    attribution = attributionLabel,
    licenseUrl = licenseUrl ?: originalProviderUrl,
    providerId = providerId,
    providerLabel = providerId.displayName,
    isVideoEmbedOnly = isVideoEmbedOnly
)

fun Playlist.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = title,
    subtitle = subtitle,
    coverUrl = coverUrl ?: artwork.imageRes,
    providerId = providerId
)

fun Album.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = title,
    subtitle = listOfNotNull(artist, releaseDate?.take(4)).joinToString(" · "),
    coverUrl = coverUrl ?: artwork.imageRes,
    providerId = providerId
)

fun Artist.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = name,
    subtitle = subtitle,
    coverUrl = imageUrl ?: artwork.imageRes,
    providerId = providerId
)

fun GenreCategory.toUiCollection(): com.example.ui.Collection = com.example.ui.Collection(
    id = id,
    title = name,
    subtitle = subtitle,
    coverUrl = artwork.imageRes,
    providerId = ProviderId.JAMENDO
)

fun MillesUiState.toPlayerUiState(): PlayerUiState? {
    val active = currentTrack ?: return null
    val fallbackRelated = (recommendedTracks + popularTracks + audiusTrendingTracks)
        .distinctBy { it.id }
        .filter { it.id != active.id && it.streamingPermitted }
        .take(8)
    val resolvedRelated = relatedTracks.ifEmpty { fallbackRelated }
    return PlayerUiState(
        track = active.toUiTrack(),
        isPlaying = isPlaying,
        isBuffering = isBuffering,
        playbackError = playbackErrorMessage,
        positionMs = progressSeconds * 1000L,
        isLiked = isCurrentTrackFavorite,
        shuffle = isShuffleEnabled,
        repeat = repeatMode != RepeatMode.OFF,
        repeatOne = repeatMode == RepeatMode.ONE,
        quality = audioQuality.shortLabel,
        source = playbackSource,
        queue = queue.map { it.toUiTrack() },
        related = resolvedRelated.map { it.toUiTrack() },
        lyrics = emptyList()
    )
}

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

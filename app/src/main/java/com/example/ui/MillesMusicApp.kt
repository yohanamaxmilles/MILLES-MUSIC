package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.session.MediaController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.SampleCatalog
import com.example.model.SecondaryDestination
import com.example.state.MillesMusicViewModel
import com.example.state.MillesUiState
import com.example.state.toPlayerUiState
import com.example.state.toUiCollection
import com.example.state.toUiTrack
import com.example.ui.components.MiniPlayer
import com.example.ui.components.MillesTab
import com.example.ui.screens.AudioQualitySheet
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.CreatePlaylistModal
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LibrarySegment
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlayerActions
import com.example.ui.screens.ProfileDetailSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.ThemeScreen
import com.example.ui.screens.TrackOptionsSheet
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles
import com.example.ui.theme.MillesTheme

@Composable
fun MillesMusicApp(
    viewModel: MillesMusicViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(viewModel, context) {
        val future = com.example.playback.MillesPlaybackConnector.connect(context)
        future.addListener({
            runCatching { future.get() }
                .onSuccess(viewModel::attachPlaybackController)
                .onFailure { viewModel.showStatusBanner("Unable to connect to audio player") }
        }, context.mainExecutor)
        onDispose {
            viewModel.detachPlaybackController()
            MediaController.releaseFuture(future)
        }
    }

    MillesTheme(themeMode = uiState.themeMode) {
        val palette = LocalMillesPalette.current
        val playerUiState = remember(uiState) { uiState.toPlayerUiState() }

        val playerActions = remember(viewModel) {
            object : PlayerActions {
                override fun onBack() = viewModel.closeNowPlaying()
                override fun onPlayPause() = viewModel.togglePlayPause()
                override fun onNext() = viewModel.skipToNext(autoTriggered = false)
                override fun onPrevious() = viewModel.skipToPrevious()
                override fun onSeek(positionMs: Long) = viewModel.seekMs(positionMs)
                override fun onLike() = viewModel.toggleFavorite(viewModel.uiState.value.currentTrack.id)
                override fun onShuffle() = viewModel.toggleShuffle()
                override fun onRepeat() = viewModel.cycleRepeatMode()
                override fun onMore() = viewModel.openTrackOptions(viewModel.uiState.value.currentTrack)
                override fun onQualityClick() = viewModel.setAudioQualitySheetOpen(true)
                override fun onTrackClick(track: Track) {
                    SampleCatalog.trackById(track.id)?.let { domainTrack ->
                        viewModel.playTrack(domainTrack, openPlayer = true)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            val activeSecondary = uiState.activeSecondary
            val fullScreenSecondary = activeSecondary is SecondaryDestination.Profile ||
                activeSecondary is SecondaryDestination.ThemeSettings

            if (fullScreenSecondary) {
                when (activeSecondary) {
                    is SecondaryDestination.Profile -> {
                        ProfileScreen(
                            themeMode = uiState.themeMode,
                            onBack = { viewModel.navigateBack() },
                            onOpenThemeSettings = { viewModel.navigateTo(SecondaryDestination.ThemeSettings) },
                            onOpenSheet = viewModel::openProfileSheet,
                            onLogoutNotice = { viewModel.showStatusBanner("Account sync arrives in Phase 2") }
                        )
                    }
                    is SecondaryDestination.ThemeSettings -> {
                        ThemeScreen(
                            themeMode = uiState.themeMode,
                            onSelectThemeMode = viewModel::selectThemeMode,
                            onToggleDynamicTheme = viewModel::toggleDynamicTheme,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    else -> Unit
                }
            } else {
                MillesShell(
                    tab = uiState.activeTab,
                    onTab = viewModel::selectTab,
                    miniPlayer = {
                        val progress = (uiState.progressSeconds.toFloat() /
                            uiState.currentTrack.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                        MiniPlayer(
                            track = playerUiState.track,
                            isPlaying = uiState.isPlaying,
                            progress = progress,
                            isLiked = uiState.isCurrentTrackFavorite,
                            onOpen = viewModel::openNowPlaying,
                            onPlayPause = viewModel::togglePlayPause,
                            onLike = { viewModel.toggleFavorite(uiState.currentTrack.id) }
                        )
                    }
                ) { bottomInset ->
                    AnimatedContent(
                        targetState = Pair(uiState.activeTab, activeSecondary),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "shell_content"
                    ) { (tab, secondary) ->
                        if (secondary != null) {
                            DetailContent(
                                destination = secondary,
                                uiState = uiState,
                                viewModel = viewModel,
                                bottomInset = bottomInset
                            )
                        } else {
                            TabContent(
                                tab = tab,
                                uiState = uiState,
                                viewModel = viewModel,
                                bottomInset = bottomInset
                            )
                        }
                    }
                }
            }

            // Full-screen Now Playing Slide-Up Overlay
            AnimatedVisibility(
                visible = uiState.isNowPlayingOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    state = playerUiState,
                    actions = playerActions
                )
            }

            // Modal Overlay Sheets
            if (uiState.isCreatePlaylistOpen) {
                CreatePlaylistModal(
                    onDismiss = { viewModel.setCreatePlaylistOpen(false) },
                    onCreate = viewModel::createPlaylist
                )
            }

            if (uiState.isAudioQualitySheetOpen) {
                AudioQualitySheet(
                    selectedQuality = uiState.audioQuality,
                    onSelectQuality = viewModel::setAudioQuality,
                    onDismiss = { viewModel.setAudioQualitySheetOpen(false) }
                )
            }

            uiState.trackOptionsTarget?.let { targetTrack ->
                TrackOptionsSheet(
                    track = targetTrack,
                    isFavorite = uiState.favoriteTrackIds.contains(targetTrack.id),
                    playlists = uiState.userPlaylists,
                    onToggleFavorite = { viewModel.toggleFavorite(targetTrack.id) },
                    onAddToPlaylist = { playlistId -> viewModel.addTrackToPlaylist(targetTrack, playlistId) },
                    onDismiss = { viewModel.openTrackOptions(null) }
                )
            }

            uiState.profileDetailSheet?.let { sheetType ->
                ProfileDetailSheet(
                    type = sheetType,
                    notificationsEnabled = uiState.notificationsEnabled,
                    audioQuality = uiState.audioQuality,
                    onToggleNotifications = viewModel::toggleNotifications,
                    onOpenAudioQuality = { viewModel.setAudioQualitySheetOpen(true) },
                    onDismiss = { viewModel.openProfileSheet(null) }
                )
            }

            // Floating Status Pill Toast
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp)
            ) {
                uiState.statusBannerMessage?.let { message ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.surfaceHigh)
                            .border(1.dp, Milles.Lime.copy(alpha = 0.7f), CircleShape)
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    tab: MillesTab,
    uiState: MillesUiState,
    viewModel: MillesMusicViewModel,
    bottomInset: androidx.compose.ui.unit.Dp
) {
    val trendingTracks = remember {
        SampleCatalog.tracks.filter { it.isTrending }.map { it.toUiTrack() }
    }
    val madeForYouTracks = remember {
        SampleCatalog.tracks.reversed().take(6).map { it.toUiTrack() }
    }
    val newReleases = remember {
        SampleCatalog.tracks.filter { it.isNewRelease }.map { it.toUiTrack() }
    }
    val topCharts = remember {
        SampleCatalog.tracks.map { it.toUiTrack() }
    }

    when (tab) {
        MillesTab.Home -> {
            HomeScreen(
                heroCover = R.drawable.img_hero_music,
                avatar = R.drawable.img_cover_midnight,
                trending = trendingTracks,
                madeForYou = madeForYouTracks,
                isHeroPlaying = uiState.isPlaying && uiState.playbackSource == "Good Music, Every Day",
                onPlayHero = viewModel::playHeroMix,
                onTrack = { uiTrack ->
                    SampleCatalog.trackById(uiTrack.id)?.let { track ->
                        viewModel.playTrack(track, source = "Trending Now")
                    }
                },
                onProfile = { viewModel.navigateTo(SecondaryDestination.Profile) },
                onSeeAllTrending = {
                    viewModel.navigateTo(
                        SecondaryDestination.CollectionOverview(
                            title = "Trending Now",
                            trackIds = SampleCatalog.tracks.filter { it.isTrending }.map { it.id }
                        )
                    )
                },
                onSeeAllMadeForYou = {
                    viewModel.navigateTo(
                        SecondaryDestination.CollectionOverview(
                            title = "Made for You",
                            trackIds = SampleCatalog.tracks.map { it.id }
                        )
                    )
                },
                bottomInset = bottomInset
            )
        }

        MillesTab.Explore -> {
            ExploreScreen(
                newReleases = newReleases,
                topCharts = topCharts,
                activeTrackId = uiState.currentTrack.id,
                onTrack = { uiTrack ->
                    SampleCatalog.trackById(uiTrack.id)?.let { track ->
                        viewModel.playTrack(track, source = "Top Charts")
                    }
                },
                onMoreTrack = { uiTrack ->
                    SampleCatalog.trackById(uiTrack.id)?.let(viewModel::openTrackOptions)
                },
                bottomInset = bottomInset
            )
        }

        MillesTab.Library -> {
            val likedUi = remember(uiState.favoriteTracks) { uiState.favoriteTracks.map { it.toUiTrack() } }
            val playlistCols = remember(uiState.userPlaylists) { uiState.userPlaylists.map { it.toUiCollection() } }
            val albumCols = remember { SampleCatalog.albums.map { it.toUiCollection() } }
            val artistCols = remember { SampleCatalog.artists.map { it.toUiCollection() } }

            LibraryScreen(
                liked = likedUi,
                playlists = playlistCols,
                albums = albumCols,
                artists = artistCols,
                activeTrackId = uiState.currentTrack.id,
                onTrack = { uiTrack ->
                    SampleCatalog.trackById(uiTrack.id)?.let { track ->
                        viewModel.playTrack(track, customQueue = uiState.favoriteTracks, source = "Liked Songs")
                    }
                },
                onMoreTrack = { uiTrack ->
                    SampleCatalog.trackById(uiTrack.id)?.let(viewModel::openTrackOptions)
                },
                onCollection = { collection, seg ->
                    val dest = when (seg) {
                        LibrarySegment.Playlists -> SecondaryDestination.PlaylistDetail(collection.id)
                        LibrarySegment.Albums -> SecondaryDestination.AlbumDetail(collection.id)
                        LibrarySegment.Artists -> SecondaryDestination.ArtistDetail(collection.id)
                        LibrarySegment.Liked -> null
                    }
                    dest?.let(viewModel::navigateTo)
                },
                onCreatePlaylist = { viewModel.setCreatePlaylistOpen(true) },
                onPlayLiked = {
                    viewModel.playTracksCollection(uiState.favoriteTracks, source = "Liked Songs", shuffled = false)
                },
                onShuffleLiked = {
                    viewModel.playTracksCollection(uiState.favoriteTracks, source = "Liked Songs", shuffled = true)
                },
                bottomInset = bottomInset
            )
        }

        MillesTab.Search -> {
            val query = uiState.searchQuery
            val results = uiState.searchResults.map { it.toUiTrack() }
            val genreCols = remember { SampleCatalog.genres.map { it.toUiCollection() } }

            SearchScreen(
                query = query,
                onQuery = viewModel::updateSearchQuery,
                results = results,
                genres = genreCols,
                activeTrackId = uiState.currentTrack.id,
                onTrack = { uiTrack ->
                    if (uiTrack.id.startsWith("jamendo_")) {
                        viewModel.playSearchResult(uiTrack.id)
                    } else {
                        SampleCatalog.trackById(uiTrack.id)?.let { track ->
                            viewModel.playTrack(track, source = "Search")
                        }
                    }
                },
                onMoreTrack = { uiTrack ->
                    if (uiTrack.id.startsWith("jamendo_")) {
                        viewModel.openSearchResultOptions(uiTrack.id)
                    } else {
                        SampleCatalog.trackById(uiTrack.id)?.let(viewModel::openTrackOptions)
                    }
                },
                onGenre = { genreCol ->
                    viewModel.navigateTo(SecondaryDestination.GenreDetail(genreCol.id))
                },
                isSearching = uiState.isSearching,
                errorMessage = uiState.searchError,
                bottomInset = bottomInset
            )
        }
    }
}

@Composable
private fun DetailContent(
    destination: SecondaryDestination,
    uiState: MillesUiState,
    viewModel: MillesMusicViewModel,
    bottomInset: androidx.compose.ui.unit.Dp
) {
    when (destination) {
        is SecondaryDestination.PlaylistDetail -> {
            val playlist = viewModel.findPlaylist(destination.playlistId)
            val tracks = SampleCatalog.tracksByIds(playlist?.trackIds.orEmpty()).ifEmpty { SampleCatalog.tracks.take(4) }
            CollectionDetailScreen(
                title = playlist?.title ?: "Playlist",
                subtitle = playlist?.subtitle ?: "",
                coverUrl = playlist?.artwork?.imageRes ?: R.drawable.img_cover_afro,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    viewModel.playTracksCollection(tracks, source = playlist?.title ?: "Playlist", shuffled = shuffled, openPlayer = true)
                },
                onPlayTrack = { track ->
                    viewModel.playTrack(track, customQueue = tracks, source = playlist?.title)
                },
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomInset = bottomInset
            )
        }

        is SecondaryDestination.GenreDetail -> {
            val genre = viewModel.findGenre(destination.genreId)
            val tracks = SampleCatalog.tracksByIds(genre?.trackIds.orEmpty()).ifEmpty { SampleCatalog.tracks.take(4) }
            CollectionDetailScreen(
                title = genre?.name ?: "Genre",
                subtitle = genre?.subtitle ?: "",
                coverUrl = genre?.artwork?.imageRes ?: R.drawable.img_cover_afro,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    viewModel.playTracksCollection(tracks, source = genre?.name ?: "Genre", shuffled = shuffled, openPlayer = true)
                },
                onPlayTrack = { track ->
                    viewModel.playTrack(track, customQueue = tracks, source = genre?.name)
                },
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomInset = bottomInset
            )
        }

        is SecondaryDestination.ArtistDetail -> {
            val artist = viewModel.findArtist(destination.artistId)
            val tracks = SampleCatalog.tracks.filter {
                artist != null && it.artist.contains(artist.name, ignoreCase = true)
            }.ifEmpty { SampleCatalog.tracks.take(3) }
            CollectionDetailScreen(
                title = artist?.name ?: "Artist",
                subtitle = artist?.monthlyListeners ?: "",
                coverUrl = artist?.artwork?.imageRes ?: R.drawable.img_cover_midnight,
                tracks = tracks,
                isCircularArtwork = true,
                currentTrackId = uiState.currentTrack.id,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    viewModel.playTracksCollection(tracks, source = artist?.name ?: "Artist", shuffled = shuffled, openPlayer = true)
                },
                onPlayTrack = { track ->
                    viewModel.playTrack(track, customQueue = tracks, source = artist?.name)
                },
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomInset = bottomInset
            )
        }

        is SecondaryDestination.AlbumDetail -> {
            val album = viewModel.findAlbum(destination.albumId)
            val tracks = SampleCatalog.tracksByIds(album?.trackIds.orEmpty()).ifEmpty { SampleCatalog.tracks.take(3) }
            CollectionDetailScreen(
                title = album?.title ?: "Album",
                subtitle = "${album?.artist ?: ""} · ${album?.year ?: 2026}",
                coverUrl = album?.artwork?.imageRes ?: R.drawable.img_cover_chill,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    viewModel.playTracksCollection(tracks, source = album?.title ?: "Album", shuffled = shuffled, openPlayer = true)
                },
                onPlayTrack = { track ->
                    viewModel.playTrack(track, customQueue = tracks, source = album?.title)
                },
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomInset = bottomInset
            )
        }

        is SecondaryDestination.CollectionOverview -> {
            val tracks = SampleCatalog.tracksByIds(destination.trackIds).ifEmpty { SampleCatalog.tracks }
            CollectionDetailScreen(
                title = destination.title,
                subtitle = "${tracks.size} curated tracks",
                coverUrl = tracks.first().artwork.imageRes ?: R.drawable.img_hero_music,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    viewModel.playTracksCollection(tracks, source = destination.title, shuffled = shuffled, openPlayer = true)
                },
                onPlayTrack = { track ->
                    viewModel.playTrack(track, customQueue = tracks, source = destination.title)
                },
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomInset = bottomInset
            )
        }

        else -> Unit
    }
}

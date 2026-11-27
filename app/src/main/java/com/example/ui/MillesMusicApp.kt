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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.MainTab
import com.example.model.SampleCatalog
import com.example.model.SecondaryDestination
import com.example.state.MillesMusicViewModel
import com.example.state.MillesUiState
import com.example.ui.components.MillesBottomNavBar
import com.example.ui.components.MillesMiniPlayer
import com.example.ui.components.MillesNavigationRail
import com.example.ui.screens.AudioQualitySheet
import com.example.ui.screens.CollectionDetailScreen
import com.example.ui.screens.CreatePlaylistModal
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.ProfileDetailSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QueueBottomSheet
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ThemeScreen
import com.example.ui.screens.TrackOptionsSheet
import com.example.ui.theme.MillesMusicTheme
import com.example.ui.theme.MillesRadius
import com.example.ui.theme.MillesTheme

@Composable
fun MillesMusicApp(
    viewModel: MillesMusicViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MillesMusicTheme(themeMode = uiState.themeMode) {
        val colors = MillesTheme.colors

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.backgroundMain)
        ) {
            if (uiState.isSplashVisible) {
                SplashScreen(progress = uiState.splashProgress)
            } else {
                MainShell(uiState = uiState, viewModel = viewModel)
            }

            // Full-screen Now Playing Player Slide-Up Overlay
            AnimatedVisibility(
                visible = !uiState.isSplashVisible && uiState.isNowPlayingOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NowPlayingScreen(
                    track = uiState.currentTrack,
                    queue = uiState.queue,
                    relatedTracks = SampleCatalog.tracks.filter { it.id != uiState.currentTrack.id }.take(8),
                    isPlaying = uiState.isPlaying,
                    progressSeconds = uiState.progressSeconds,
                    isFavorite = uiState.isCurrentTrackFavorite,
                    isShuffleEnabled = uiState.isShuffleEnabled,
                    repeatMode = uiState.repeatMode,
                    audioQuality = uiState.audioQuality,
                    onMinimize = viewModel::closeNowPlaying,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeek = viewModel::seekTo,
                    onNext = { viewModel.skipToNext(autoTriggered = false) },
                    onPrevious = viewModel::skipToPrevious,
                    onToggleFavorite = { viewModel.toggleFavorite(uiState.currentTrack.id) },
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onOpenQueue = { viewModel.setQueueSheetOpen(true) },
                    onOpenAudioQuality = { viewModel.setAudioQualitySheetOpen(true) },
                    onOpenTrackOptions = { viewModel.openTrackOptions(uiState.currentTrack) }
                )
            }

            // Modal Overlay Sheets
            if (uiState.isCreatePlaylistOpen) {
                CreatePlaylistModal(
                    onDismiss = { viewModel.setCreatePlaylistOpen(false) },
                    onCreate = viewModel::createPlaylist
                )
            }

            if (uiState.isQueueSheetOpen) {
                QueueBottomSheet(
                    queue = uiState.queue,
                    currentTrackId = uiState.currentTrack.id,
                    isPlaying = uiState.isPlaying,
                    onSelectTrack = { track ->
                        viewModel.playTrack(track)
                        viewModel.setQueueSheetOpen(false)
                    },
                    onDismiss = { viewModel.setQueueSheetOpen(false) }
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
                            .clip(RoundedCornerShape(MillesRadius.Pill))
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.lime.copy(alpha = 0.65f), RoundedCornerShape(MillesRadius.Pill))
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MainShell(
    uiState: MillesUiState,
    viewModel: MillesMusicViewModel
) {
    val colors = MillesTheme.colors
    val activeSecondary = uiState.activeSecondary
    val showBottomNavigation = activeSecondary !is SecondaryDestination.ThemeSettings

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 720.dp
        val bottomScrollPadding = if (showBottomNavigation && !isWideLayout) 168.dp else 104.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideLayout && showBottomNavigation) {
                MillesNavigationRail(
                    activeTab = uiState.activeTab,
                    onSelectTab = viewModel::selectTab
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(colors.backgroundMain)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AnimatedContent(
                    targetState = Pair(uiState.activeTab, activeSecondary),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { (tab, secondary) ->
                    if (secondary != null) {
                        SecondaryDestinationContent(
                            destination = secondary,
                            uiState = uiState,
                            viewModel = viewModel,
                            bottomContentPadding = bottomScrollPadding
                        )
                    } else {
                        PrimaryTabContent(
                            tab = tab,
                            uiState = uiState,
                            viewModel = viewModel,
                            bottomContentPadding = bottomScrollPadding
                        )
                    }
                }

                // Docked Mini Player + Custom Bottom Navigation Bar
                if (showBottomNavigation) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .then(
                                if (isWideLayout) Modifier.navigationBarsPadding()
                                else Modifier
                            )
                    ) {
                        MillesMiniPlayer(
                            track = uiState.currentTrack,
                            isPlaying = uiState.isPlaying,
                            isFavorite = uiState.isCurrentTrackFavorite,
                            progressSeconds = uiState.progressSeconds,
                            onOpenNowPlaying = viewModel::openNowPlaying,
                            onTogglePlayPause = viewModel::togglePlayPause,
                            onToggleFavorite = { viewModel.toggleFavorite(uiState.currentTrack.id) }
                        )
                        if (!isWideLayout) {
                            MillesBottomNavBar(
                                activeTab = uiState.activeTab,
                                onSelectTab = viewModel::selectTab
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryTabContent(
    tab: MainTab,
    uiState: MillesUiState,
    viewModel: MillesMusicViewModel,
    bottomContentPadding: androidx.compose.ui.unit.Dp
) {
    when (tab) {
        MainTab.HOME -> {
            HomeScreen(
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onOpenSearch = { viewModel.navigateTo(SecondaryDestination.Search) },
                onOpenProfile = { viewModel.selectTab(MainTab.PROFILE) },
                onPlayHeroMix = viewModel::playHeroMix,
                onOpenPlaylist = { playlist ->
                    viewModel.navigateTo(SecondaryDestination.PlaylistDetail(playlist.id))
                },
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
                onPlayTrack = { track -> viewModel.playTrack(track) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        MainTab.EXPLORE -> {
            ExploreScreen(
                selectedFilter = uiState.exploreFilter,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onSelectFilter = viewModel::setExploreFilter,
                onOpenSearch = { viewModel.navigateTo(SecondaryDestination.Search) },
                onOpenGenre = { genre ->
                    viewModel.navigateTo(SecondaryDestination.GenreDetail(genre.id))
                },
                onOpenArtist = { artist ->
                    viewModel.navigateTo(SecondaryDestination.ArtistDetail(artist.id))
                },
                onPlayTrack = { track -> viewModel.playTrack(track) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        MainTab.LIBRARY -> {
            LibraryScreen(
                selectedFilter = uiState.libraryFilter,
                playlists = uiState.userPlaylists,
                favoriteTracks = uiState.favoriteTracks,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                onSelectFilter = viewModel::setLibraryFilter,
                onOpenSearch = { viewModel.navigateTo(SecondaryDestination.Search) },
                onCreatePlaylist = { viewModel.setCreatePlaylistOpen(true) },
                onOpenPlaylist = { playlist ->
                    viewModel.navigateTo(SecondaryDestination.PlaylistDetail(playlist.id))
                },
                onOpenAlbum = { album ->
                    viewModel.navigateTo(SecondaryDestination.AlbumDetail(album.id))
                },
                onOpenArtist = { artist ->
                    viewModel.navigateTo(SecondaryDestination.ArtistDetail(artist.id))
                },
                onPlayTrack = { track -> viewModel.playTrack(track) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        MainTab.FAVORITES -> {
            FavoritesScreen(
                favoriteTracks = uiState.favoriteTracks,
                selectedFilter = uiState.favoritesFilter,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                onSelectFilter = viewModel::setFavoritesFilter,
                onPlayAll = { tracks, shuffled ->
                    val ordered = if (shuffled) tracks.shuffled() else tracks
                    if (ordered.isNotEmpty()) {
                        viewModel.playTrack(ordered.first(), customQueue = ordered, openPlayer = false)
                    }
                },
                onPlayTrack = { track -> viewModel.playTrack(track) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        MainTab.PROFILE -> {
            ProfileScreen(
                themeMode = uiState.themeMode,
                onOpenThemeSettings = { viewModel.navigateTo(SecondaryDestination.ThemeSettings) },
                onOpenSheet = viewModel::openProfileSheet,
                onLogoutNotice = { viewModel.showStatusBanner("Account sync arrives in Phase 2") },
                bottomContentPadding = bottomContentPadding
            )
        }
    }
}

@Composable
private fun SecondaryDestinationContent(
    destination: SecondaryDestination,
    uiState: MillesUiState,
    viewModel: MillesMusicViewModel,
    bottomContentPadding: androidx.compose.ui.unit.Dp
) {
    when (destination) {
        is SecondaryDestination.Search -> {
            SearchScreen(
                query = uiState.searchQuery,
                recentSearches = uiState.recentSearches,
                onQueryChange = viewModel::updateSearchQuery,
                onSubmitQuery = viewModel::submitSearchQuery,
                onBack = { viewModel.navigateBack() },
                onPlayTrack = { track -> viewModel.playTrack(track, openPlayer = true) },
                onOpenArtist = { artist ->
                    viewModel.navigateTo(SecondaryDestination.ArtistDetail(artist.id))
                },
                onOpenAlbum = { album ->
                    viewModel.navigateTo(SecondaryDestination.AlbumDetail(album.id))
                },
                bottomContentPadding = bottomContentPadding
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

        is SecondaryDestination.PlaylistDetail -> {
            val playlist = viewModel.findPlaylist(destination.playlistId)
            val tracks = SampleCatalog.tracksByIds(playlist?.trackIds.orEmpty()).ifEmpty { SampleCatalog.tracks.take(4) }
            CollectionDetailScreen(
                title = playlist?.title ?: "Playlist",
                subtitle = "${playlist?.subtitle ?: "Curated"} · ${playlist?.displayedSongCount ?: tracks.size} songs",
                artwork = playlist?.artwork ?: SampleCatalog.tracks.first().artwork,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    val ordered = if (shuffled) tracks.shuffled() else tracks
                    viewModel.playTrack(ordered.first(), customQueue = ordered, openPlayer = true)
                },
                onPlayTrack = { track -> viewModel.playTrack(track, customQueue = tracks) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        is SecondaryDestination.GenreDetail -> {
            val genre = viewModel.findGenre(destination.genreId)
            val tracks = SampleCatalog.tracksByIds(genre?.trackIds.orEmpty()).ifEmpty { SampleCatalog.tracks.take(4) }
            CollectionDetailScreen(
                title = genre?.name ?: "Genre",
                subtitle = "${genre?.subtitle ?: "Editorial"} · ${tracks.size} featured tracks",
                artwork = genre?.artwork ?: SampleCatalog.tracks.first().artwork,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    val ordered = if (shuffled) tracks.shuffled() else tracks
                    viewModel.playTrack(ordered.first(), customQueue = ordered, openPlayer = true)
                },
                onPlayTrack = { track -> viewModel.playTrack(track, customQueue = tracks) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
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
                artwork = artist?.artwork ?: SampleCatalog.tracks.first().artwork,
                tracks = tracks,
                isCircularArtwork = true,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    val ordered = if (shuffled) tracks.shuffled() else tracks
                    viewModel.playTrack(ordered.first(), customQueue = ordered, openPlayer = true)
                },
                onPlayTrack = { track -> viewModel.playTrack(track, customQueue = tracks) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        is SecondaryDestination.AlbumDetail -> {
            val album = viewModel.findAlbum(destination.albumId)
            val tracks = SampleCatalog.tracksByIds(album?.trackIds.orEmpty()).ifEmpty { SampleCatalog.tracks.take(3) }
            CollectionDetailScreen(
                title = album?.title ?: "Album",
                subtitle = "${album?.artist ?: ""} · ${album?.year ?: 2026}",
                artwork = album?.artwork ?: SampleCatalog.tracks.first().artwork,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    val ordered = if (shuffled) tracks.shuffled() else tracks
                    viewModel.playTrack(ordered.first(), customQueue = ordered, openPlayer = true)
                },
                onPlayTrack = { track -> viewModel.playTrack(track, customQueue = tracks) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }

        is SecondaryDestination.CollectionOverview -> {
            val tracks = SampleCatalog.tracksByIds(destination.trackIds).ifEmpty { SampleCatalog.tracks }
            CollectionDetailScreen(
                title = destination.title,
                subtitle = "${tracks.size} curated tracks",
                artwork = tracks.first().artwork,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack.id,
                isPlaying = uiState.isPlaying,
                favoriteTrackIds = uiState.favoriteTrackIds,
                onBack = { viewModel.navigateBack() },
                onPlayAll = { shuffled ->
                    val ordered = if (shuffled) tracks.shuffled() else tracks
                    viewModel.playTrack(ordered.first(), customQueue = ordered, openPlayer = true)
                },
                onPlayTrack = { track -> viewModel.playTrack(track, customQueue = tracks) },
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenTrackOptions = viewModel::openTrackOptions,
                bottomContentPadding = bottomContentPadding
            )
        }
    }
}

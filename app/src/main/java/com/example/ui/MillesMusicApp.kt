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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.JamendoGenres
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
import com.example.ui.screens.YouTubePlayerOverlay
import com.example.ui.theme.LocalMillesPalette
import com.example.ui.theme.Milles
import com.example.ui.theme.MillesTheme

@Composable
fun MillesMusicApp(
    viewModel: MillesMusicViewModel = viewModel(
        factory = MillesMusicViewModel.factory(LocalContext.current.applicationContext)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MillesTheme(themeMode = uiState.themeMode) {
        val palette = LocalMillesPalette.current
        val playerUiState = remember(uiState) { uiState.toPlayerUiState() }

        val playerActions = remember(viewModel) {
            object : PlayerActions {
                override fun onBack() = viewModel.closeNowPlaying()
                override fun onPlayPause() = viewModel.togglePlayPause()
                override fun onNext() = viewModel.skipToNext()
                override fun onPrevious() = viewModel.skipToPrevious()
                override fun onSeek(positionMs: Long) = viewModel.seekMs(positionMs)
                override fun onLike() {
                    viewModel.uiState.value.currentTrack?.id?.let(viewModel::toggleFavorite)
                }
                override fun onShuffle() = viewModel.toggleShuffle()
                override fun onRepeat() = viewModel.cycleRepeatMode()
                override fun onMore() {
                    viewModel.uiState.value.currentTrack?.let(viewModel::openTrackOptions)
                }
                override fun onQualityClick() = viewModel.setAudioQualitySheetOpen(true)
                override fun onTrackClick(track: Track) {
                    viewModel.findTrackById(track.id)?.let { domainTrack ->
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
                            favoriteCount = uiState.favoriteTrackIds.size,
                            playlistCount = uiState.userPlaylists.size,
                            recentlyPlayedCount = uiState.recentlyPlayedTracks.size,
                            onBack = { viewModel.navigateBack() },
                            onOpenThemeSettings = { viewModel.navigateTo(SecondaryDestination.ThemeSettings) },
                            onOpenSheet = viewModel::openProfileSheet,
                            onLogoutNotice = {
                                viewModel.showStatusBanner("Streaming via public Jamendo & Audius catalogs")
                            }
                        )
                    }
                    else -> {
                        ThemeScreen(
                            themeMode = uiState.themeMode,
                            onSelectThemeMode = viewModel::selectThemeMode,
                            onToggleDynamicTheme = viewModel::toggleDynamicTheme,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            } else {
                val activeTrack = uiState.currentTrack
                MillesShell(
                    tab = uiState.activeTab,
                    onTab = viewModel::selectTab,
                    miniPlayer = if (playerUiState != null && activeTrack != null) {
                        {
                            val progress = (uiState.progressSeconds.toFloat() /
                                activeTrack.durationSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
                            MiniPlayer(
                                track = playerUiState.track,
                                isPlaying = uiState.isPlaying,
                                progress = progress,
                                isLiked = uiState.isCurrentTrackFavorite,
                                onOpen = viewModel::openNowPlaying,
                                onPlayPause = viewModel::togglePlayPause,
                                onLike = { viewModel.toggleFavorite(activeTrack.id) }
                            )
                        }
                    } else null
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
                visible = uiState.isNowPlayingOpen && playerUiState != null && uiState.activeYouTubeTrack == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                playerUiState?.let { activePlayerState ->
                    NowPlayingScreen(
                        state = activePlayerState,
                        actions = playerActions
                    )
                }
            }

            // Dedicated Official YouTube Embedded Player Overlay (never routed through Media3)
            AnimatedVisibility(
                visible = uiState.activeYouTubeTrack != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                uiState.activeYouTubeTrack?.let { ytTrack ->
                    YouTubePlayerOverlay(
                        track = ytTrack,
                        onDismiss = viewModel::closeYouTubePlayer
                    )
                }
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
    val popularUi = remember(uiState.popularTracks) { uiState.popularTracks.map { it.toUiTrack() } }
    val recommendedUi = remember(uiState.recommendedTracks) { uiState.recommendedTracks.map { it.toUiTrack() } }
    val newReleasesUi = remember(uiState.newReleases) { uiState.newReleases.map { it.toUiTrack() } }
    val audiusTrendingUi = remember(uiState.audiusTrendingTracks) { uiState.audiusTrendingTracks.map { it.toUiTrack() } }
    val recentlyPlayedUi = remember(uiState.recentlyPlayedTracks) { uiState.recentlyPlayedTracks.map { it.toUiTrack() } }
    val recentlyAddedAlbumsUi = remember(uiState.recentlyAddedAlbums) { uiState.recentlyAddedAlbums.map { it.toUiCollection() } }
    val genreCols = remember { JamendoGenres.genres.map { it.toUiCollection() } }

    when (tab) {
        MillesTab.Home -> {
            HomeScreen(
                heroCover = R.drawable.img_hero_music,
                avatar = R.drawable.img_cover_midnight,
                isLoading = uiState.isCatalogLoading,
                errorMessage = uiState.catalogErrorMessage,
                popular = popularUi,
                recommended = recommendedUi,
                newReleases = newReleasesUi,
                audiusTrending = audiusTrendingUi,
                recentlyPlayed = recentlyPlayedUi,
                recentlyAddedAlbums = recentlyAddedAlbumsUi,
                genres = genreCols,
                isHeroPlaying = uiState.isPlaying && (
                    uiState.playbackSource == "Popular on Jamendo" ||
                        uiState.playbackSource == "Trending on Audius"
                    ),
                onPlayHero = viewModel::playHeroMix,
                onRetry = viewModel::retryCatalogLoad,
                onTrack = { uiTrack, sectionSource ->
                    viewModel.findTrackById(uiTrack.id)?.let { track ->
                        val sectionQueue = when (sectionSource) {
                            "Popular Tracks" -> uiState.popularTracks
                            "Recommended Music" -> uiState.recommendedTracks
                            "New Releases" -> uiState.newReleases
                            "Trending on Audius" -> uiState.audiusTrendingTracks
                            "Recently Played" -> uiState.recentlyPlayedTracks
                            else -> null
                        }
                        viewModel.playTrack(track, customQueue = sectionQueue, source = sectionSource)
                    }
                },
                onAlbum = { album ->
                    viewModel.navigateTo(SecondaryDestination.AlbumDetail(album.id))
                },
                onGenre = { genre ->
                    viewModel.navigateTo(SecondaryDestination.GenreDetail(genre.id))
                },
                onProfile = { viewModel.navigateTo(SecondaryDestination.Profile) },
                onSeeAllPopular = {
                    viewModel.navigateTo(
                        SecondaryDestination.CollectionOverview(
                            title = "Popular Tracks",
                            trackIds = uiState.popularTracks.map { it.id }
                        )
                    )
                },
                onSeeAllRecommended = {
                    viewModel.navigateTo(
                        SecondaryDestination.CollectionOverview(
                            title = "Recommended Music",
                            trackIds = uiState.recommendedTracks.map { it.id }
                        )
                    )
                },
                onSeeAllNewReleases = {
                    viewModel.navigateTo(
                        SecondaryDestination.CollectionOverview(
                            title = "New Releases",
                            trackIds = uiState.newReleases.map { it.id }
                        )
                    )
                },
                onSeeAllAudiusTrending = {
                    viewModel.navigateTo(
                        SecondaryDestination.CollectionOverview(
                            title = "Trending on Audius",
                            trackIds = uiState.audiusTrendingTracks.map { it.id }
                        )
                    )
                },
                bottomInset = bottomInset
            )
        }

        MillesTab.Explore -> {
            ExploreScreen(
                isLoading = uiState.isCatalogLoading,
                errorMessage = uiState.catalogErrorMessage,
                newReleases = newReleasesUi,
                topCharts = popularUi,
                activeTrackId = uiState.currentTrack?.id,
                onRetry = viewModel::retryCatalogLoad,
                onTrack = { uiTrack ->
                    viewModel.findTrackById(uiTrack.id)?.let { track ->
                        viewModel.playTrack(track, customQueue = uiState.popularTracks, source = "Popular on Jamendo")
                    }
                },
                onMoreTrack = { uiTrack ->
                    viewModel.findTrackById(uiTrack.id)?.let(viewModel::openTrackOptions)
                },
                bottomInset = bottomInset
            )
        }

        MillesTab.Library -> {
            val likedUi = remember(uiState.favoriteTracks) { uiState.favoriteTracks.map { it.toUiTrack() } }
            val playlistCols = remember(uiState.userPlaylists) { uiState.userPlaylists.map { it.toUiCollection() } }
            val albumCols = remember(uiState.albums) { uiState.albums.map { it.toUiCollection() } }
            val artistCols = remember(uiState.artists) { uiState.artists.map { it.toUiCollection() } }

            LibraryScreen(
                liked = likedUi,
                playlists = playlistCols,
                albums = albumCols,
                artists = artistCols,
                isCatalogLoading = uiState.isCatalogLoading,
                catalogErrorMessage = uiState.catalogErrorMessage,
                activeTrackId = uiState.currentTrack?.id,
                onRetryCatalog = viewModel::retryCatalogLoad,
                onTrack = { uiTrack ->
                    viewModel.findTrackById(uiTrack.id)?.let { track ->
                        viewModel.playTrack(track, customQueue = uiState.favoriteTracks, source = "Liked Songs")
                    }
                },
                onMoreTrack = { uiTrack ->
                    viewModel.findTrackById(uiTrack.id)?.let(viewModel::openTrackOptions)
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
            val results = remember(uiState.searchResults) { uiState.searchResults.map { it.toUiTrack() } }
            val searchArtistCols = remember(uiState.searchArtists) { uiState.searchArtists.map { it.toUiCollection() } }
            val searchAlbumCols = remember(uiState.searchAlbums) { uiState.searchAlbums.map { it.toUiCollection() } }

            SearchScreen(
                query = query,
                onQuery = viewModel::updateSearchQuery,
                selectedProviderFilter = uiState.searchProviderFilter,
                onSelectProviderFilter = viewModel::selectSearchProviderFilter,
                providerNotices = uiState.providerSearchNotices,
                isSearching = uiState.isSearching,
                errorMessage = uiState.searchErrorMessage,
                results = results,
                artists = searchArtistCols,
                albums = searchAlbumCols,
                genres = genreCols,
                activeTrackId = uiState.currentTrack?.id,
                onRetry = { viewModel.updateSearchQuery(query) },
                onTrack = { uiTrack ->
                    viewModel.findTrackById(uiTrack.id)?.let { track ->
                        viewModel.playTrack(track, customQueue = uiState.searchResults, source = "Search")
                    }
                },
                onMoreTrack = { uiTrack ->
                    viewModel.findTrackById(uiTrack.id)?.let(viewModel::openTrackOptions)
                },
                onArtist = { artistCol ->
                    viewModel.navigateTo(SecondaryDestination.ArtistDetail(artistCol.id))
                },
                onAlbum = { albumCol ->
                    viewModel.navigateTo(SecondaryDestination.AlbumDetail(albumCol.id))
                },
                onGenre = { genreCol ->
                    viewModel.navigateTo(SecondaryDestination.GenreDetail(genreCol.id))
                },
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
            val tracks = viewModel.findTracksByIds(playlist?.trackIds.orEmpty())
            CollectionDetailScreen(
                title = playlist?.title ?: "Playlist",
                subtitle = playlist?.subtitle ?: "",
                coverUrl = playlist?.artwork?.imageRes ?: R.drawable.img_cover_afro,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack?.id,
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
            val tracks = uiState.genreTracksByGenreId[destination.genreId].orEmpty()
            CollectionDetailScreen(
                title = genre?.name ?: "Genre",
                subtitle = genre?.subtitle ?: "",
                coverUrl = tracks.firstOrNull()?.coverUrl ?: genre?.artwork?.imageRes ?: R.drawable.img_cover_chill,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack?.id,
                isLoading = uiState.detailLoadingKey == destination.genreId,
                errorMessage = uiState.detailErrorMessage,
                onRetry = { viewModel.retrySecondaryLoad(destination) },
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
            val tracks = uiState.artistTracksByArtistId[destination.artistId].orEmpty()
            CollectionDetailScreen(
                title = artist?.name ?: "Artist",
                subtitle = artist?.subtitle ?: "Jamendo Artist",
                coverUrl = artist?.imageUrl ?: artist?.artwork?.imageRes ?: R.drawable.img_cover_midnight,
                tracks = tracks,
                isCircularArtwork = true,
                currentTrackId = uiState.currentTrack?.id,
                isLoading = uiState.detailLoadingKey == destination.artistId,
                errorMessage = uiState.detailErrorMessage,
                onRetry = { viewModel.retrySecondaryLoad(destination) },
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
            val tracks = uiState.albumTracksByAlbumId[destination.albumId].orEmpty()
            CollectionDetailScreen(
                title = album?.title ?: "Album",
                subtitle = listOfNotNull(album?.artist, album?.releaseDate?.take(4)).joinToString(" · "),
                coverUrl = album?.coverUrl ?: album?.artwork?.imageRes ?: R.drawable.img_cover_chill,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack?.id,
                isLoading = uiState.detailLoadingKey == destination.albumId,
                errorMessage = uiState.detailErrorMessage,
                onRetry = { viewModel.retrySecondaryLoad(destination) },
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
            val tracks = viewModel.findTracksByIds(destination.trackIds)
            CollectionDetailScreen(
                title = destination.title,
                subtitle = "${tracks.size} tracks",
                coverUrl = tracks.firstOrNull()?.coverUrl ?: tracks.firstOrNull()?.artwork?.imageRes ?: R.drawable.img_hero_music,
                tracks = tracks,
                isCircularArtwork = false,
                currentTrackId = uiState.currentTrack?.id,
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

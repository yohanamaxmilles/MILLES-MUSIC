package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.example.data.JamendoRepository
import com.example.model.ProviderId
import com.example.model.RepeatMode
import com.example.model.ThemeMode
import com.example.model.Track
import com.example.network.AudiusAccessDto
import com.example.network.AudiusApiService
import com.example.network.AudiusArtworkDto
import com.example.network.AudiusPlaylistDto
import com.example.network.AudiusPlaylistsResponse
import com.example.network.AudiusSinglePlaylistResponse
import com.example.network.AudiusSingleTrackResponse
import com.example.network.AudiusSingleUserResponse
import com.example.network.AudiusStreamDto
import com.example.network.AudiusTrackDto
import com.example.network.AudiusTracksResponse
import com.example.network.AudiusUserDto
import com.example.network.AudiusUsersResponse
import com.example.network.JamendoAlbumDto
import com.example.network.JamendoAlbumsResponse
import com.example.network.JamendoApiService
import com.example.network.JamendoArtistDto
import com.example.network.JamendoArtistsResponse
import com.example.network.JamendoTrackDto
import com.example.network.JamendoTracksResponse
import com.example.network.YouTubeApiService
import com.example.network.YouTubeContentDetailsDto
import com.example.network.YouTubeRegionRestrictionDto
import com.example.network.YouTubeSearchItemDto
import com.example.network.YouTubeSearchResponse
import com.example.network.YouTubeSnippetDto
import com.example.network.YouTubeThumbnailDto
import com.example.network.YouTubeThumbnailsDto
import com.example.network.YouTubeVideoIdDto
import com.example.network.YouTubeVideoItemDto
import com.example.network.YouTubeVideoStatusDto
import com.example.network.YouTubeVideosResponse
import com.example.playback.PlaybackEngine
import com.example.playback.PlaybackEngineEvent
import com.example.provider.AudiusMusicProvider
import com.example.provider.JamendoMusicProvider
import com.example.provider.SoundCloudMusicProvider
import com.example.provider.SpotifyMetadataProvider
import com.example.provider.UnifiedMusicCatalogService
import com.example.provider.YouTubeMusicProvider
import com.example.provider.toDomainTrack
import com.example.state.MillesMusicViewModel
import com.example.state.formatDuration
import com.example.state.toPlayerUiState
import com.example.state.toUiTrack
import com.example.ui.components.MillesTab
import com.example.ui.formatTime
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `formatDuration and formatTime format correctly`() {
        assertEquals("00:45", formatDuration(45))
        assertEquals("03:52", formatDuration(232))
        assertEquals("00:45", formatTime(45_000L))
    }

    @Test
    fun `view model integrates multi-provider catalog, PlaybackEngine, and YouTube embed isolation`() = runTest(testDispatcher) {
        val fakePlayer = FakePlaybackEngine()
        val repo = JamendoRepository(
            apiService = FakeJamendoApiService(),
            clientId = "sample_proto_id"
        )
        val audiusProvider = AudiusMusicProvider(apiService = FakeAudiusApiService())
        val youTubeProvider = YouTubeMusicProvider(
            apiService = FakeYouTubeApiService(),
            apiKey = "sample_youtube_key"
        )
        val catalogService = UnifiedMusicCatalogService(
            jamendoRepository = repo,
            jamendoProvider = JamendoMusicProvider(repo),
            audiusProvider = audiusProvider,
            youTubeProvider = youTubeProvider
        )
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MillesMusicViewModel(
                    jamendoRepository = repo,
                    playbackEngine = fakePlayer,
                    autoLoadCatalog = true,
                    audiusProvider = audiusProvider,
                    youTubeProvider = youTubeProvider,
                    catalogService = catalogService
                ) as T
            }
        )[MillesMusicViewModel::class.java]

        try {
            assertEquals(4, MillesTab.entries.size)
            assertNull(vm.uiState.value.currentTrack)
            assertTrue(vm.uiState.value.userPlaylists.isEmpty())
            assertTrue(vm.uiState.value.favoriteTrackIds.isEmpty())

            testScheduler.advanceTimeBy(100)
            testScheduler.runCurrent()

            assertFalse(vm.uiState.value.isCatalogLoading)
            val loadedTrack = vm.uiState.value.popularTracks.first()
            assertEquals("jam_101", loadedTrack.id)
            assertEquals(ProviderId.JAMENDO, loadedTrack.providerId)
            assertTrue(loadedTrack.attributionLabel.contains("CC BY-NC-SA"))
            assertFalse(loadedTrack.isCommercialLicensed)
            assertEquals("https://cdn.example.com/cover.jpg", loadedTrack.toUiTrack().coverUrl)

            assertEquals(1, vm.uiState.value.audiusTrendingTracks.size)
            assertEquals(ProviderId.AUDIUS, vm.uiState.value.audiusTrendingTracks.first().providerId)

            vm.playTrack(loadedTrack, openPlayer = true)
            testScheduler.runCurrent()
            assertTrue(vm.uiState.value.isPlaying)
            assertEquals("jam_101", vm.uiState.value.currentTrack?.id)
            assertEquals(1, vm.uiState.value.recentlyPlayedTracks.size)

            // Playing a YouTube result must pause Media3 and open activeYouTubeTrack instead
            val ytTrack = youTubeProvider.searchTracks("solar").getOrThrow().first().toDomainTrack()
            assertNull(ytTrack.streamUrl)
            assertFalse(ytTrack.streamingPermitted)
            assertTrue(ytTrack.isVideoEmbedOnly)

            vm.playTrack(ytTrack)
            testScheduler.runCurrent()
            assertFalse(fakePlayer.isPlaying())
            assertEquals(ytTrack.id, vm.uiState.value.activeYouTubeTrack?.id)
            assertEquals("jam_101", vm.uiState.value.currentTrack?.id)

            vm.closeYouTubePlayer()
            assertNull(vm.uiState.value.activeYouTubeTrack)

            vm.toggleShuffle()
            assertTrue(fakePlayer.currentShuffle)
            vm.cycleRepeatMode()
            assertEquals(RepeatMode.ALL, fakePlayer.currentRepeatMode)
            vm.cycleRepeatMode()
            assertEquals(RepeatMode.ONE, fakePlayer.currentRepeatMode)

            vm.seekTo(60)
            assertEquals(60_000L, fakePlayer.lastSeekMs)

            vm.selectTab(MillesTab.Library)
            assertEquals(MillesTab.Library, vm.uiState.value.activeTab)

            vm.createPlaylist("Live Session", "Real picks")
            assertEquals(1, vm.uiState.value.userPlaylists.size)
            assertEquals("Live Session", vm.uiState.value.userPlaylists.first().title)

            vm.selectThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)

            val playerUiState = vm.uiState.value.toPlayerUiState()
            assertNotNull(playerUiState)
            assertTrue(playerUiState!!.repeatOne)
            assertEquals(loadedTrack.id, playerUiState.track.id)
        } finally {
            store.clear()
        }
    }

    @Test
    fun `unified catalog isolates failing providers and deduplicates overlapping audio`() = runBlocking {
        val repo = JamendoRepository(apiService = FakeJamendoApiService(), clientId = "sample_proto_id")
        val failingAudius = AudiusMusicProvider(apiService = FailingAudiusApiService())
        val youTube = YouTubeMusicProvider(apiService = FakeYouTubeApiService(), apiKey = "sample_yt_key")

        val service = UnifiedMusicCatalogService(
            jamendoRepository = repo,
            jamendoProvider = JamendoMusicProvider(repo),
            audiusProvider = failingAudius,
            youTubeProvider = youTube
        )

        val result = service.search("Solar Echoes")
        // Jamendo & YouTube succeed even though Audius threw IOException
        assertEquals(2, result.tracks.size)
        assertTrue(result.tracks.any { it.providerId == ProviderId.JAMENDO })
        assertTrue(result.tracks.any { it.providerId == ProviderId.YOUTUBE })
        assertEquals(1, result.providerFailures.size)
        assertEquals(ProviderId.AUDIUS, result.providerFailures.first().providerId)
    }

    @Test
    fun `soundcloud and spotify adapters remain disabled and prohibit scraping or extraction`() = runBlocking {
        val soundCloud = SoundCloudMusicProvider()
        val spotify = SpotifyMetadataProvider()

        assertFalse(soundCloud.isEnabled)
        assertFalse(spotify.isEnabled)
        assertTrue(soundCloud.searchTracks("test").isFailure)
        assertTrue(soundCloud.getAuthorizedStreamUrl("sc_1").isFailure)
        assertTrue(spotify.searchTracks("test").isFailure)
        assertTrue(spotify.getAuthorizedStreamUrl("sp_1").isFailure)
    }

    @Test
    fun `youtube provider parses ISO durations and blocks audio stream extraction`() = runBlocking {
        assertEquals(225, YouTubeMusicProvider.parseIso8601DurationSeconds("PT3M45S"))
        assertEquals(3725, YouTubeMusicProvider.parseIso8601DurationSeconds("PT1H2M5S"))

        val unconfigured = YouTubeMusicProvider(
            apiService = FakeYouTubeApiService(),
            apiKey = YouTubeMusicProvider.PLACEHOLDER_API_KEY
        )
        assertFalse(unconfigured.isEnabled)
        assertTrue(unconfigured.searchTracks("music").isFailure)

        val configured = YouTubeMusicProvider(
            apiService = FakeYouTubeApiService(),
            apiKey = "sample_yt_key"
        )
        assertTrue(configured.isEnabled)
        assertTrue(configured.getAuthorizedStreamUrl("yt_abc123").isFailure)
        val results = configured.searchTracks("solar").getOrThrow()
        assertEquals(1, results.size)
        assertNull(results.first().streamUrl)
        assertFalse(results.first().streamingPermitted)
        assertTrue(results.first().isVideoEmbedOnly)
        assertEquals("https://www.youtube.com/watch?v=abc123", results.first().originalProviderUrl)
        assertTrue(results.first().territoryRestrictions?.contains("US") == true)
    }

    @Test
    fun `audius provider rejects stream-gated tracks from authorized streaming`() = runBlocking {
        val audius = AudiusMusicProvider(apiService = FakeAudiusApiService(streamGated = true))
        val tracks = audius.fetchTrendingTracks(limit = 5).getOrThrow()
        assertEquals(1, tracks.size)
        assertFalse(tracks.first().streamingPermitted)
        assertNull(tracks.first().streamUrl)
        assertTrue(audius.getAuthorizedStreamUrl("aud_a1").isFailure)
    }

    @Test
    fun `live Jamendo API returns real tracks artists albums and reachable audio stream`() = runBlocking {
        val repo = JamendoRepository()
        Assume.assumeTrue("JAMENDO_CLIENT_ID not configured in this test environment", repo.isConfigured)

        val popular = repo.fetchPopularTracks(limit = 10).getOrThrow()
        val recommended = repo.fetchRecommendedTracks(limit = 8).getOrThrow()
        val newReleases = repo.fetchNewReleases(limit = 8).getOrThrow()
        val artists = repo.fetchArtists(limit = 8).getOrThrow()
        val albums = repo.fetchAlbums(limit = 8).getOrThrow()
        val search = repo.searchTracks("ambient", limit = 5).getOrThrow()
        val genreTracks = repo.fetchTracksByGenre("electronic", limit = 5).getOrThrow()

        assertTrue("Expected non-empty popular tracks from Jamendo", popular.isNotEmpty())
        assertTrue("Expected non-empty recommended tracks from Jamendo", recommended.isNotEmpty())
        assertTrue("Expected non-empty new releases from Jamendo", newReleases.isNotEmpty())
        assertTrue("Expected non-empty artists from Jamendo", artists.isNotEmpty())
        assertTrue("Expected non-empty albums from Jamendo", albums.isNotEmpty())
        assertTrue("Expected non-empty search results from Jamendo", search.isNotEmpty())
        assertTrue("Expected non-empty genre tracks from Jamendo", genreTracks.isNotEmpty())

        val firstTrack = popular.first()
        assertFalse(firstTrack.streamUrl.isNullOrBlank())

        val audioStatus = probeHttpRangeStatus(firstTrack.streamUrl!!)
        assertTrue("Expected HTTP 200/206 for Jamendo audio stream, got $audioStatus", audioStatus in 200..299)

        println(
            "JAMENDO_LIVE_TEST_SUMMARY popular=${popular.size} recommended=${recommended.size} " +
                "newReleases=${newReleases.size} artists=${artists.size} albums=${albums.size} " +
                "search=${search.size} genre=${genreTracks.size} streamHttp=$audioStatus"
        )
    }

    @Test
    fun `live Audius API returns trending tracks search results playlists and reachable stream`() = runBlocking {
        val audius = AudiusMusicProvider()
        assertTrue(audius.isEnabled)

        val trending = audius.fetchTrendingTracks(limit = 8).getOrThrow()
        val search = audius.searchTracks("electronic", limit = 6).getOrThrow()
        val artists = audius.searchArtists("audius", limit = 4).getOrThrow()
        val playlists = audius.getPlaylists(limit = 4).getOrThrow()

        assertTrue("Expected non-empty trending tracks from Audius", trending.isNotEmpty())
        assertTrue("Expected non-empty search results from Audius", search.isNotEmpty())
        assertTrue("Expected non-empty artists from Audius", artists.isNotEmpty())
        assertTrue("Expected non-empty playlists from Audius", playlists.isNotEmpty())

        val playable = trending.firstOrNull { it.streamingPermitted && !it.streamUrl.isNullOrBlank() }
        assertNotNull("Expected at least one playable Audius track", playable)

        val streamStatus = probeHttpRangeStatus(playable!!.streamUrl!!)
        assertTrue("Expected HTTP 200/206 for Audius stream, got $streamStatus", streamStatus in 200..299)

        println(
            "AUDIUS_LIVE_TEST_SUMMARY trending=${trending.size} search=${search.size} " +
                "artists=${artists.size} playlists=${playlists.size} streamHttp=$streamStatus"
        )
    }

    @Test
    fun `live YouTube API returns embeddable music videos when YOUTUBE_API_KEY is configured`() = runBlocking {
        val youtube = YouTubeMusicProvider()
        Assume.assumeTrue("YOUTUBE_API_KEY not configured in this test environment", youtube.isEnabled)

        val results = youtube.searchTracks("ambient music", limit = 5).getOrThrow()
        assertTrue("Expected non-empty YouTube music video results", results.isNotEmpty())
        val first = results.first()
        assertNull(first.streamUrl)
        assertFalse(first.streamingPermitted)
        assertTrue(first.isVideoEmbedOnly)
        assertTrue(first.originalProviderUrl?.startsWith("https://www.youtube.com/watch?v=") == true)
        println("YOUTUBE_LIVE_TEST_SUMMARY results=${results.size}")
    }

    private fun probeHttpRangeStatus(url: String): Int {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Range", "bytes=0-1023")
        }
        return try {
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private class FakeJamendoApiService : JamendoApiService {
        override suspend fun getTracks(
            clientId: String,
            format: String,
            limit: Int,
            audioFormat: String,
            include: String,
            order: String?,
            search: String?,
            tags: String?,
            featured: Int?,
            artistId: String?,
            albumId: String?
        ): JamendoTracksResponse = JamendoTracksResponse(
            results = listOf(
                JamendoTrackDto(
                    id = "101",
                    name = "Solar Echoes",
                    duration = 210,
                    artistId = "501",
                    artistName = "Milles Artist",
                    albumId = "801",
                    albumName = "Prototype LP",
                    releaseDate = "2026-01-15",
                    albumImage = "https://cdn.example.com/cover.jpg",
                    audio = "https://cdn.example.com/audio.mp3",
                    licenseCcUrl = "https://creativecommons.org/licenses/by-nc-sa/3.0/"
                )
            )
        )

        override suspend fun getArtists(
            clientId: String,
            format: String,
            limit: Int,
            order: String?,
            nameSearch: String?,
            hasImage: Boolean?
        ): JamendoArtistsResponse = JamendoArtistsResponse(
            results = listOf(JamendoArtistDto(id = "501", name = "Milles Artist", joinDate = "2024-05-01"))
        )

        override suspend fun getAlbums(
            clientId: String,
            format: String,
            limit: Int,
            order: String?,
            nameSearch: String?,
            artistId: String?
        ): JamendoAlbumsResponse = JamendoAlbumsResponse(
            results = listOf(
                JamendoAlbumDto(
                    id = "801",
                    name = "Prototype LP",
                    releaseDate = "2026-01-15",
                    artistId = "501",
                    artistName = "Milles Artist"
                )
            )
        )
    }

    private class FakeAudiusApiService(private val streamGated: Boolean = false) : AudiusApiService {
        private val sampleTrack = AudiusTrackDto(
            id = "a1",
            title = "Midnight Signal",
            duration = 195,
            genre = "Electronic",
            permalink = "/audiuscreator/midnight-signal",
            isAvailable = true,
            isDelete = false,
            isStreamable = !streamGated,
            isStreamGated = streamGated,
            artwork = AudiusArtworkDto(medium = "https://cdn.example.com/audius_cover.jpg"),
            stream = AudiusStreamDto(url = "https://cdn.example.com/audius_stream.mp3"),
            access = AudiusAccessDto(stream = !streamGated),
            user = AudiusUserDto(id = "u1", name = "Audius Creator", handle = "audiuscreator")
        )

        override suspend fun getTrendingTracks(time: String, limit: Int, appName: String) =
            AudiusTracksResponse(data = listOf(sampleTrack))

        override suspend fun searchTracks(query: String, limit: Int, appName: String) =
            AudiusTracksResponse(data = listOf(sampleTrack))

        override suspend fun getTrack(trackId: String, appName: String) =
            AudiusSingleTrackResponse(data = sampleTrack)

        override suspend fun searchUsers(query: String, limit: Int, appName: String) =
            AudiusUsersResponse(data = listOf(sampleTrack.user!!))

        override suspend fun getUser(userId: String, appName: String) =
            AudiusSingleUserResponse(data = sampleTrack.user)

        override suspend fun getUserTracks(userId: String, limit: Int, appName: String) =
            AudiusTracksResponse(data = listOf(sampleTrack))

        override suspend fun getTrendingPlaylists(time: String, limit: Int, appName: String) =
            AudiusPlaylistsResponse(
                data = listOf(AudiusPlaylistDto(id = "pl1", playlistName = "Audius Picks", user = sampleTrack.user))
            )

        override suspend fun getPlaylist(playlistId: String, appName: String) =
            AudiusSinglePlaylistResponse(data = AudiusPlaylistDto(id = "pl1", playlistName = "Audius Picks"))

        override suspend fun getPlaylistTracks(playlistId: String, appName: String) =
            AudiusTracksResponse(data = listOf(sampleTrack))
    }

    private class FailingAudiusApiService : AudiusApiService {
        private fun fail(): Nothing = throw IOException("Simulated network timeout")
        override suspend fun getTrendingTracks(time: String, limit: Int, appName: String): AudiusTracksResponse = fail()
        override suspend fun searchTracks(query: String, limit: Int, appName: String): AudiusTracksResponse = fail()
        override suspend fun getTrack(trackId: String, appName: String): AudiusSingleTrackResponse = fail()
        override suspend fun searchUsers(query: String, limit: Int, appName: String): AudiusUsersResponse = fail()
        override suspend fun getUser(userId: String, appName: String): AudiusSingleUserResponse = fail()
        override suspend fun getUserTracks(userId: String, limit: Int, appName: String): AudiusTracksResponse = fail()
        override suspend fun getTrendingPlaylists(time: String, limit: Int, appName: String): AudiusPlaylistsResponse = fail()
        override suspend fun getPlaylist(playlistId: String, appName: String): AudiusSinglePlaylistResponse = fail()
        override suspend fun getPlaylistTracks(playlistId: String, appName: String): AudiusTracksResponse = fail()
    }

    private class FakeYouTubeApiService : YouTubeApiService {
        override suspend fun searchMusicVideos(
            part: String,
            type: String,
            videoCategoryId: String,
            videoEmbeddable: String,
            maxResults: Int,
            query: String,
            apiKey: String
        ) = YouTubeSearchResponse(
            items = listOf(
                YouTubeSearchItemDto(
                    id = YouTubeVideoIdDto(videoId = "abc123"),
                    snippet = YouTubeSnippetDto(
                        title = "Solar Echoes (Official Video)",
                        channelTitle = "Milles Channel",
                        thumbnails = YouTubeThumbnailsDto(high = YouTubeThumbnailDto("https://i.ytimg.com/vi/abc123/hqdefault.jpg"))
                    )
                )
            )
        )

        override suspend fun getVideoDetails(part: String, videoIds: String, apiKey: String) =
            YouTubeVideosResponse(
                items = listOf(
                    YouTubeVideoItemDto(
                        id = "abc123",
                        snippet = YouTubeSnippetDto(
                            title = "Solar Echoes (Official Video)",
                            channelTitle = "Milles Channel",
                            thumbnails = YouTubeThumbnailsDto(high = YouTubeThumbnailDto("https://i.ytimg.com/vi/abc123/hqdefault.jpg"))
                        ),
                        contentDetails = YouTubeContentDetailsDto(
                            duration = "PT3M45S",
                            regionRestriction = YouTubeRegionRestrictionDto(allowed = listOf("US", "CA"))
                        ),
                        status = YouTubeVideoStatusDto(embeddable = true)
                    )
                )
            )
    }

    private class FakePlaybackEngine : PlaybackEngine {
        private val _events = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 16)
        override val events: Flow<PlaybackEngineEvent> = _events

        var currentShuffle: Boolean = false
        var currentRepeatMode: RepeatMode = RepeatMode.OFF
        var lastSeekMs: Long = 0L
        private var playing: Boolean = false

        override fun setQueueAndPlay(tracks: List<Track>, startIndex: Int) {
            playing = true
            val track = tracks.getOrNull(startIndex)
            _events.tryEmit(PlaybackEngineEvent.BufferingChanged(false))
            _events.tryEmit(PlaybackEngineEvent.IsPlayingChanged(true))
            if (track != null) {
                _events.tryEmit(PlaybackEngineEvent.TrackTransitioned(track.id, track.durationSeconds))
            }
        }

        override fun play() {
            playing = true
            _events.tryEmit(PlaybackEngineEvent.IsPlayingChanged(true))
        }

        override fun pause() {
            playing = false
            _events.tryEmit(PlaybackEngineEvent.IsPlayingChanged(false))
        }

        override fun seekToNext() = Unit
        override fun seekToPrevious() = Unit
        override fun seekToMs(positionMs: Long) {
            lastSeekMs = positionMs
        }

        override fun setShuffleEnabled(enabled: Boolean) {
            currentShuffle = enabled
            _events.tryEmit(PlaybackEngineEvent.ShuffleChanged(enabled))
        }

        override fun setRepeatMode(mode: RepeatMode) {
            currentRepeatMode = mode
            _events.tryEmit(PlaybackEngineEvent.RepeatChanged(mode))
        }

        override fun currentPositionSeconds(): Int = (lastSeekMs / 1000L).toInt()
        override fun currentDurationSeconds(): Int? = 210
        override fun isPlaying(): Boolean = playing
        override fun release() {
            playing = false
        }
    }
}

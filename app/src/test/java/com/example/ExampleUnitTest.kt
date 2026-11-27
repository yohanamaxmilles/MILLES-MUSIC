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
import com.example.network.JamendoTagsDto
import com.example.network.JamendoTrackDto
import com.example.network.JamendoTrackStatsDto
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
import com.example.playback.PlaybackResolution
import com.example.playback.PlaybackRouter
import com.example.provider.AudiusMusicProvider
import com.example.provider.JamendoMusicProvider
import com.example.provider.SoundCloudMusicProvider
import com.example.provider.SpotifyMetadataProvider
import com.example.provider.TrackDeduplicator
import com.example.provider.TrendingRankingEngine
import com.example.provider.UnifiedMusicCatalogService
import com.example.provider.UnifiedTrack
import com.example.provider.YouTubeMusicProvider
import com.example.provider.toDomainTrack
import com.example.state.MillesMusicViewModel
import com.example.state.formatDuration
import com.example.state.formatTrendMetricsLabel
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

            // Both Trending Now and Popular Tracks must contain Jamendo and Audius tracks with accurate provider badges
            assertTrue(vm.uiState.value.trendingNowTracks.any { it.providerId == ProviderId.JAMENDO && it.toUiTrack().providerId == ProviderId.JAMENDO })
            assertTrue(vm.uiState.value.trendingNowTracks.any { it.providerId == ProviderId.AUDIUS && it.toUiTrack().providerId == ProviderId.AUDIUS })
            assertTrue(vm.uiState.value.popularTracks.any { it.providerId == ProviderId.JAMENDO && it.toUiTrack().providerId == ProviderId.JAMENDO })
            assertTrue(vm.uiState.value.popularTracks.any { it.providerId == ProviderId.AUDIUS && it.toUiTrack().providerId == ProviderId.AUDIUS })
            assertTrue(vm.uiState.value.recommendedTracks.any { it.providerId == ProviderId.JAMENDO })
            assertTrue(vm.uiState.value.recommendedTracks.any { it.providerId == ProviderId.AUDIUS })
            assertTrue(vm.uiState.value.newReleases.any { it.providerId == ProviderId.JAMENDO })
            assertTrue(vm.uiState.value.newReleases.any { it.providerId == ProviderId.AUDIUS })

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
    fun `credential validation rejects placeholders, blanks, and null strings while trimming quotes`() {
        assertFalse(JamendoRepository(apiService = FakeJamendoApiService(), clientId = "").isConfigured)
        assertFalse(JamendoRepository(apiService = FakeJamendoApiService(), clientId = "   ").isConfigured)
        assertFalse(JamendoRepository(apiService = FakeJamendoApiService(), clientId = "null").isConfigured)
        assertFalse(JamendoRepository(apiService = FakeJamendoApiService(), clientId = JamendoRepository.PLACEHOLDER_CLIENT_ID).isConfigured)
        assertFalse(JamendoRepository(apiService = FakeJamendoApiService(), clientId = "\"MY_JAMENDO_CLIENT_ID\"").isConfigured)
        assertTrue(JamendoRepository(apiService = FakeJamendoApiService(), clientId = "  \"demo_client_123\" ").isConfigured)

        assertFalse(com.example.network.YouTubeApiFactory.isConfigured(""))
        assertFalse(com.example.network.YouTubeApiFactory.isConfigured("  "))
        assertFalse(com.example.network.YouTubeApiFactory.isConfigured("null"))
        assertFalse(com.example.network.YouTubeApiFactory.isConfigured(com.example.network.YouTubeApiFactory.PLACEHOLDER_API_KEY))
        assertFalse(YouTubeMusicProvider(apiService = FakeYouTubeApiService(), apiKey = "\"MY_YOUTUBE_API_KEY\"").isEnabled)
        assertTrue(YouTubeMusicProvider(apiService = FakeYouTubeApiService(), apiKey = " \"demo_yt_key_456\" ").isEnabled)
    }

    @Test
    fun `jamendo and youtube error paths never expose configured credentials`() = runBlocking {
        val secretJamId = "super_secret_jam_id_999"
        val failingJamRepo = JamendoRepository(
            apiService = FailingJamendoApiService(),
            clientId = secretJamId
        )
        val jamErr = failingJamRepo.fetchPopularTracks().exceptionOrNull()?.message.orEmpty()
        assertFalse(jamErr.contains(secretJamId))

        val secretYtKey = "super_secret_yt_key_888"
        val failingYtProvider = YouTubeMusicProvider(
            apiService = object : YouTubeApiService {
                override suspend fun searchMusicVideos(
                    part: String,
                    type: String,
                    videoCategoryId: String,
                    videoEmbeddable: String,
                    maxResults: Int,
                    query: String,
                    apiKey: String
                ): YouTubeSearchResponse = throw IllegalStateException("Request failed for key=$secretYtKey")

                override suspend fun getVideoDetails(
                    part: String,
                    videoIds: String,
                    apiKey: String
                ): YouTubeVideosResponse = throw IllegalStateException("Request failed for key=$secretYtKey")
            },
            apiKey = secretYtKey
        )
        val ytErr = failingYtProvider.searchTracks("test").exceptionOrNull()?.message.orEmpty()
        assertFalse(ytErr.contains(secretYtKey))
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
    fun `unified home catalog combines jamendo and audius and isolates single provider failures`() = runBlocking {
        val workingJamendoRepo = JamendoRepository(apiService = FakeJamendoApiService(), clientId = "sample_proto_id")
        val workingAudius = AudiusMusicProvider(apiService = FakeAudiusApiService())
        val disabledYt = YouTubeMusicProvider(apiService = FakeYouTubeApiService(), apiKey = YouTubeMusicProvider.PLACEHOLDER_API_KEY)

        val bothWorking = UnifiedMusicCatalogService(
            jamendoRepository = workingJamendoRepo,
            jamendoProvider = JamendoMusicProvider(workingJamendoRepo),
            audiusProvider = workingAudius,
            youTubeProvider = disabledYt
        ).loadHomeCatalog()

        assertTrue(bothWorking.trendingNow.any { it.providerId == ProviderId.JAMENDO })
        assertTrue(bothWorking.trendingNow.any { it.providerId == ProviderId.AUDIUS })
        assertTrue(bothWorking.popularTracks.any { it.providerId == ProviderId.JAMENDO })
        assertTrue(bothWorking.popularTracks.any { it.providerId == ProviderId.AUDIUS })
        assertTrue(bothWorking.providerFailures.isEmpty())

        // If Jamendo fails, Audius must still populate catalog
        val failingJamendoRepo = JamendoRepository(apiService = FailingJamendoApiService(), clientId = "sample_proto_id")
        val jamendoFailedCatalog = UnifiedMusicCatalogService(
            jamendoRepository = failingJamendoRepo,
            jamendoProvider = JamendoMusicProvider(failingJamendoRepo),
            audiusProvider = workingAudius,
            youTubeProvider = disabledYt
        ).loadHomeCatalog()

        assertTrue(jamendoFailedCatalog.trendingNow.any { it.providerId == ProviderId.AUDIUS })
        assertTrue(jamendoFailedCatalog.popularTracks.any { it.providerId == ProviderId.AUDIUS })
        assertTrue(jamendoFailedCatalog.providerFailures.any { it.providerId == ProviderId.JAMENDO })

        // If Audius fails, Jamendo must still populate catalog
        val audiusFailedCatalog = UnifiedMusicCatalogService(
            jamendoRepository = workingJamendoRepo,
            jamendoProvider = JamendoMusicProvider(workingJamendoRepo),
            audiusProvider = AudiusMusicProvider(apiService = FailingAudiusApiService()),
            youTubeProvider = disabledYt
        ).loadHomeCatalog()

        assertTrue(audiusFailedCatalog.trendingNow.any { it.providerId == ProviderId.JAMENDO })
        assertTrue(audiusFailedCatalog.popularTracks.any { it.providerId == ProviderId.JAMENDO })
        assertTrue(audiusFailedCatalog.providerFailures.any { it.providerId == ProviderId.AUDIUS })
    }

    @Test
    fun `unconfigured jamendo surfaces notice while audius continues populating home catalog`() = runTest(testDispatcher) {
        val unconfiguredJamendoRepo = JamendoRepository(
            apiService = FakeJamendoApiService(),
            clientId = JamendoRepository.PLACEHOLDER_CLIENT_ID
        )
        val workingAudius = AudiusMusicProvider(apiService = FakeAudiusApiService())
        val disabledYt = YouTubeMusicProvider(apiService = FakeYouTubeApiService(), apiKey = YouTubeMusicProvider.PLACEHOLDER_API_KEY)
        val unconfiguredVmStore = ViewModelStore()
        val unconfiguredVm = ViewModelProvider(
            unconfiguredVmStore,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MillesMusicViewModel(
                    jamendoRepository = unconfiguredJamendoRepo,
                    playbackEngine = FakePlaybackEngine(),
                    autoLoadCatalog = true,
                    audiusProvider = workingAudius,
                    youTubeProvider = disabledYt
                ) as T
            }
        )[MillesMusicViewModel::class.java]
        try {
            testScheduler.advanceTimeBy(100)
            testScheduler.runCurrent()
            assertTrue(unconfiguredVm.uiState.value.trendingNowTracks.any { it.providerId == ProviderId.AUDIUS })
            assertTrue(unconfiguredVm.uiState.value.catalogProviderNotices.any { it.providerId == ProviderId.JAMENDO && it.isUnconfigured })
        } finally {
            unconfiguredVmStore.clear()
        }
    }

    @Test
    fun `cross provider deduplicator merges matching recordings and prevents false merges on remixes or live versions`() {
        val jamOriginal = UnifiedTrack(
            trackId = "jam_101",
            providerId = ProviderId.JAMENDO,
            providerTrackId = "101",
            title = "Neon Horizon",
            artist = "Milles Artist",
            album = "Horizon LP",
            durationSeconds = 210,
            genre = "Electronic",
            releaseDate = "2026-02-01",
            artworkUrl = "https://cdn.example.com/jam.jpg",
            streamUrl = "https://cdn.example.com/jam.mp3",
            originalProviderUrl = "https://jamendo.com/track/101",
            licenseType = "CC BY",
            attributionInfo = "CC BY",
            licenseUrl = "https://creativecommons.org/licenses/by/4.0/",
            commercialUsePermitted = true,
            streamingPermitted = true,
            isrc = "USABC2600001"
        )
        val audOriginalSameIsrc = UnifiedTrack(
            trackId = "aud_202",
            providerId = ProviderId.AUDIUS,
            providerTrackId = "aud_202",
            title = "Neon Horizon (feat. Guest)",
            artist = "Milles Artist",
            album = "Audius Single",
            durationSeconds = 211,
            genre = "Electronic",
            releaseDate = "2026-02-01",
            artworkUrl = "https://cdn.example.com/aud.jpg",
            streamUrl = "https://cdn.example.com/aud.mp3",
            originalProviderUrl = "https://audius.co/milles/neon-horizon",
            licenseType = "Audius Public Streaming",
            attributionInfo = "Audius",
            commercialUsePermitted = false,
            streamingPermitted = true,
            isrc = "us-abc-26-00001"
        )
        val audRemix = audOriginalSameIsrc.copy(
            trackId = "aud_203",
            providerTrackId = "aud_203",
            title = "Neon Horizon (Club Remix)",
            isrc = null
        )
        val audLive = audOriginalSameIsrc.copy(
            trackId = "aud_204",
            providerTrackId = "aud_204",
            title = "Neon Horizon - Live Session",
            isrc = null
        )
        val audCover = audOriginalSameIsrc.copy(
            trackId = "aud_205",
            providerTrackId = "aud_205",
            title = "Neon Horizon (Acoustic Cover)",
            isrc = null
        )

        // Also test metadata-only match (no ISRC on either side)
        val jamMetaOnly = jamOriginal.copy(trackId = "jam_109", providerTrackId = "109", title = "Dawn Pulse", isrc = null, album = "Dawn EP")
        val audMetaOnly = audOriginalSameIsrc.copy(
            trackId = "aud_209",
            providerTrackId = "aud_209",
            title = "Dawn Pulse",
            isrc = null,
            album = "Audius Single"
        )

        val deduped = TrackDeduplicator.deduplicate(
            listOf(jamOriginal, audOriginalSameIsrc, audRemix, audLive, audCover, jamMetaOnly, audMetaOnly)
        )

        assertEquals(5, deduped.size)
        val mergedHorizon = deduped.first { it.title.startsWith("Neon Horizon") && !it.title.contains("Remix") }
        assertEquals(ProviderId.JAMENDO, mergedHorizon.providerId)
        assertEquals(2, mergedHorizon.resolvedSources().size)
        assertTrue(mergedHorizon.resolvedSources().any { it.providerId == ProviderId.JAMENDO })
        assertTrue(mergedHorizon.resolvedSources().any { it.providerId == ProviderId.AUDIUS })

        val mergedDawn = deduped.first { it.title == "Dawn Pulse" }
        assertEquals(2, mergedDawn.resolvedSources().size)
        assertTrue(deduped.any { it.providerTrackId == "aud_203" })
        assertTrue(deduped.any { it.providerTrackId == "aud_204" })
        assertTrue(deduped.any { it.providerTrackId == "aud_205" })
    }

    @Test
    fun `trends now ranking uses genuine provider metrics and never fabricates missing stats`() {
        val jamLow = UnifiedTrack(
            trackId = "jam_low",
            providerId = ProviderId.JAMENDO,
            providerTrackId = "j_low",
            title = "Quiet Breeze",
            artist = "Artist A",
            album = "LP",
            durationSeconds = 180,
            genre = "Ambient",
            releaseDate = "2020-01-01",
            streamUrl = "https://cdn.example.com/j1.mp3",
            licenseType = "CC BY",
            attributionInfo = "CC BY",
            streamingPermitted = true,
            trendMetrics = com.example.model.TrendMetrics(
                providerRank = 10,
                listensOrPlays = 50,
                sourceChartLabel = "Jamendo Charts"
            )
        )
        val audHigh = UnifiedTrack(
            trackId = "aud_high",
            providerId = ProviderId.AUDIUS,
            providerTrackId = "a_high",
            title = "Peak Surge",
            artist = "Artist B",
            album = "Single",
            durationSeconds = 200,
            genre = "Electronic",
            releaseDate = "2026-11-24",
            streamUrl = "https://cdn.example.com/a1.mp3",
            licenseType = "Audius",
            attributionInfo = "Audius",
            streamingPermitted = true,
            trendMetrics = com.example.model.TrendMetrics(
                providerRank = 1,
                listensOrPlays = 250_000,
                favorites = 12_000,
                reposts = 3_400,
                sourceChartLabel = "Audius Trending"
            )
        )
        val noStatsTrack = jamLow.copy(
            trackId = "jam_nostats",
            providerTrackId = "j_nostats",
            title = "Unmeasured Track",
            trendMetrics = com.example.model.TrendMetrics(providerRank = 5)
        )

        val ranked = TrendingRankingEngine.rankUnifiedTrending(
            providerBatches = mapOf(
                ProviderId.JAMENDO to listOf(jamLow, noStatsTrack),
                ProviderId.AUDIUS to listOf(audHigh)
            ),
            nowMillis = 1795651200000L // 2026-11-26 UTC approx
        )

        assertEquals("a_high", ranked.first().providerTrackId)
        assertNull(noStatsTrack.trendMetrics?.listensOrPlays)
        assertEquals("#5", formatTrendMetricsLabel(noStatsTrack.trendMetrics))
        assertTrue(formatTrendMetricsLabel(audHigh.trendMetrics)?.contains("250.0K plays") == true)
    }

    @Test
    fun `playback router and view model automatically fail over to compatible backup source on error`() = runTest(testDispatcher) {
        val fakePlayer = FakePlaybackEngine()
        val repo = JamendoRepository(apiService = FakeJamendoApiService(), clientId = "sample_proto_id")
        val audiusProvider = AudiusMusicProvider(apiService = FakeAudiusApiService())
        val youTubeProvider = YouTubeMusicProvider(apiService = FakeYouTubeApiService(), apiKey = "sample_youtube_key")
        val store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = MillesMusicViewModel(
                    jamendoRepository = repo,
                    playbackEngine = fakePlayer,
                    autoLoadCatalog = false,
                    audiusProvider = audiusProvider,
                    youTubeProvider = youTubeProvider
                ) as T
            }
        )[MillesMusicViewModel::class.java]

        try {
            val jamSource = com.example.model.TrackSource(
                providerId = ProviderId.JAMENDO,
                providerTrackId = "101",
                streamUrl = "https://cdn.example.com/primary_broken.mp3",
                originalProviderUrl = "https://jamendo.com/track/101",
                licenseType = "CC BY",
                attributionInfo = "CC BY",
                commercialUsePermitted = true,
                streamingPermitted = true
            )
            val audSource = com.example.model.TrackSource(
                providerId = ProviderId.AUDIUS,
                providerTrackId = "aud_202",
                streamUrl = "https://cdn.example.com/backup_audius.mp3",
                originalProviderUrl = "https://audius.co/track/202",
                licenseType = "Audius",
                attributionInfo = "Audius",
                commercialUsePermitted = false,
                streamingPermitted = true
            )
            val multiSourceTrack = Track(
                id = "jam_101",
                title = "Failover Anthem",
                artist = "Milles Artist",
                album = "Dual LP",
                durationSeconds = 200,
                genre = "Electronic",
                artwork = com.example.model.ArtworkDescriptor(),
                streamUrl = jamSource.streamUrl,
                providerId = ProviderId.JAMENDO,
                providerTrackId = "101",
                sources = listOf(jamSource, audSource)
            )

            // 1. Initial resolution chooses Jamendo without asking user
            val initial = PlaybackRouter.resolveInitialPlayback(multiSourceTrack)
            assertTrue(initial is PlaybackResolution.AudioPlayback)
            assertEquals(ProviderId.JAMENDO, (initial as PlaybackResolution.AudioPlayback).activeSource.providerId)

            // 2. Play track via ViewModel, then emit PlaybackError on primary source
            vm.playTrack(multiSourceTrack, openPlayer = true)
            testScheduler.runCurrent()
            assertEquals(ProviderId.JAMENDO, vm.uiState.value.currentTrack?.providerId)
            assertEquals("https://cdn.example.com/primary_broken.mp3", fakePlayer.lastPlayedQueue.first().streamUrl)

            fakePlayer.emitError("HTTP 503", failedTrackId = multiSourceTrack.id)
            testScheduler.runCurrent()

            // ViewModel automatically fails over to Audius backup source in the same player UI
            assertEquals(ProviderId.AUDIUS, vm.uiState.value.currentTrack?.providerId)
            assertEquals("https://cdn.example.com/backup_audius.mp3", fakePlayer.lastPlayedQueue.first().streamUrl)
            assertTrue(vm.uiState.value.isNowPlayingOpen)
            assertNull(vm.uiState.value.playbackErrorMessage)
        } finally {
            store.clear()
        }
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

    @Test
    fun `live unified Home catalog loads available providers concurrently and preserves provider identity`() = runBlocking {
        val jamendoRepo = JamendoRepository()
        val audius = AudiusMusicProvider()
        val youtube = YouTubeMusicProvider()
        val catalogService = UnifiedMusicCatalogService(
            jamendoRepository = jamendoRepo,
            jamendoProvider = JamendoMusicProvider(jamendoRepo),
            audiusProvider = audius,
            youTubeProvider = youtube
        )

        val home = catalogService.loadHomeCatalog(forceRefresh = true)
        assertTrue("Expected non-empty unified Trending Now", home.trendingNow.isNotEmpty())
        assertTrue("Expected non-empty unified Popular Tracks", home.popularTracks.isNotEmpty())
        assertTrue("Expected non-empty unified Recommended Music", home.recommendedTracks.isNotEmpty())
        assertTrue("Expected non-empty unified New Releases", home.newReleases.isNotEmpty())
        assertTrue("Home catalog must never contain YouTube embed-only tracks", (home.trendingNow + home.popularTracks + home.recommendedTracks + home.newReleases).none { it.providerId == ProviderId.YOUTUBE })

        val jamTrend = home.trendingNow.count { it.providerId == ProviderId.JAMENDO }
        val audTrend = home.trendingNow.count { it.providerId == ProviderId.AUDIUS }
        val jamPop = home.popularTracks.count { it.providerId == ProviderId.JAMENDO }
        val audPop = home.popularTracks.count { it.providerId == ProviderId.AUDIUS }
        val jamRec = home.recommendedTracks.count { it.providerId == ProviderId.JAMENDO }
        val audRec = home.recommendedTracks.count { it.providerId == ProviderId.AUDIUS }
        val jamNew = home.newReleases.count { it.providerId == ProviderId.JAMENDO }
        val audNew = home.newReleases.count { it.providerId == ProviderId.AUDIUS }

        println(
            "UNIFIED_HOME_LIVE_SUMMARY jamendoConfigured=${jamendoRepo.isConfigured} " +
                "youtubeConfigured=${youtube.isEnabled} " +
                "trendingTotal=${home.trendingNow.size}(jam=$jamTrend,aud=$audTrend) " +
                "popularTotal=${home.popularTracks.size}(jam=$jamPop,aud=$audPop) " +
                "recommendedTotal=${home.recommendedTracks.size}(jam=$jamRec,aud=$audRec) " +
                "newTotal=${home.newReleases.size}(jam=$jamNew,aud=$audNew) " +
                "failures=${home.providerFailures.map { "${it.providerId.id}:unconfigured=${it.isUnconfigured}" }}"
        )
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
            offset: Int?,
            audioFormat: String,
            include: String,
            order: String?,
            boost: String?,
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
                    licenseCcUrl = "https://creativecommons.org/licenses/by-nc-sa/3.0/",
                    stats = JamendoTrackStatsDto(
                        rateListenedTotal = 14200,
                        rateDownloadsTotal = 850,
                        playlisted = 120,
                        favorited = 410
                    )
                )
            )
        )

        override suspend fun getArtists(
            clientId: String,
            format: String,
            limit: Int,
            offset: Int?,
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
            offset: Int?,
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

    private class FailingJamendoApiService : JamendoApiService {
        private fun fail(): Nothing = throw IOException("Simulated Jamendo timeout")
        override suspend fun getTracks(
            clientId: String,
            format: String,
            limit: Int,
            offset: Int?,
            audioFormat: String,
            include: String,
            order: String?,
            boost: String?,
            search: String?,
            tags: String?,
            featured: Int?,
            artistId: String?,
            albumId: String?
        ): JamendoTracksResponse = fail()

        override suspend fun getArtists(
            clientId: String,
            format: String,
            limit: Int,
            offset: Int?,
            order: String?,
            nameSearch: String?,
            hasImage: Boolean?
        ): JamendoArtistsResponse = fail()

        override suspend fun getAlbums(
            clientId: String,
            format: String,
            limit: Int,
            offset: Int?,
            order: String?,
            nameSearch: String?,
            artistId: String?
        ): JamendoAlbumsResponse = fail()
    }

    private class FakeAudiusApiService(private val streamGated: Boolean = false) : AudiusApiService {
        private val sampleTrack = AudiusTrackDto(
            id = "a1",
            title = "Midnight Signal",
            duration = 195,
            genre = "Electronic",
            permalink = "/audiuscreator/midnight-signal",
            playCount = 32400,
            repostCount = 410,
            favoriteCount = 1890,
            releaseDate = "2026-11-20T00:00:00Z",
            isAvailable = true,
            isDelete = false,
            isStreamable = !streamGated,
            isStreamGated = streamGated,
            artwork = AudiusArtworkDto(medium = "https://cdn.example.com/audius_cover.jpg"),
            stream = AudiusStreamDto(url = "https://cdn.example.com/audius_stream.mp3"),
            access = AudiusAccessDto(stream = !streamGated),
            user = AudiusUserDto(id = "u1", name = "Audius Creator", handle = "audiuscreator")
        )

        override suspend fun getTrendingTracks(time: String, limit: Int, offset: Int, genre: String?, appName: String) =
            AudiusTracksResponse(data = listOf(sampleTrack))

        override suspend fun searchTracks(query: String, limit: Int, offset: Int, appName: String) =
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
        override suspend fun getTrendingTracks(time: String, limit: Int, offset: Int, genre: String?, appName: String): AudiusTracksResponse = fail()
        override suspend fun searchTracks(query: String, limit: Int, offset: Int, appName: String): AudiusTracksResponse = fail()
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
        var lastPlayedQueue: List<Track> = emptyList()
        private var playing: Boolean = false

        fun emitError(message: String, failedTrackId: String? = null) {
            playing = false
            _events.tryEmit(PlaybackEngineEvent.PlaybackError(message, failedTrackId))
        }

        override fun setQueueAndPlay(tracks: List<Track>, startIndex: Int) {
            lastPlayedQueue = tracks
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

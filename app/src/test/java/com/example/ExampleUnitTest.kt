package com.example

import com.example.audio.AudioStreamPlayer
import com.example.data.JamendoRepository
import com.example.model.ThemeMode
import com.example.network.JamendoApiService
import com.example.network.JamendoTrackDto
import com.example.network.JamendoTracksResponse
import com.example.state.MillesMusicViewModel
import com.example.state.formatDuration
import com.example.state.toPlayerUiState
import com.example.state.toUiTrack
import com.example.ui.components.MillesTab
import com.example.ui.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `view model maps to PlayerUiState and supports 4 MillesTab destinations`() {
        val vm = MillesMusicViewModel(
            jamendoRepository = JamendoRepository(
                apiService = FakeJamendoApiService(),
                clientId = JamendoRepository.PLACEHOLDER_CLIENT_ID
            ),
            streamPlayer = FakeAudioStreamPlayer()
        )
        assertEquals(4, MillesTab.entries.size)
        assertFalse(vm.isJamendoConfigured)

        vm.selectTab(MillesTab.Library)
        assertEquals(MillesTab.Library, vm.uiState.value.activeTab)

        val initialCount = vm.uiState.value.userPlaylists.size
        vm.createPlaylist("Midnight Groove", "Late night drive")
        assertEquals(initialCount + 1, vm.uiState.value.userPlaylists.size)
        assertEquals("Midnight Groove", vm.uiState.value.userPlaylists.first().title)

        vm.selectThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)

        vm.togglePlayPause()
        assertTrue(vm.uiState.value.isPlaying)

        val playerUiState = vm.uiState.value.toPlayerUiState()
        assertTrue(playerUiState.isPlaying)
        assertEquals(vm.uiState.value.currentTrack.id, playerUiState.track.id)
        assertTrue(BuildConfig.JAMENDO_CLIENT_ID.isNotBlank())
    }

    @Test
    fun `jamendo repository loads and streams tracks when configured`() = runTest(testDispatcher) {
        val fakePlayer = FakeAudioStreamPlayer()
        val repo = JamendoRepository(
            apiService = FakeJamendoApiService(),
            clientId = "sample_proto_id"
        )
        val vm = MillesMusicViewModel(jamendoRepository = repo, streamPlayer = fakePlayer)
        assertTrue(vm.isJamendoConfigured)

        // Run init catalog load coroutine without looping infinitely on ticker
        testScheduler.advanceTimeBy(100)
        testScheduler.runCurrent()

        val loadedTrack = vm.uiState.value.trendingTracks.first()
        assertEquals("jam_101", loadedTrack.id)
        assertEquals("https://cdn.example.com/cover.jpg", loadedTrack.toUiTrack().coverUrl)

        vm.playTrack(loadedTrack, openPlayer = true)
        assertEquals("https://cdn.example.com/audio.mp3", fakePlayer.lastPlayedUrl)
        assertTrue(vm.uiState.value.isPlaying)

        vm.updateSearchQuery("Solar")
        testScheduler.advanceTimeBy(400)
        testScheduler.runCurrent()
        assertEquals(1, vm.uiState.value.searchResults.size)
        assertEquals("jam_101", vm.uiState.value.searchResults.first().id)
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
            tags: String?
        ): JamendoTracksResponse {
            return JamendoTracksResponse(
                results = listOf(
                    JamendoTrackDto(
                        id = "101",
                        name = "Solar Echoes",
                        duration = 210,
                        artistName = "Milles Artist",
                        albumName = "Prototype LP",
                        albumImage = "https://cdn.example.com/cover.jpg",
                        audio = "https://cdn.example.com/audio.mp3"
                    )
                )
            )
        }
    }

    private class FakeAudioStreamPlayer : AudioStreamPlayer {
        var lastPlayedUrl: String? = null
        private var playing: Boolean = false

        override fun playStream(
            url: String,
            onPrepared: (durationSeconds: Int) -> Unit,
            onCompletion: () -> Unit,
            onError: (String) -> Unit
        ) {
            lastPlayedUrl = url
            playing = true
            onPrepared(210)
        }

        override fun pause() {
            playing = false
        }

        override fun resume() {
            playing = true
        }

        override fun seekToMs(positionMs: Int) = Unit
        override fun currentPositionSeconds(): Int = 0
        override fun isActivelyPlaying(): Boolean = playing
        override fun stopAndReset() {
            playing = false
        }

        override fun release() {
            playing = false
        }
    }
}

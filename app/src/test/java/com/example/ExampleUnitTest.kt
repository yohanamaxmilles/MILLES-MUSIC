package com.example

import com.example.model.MainTab
import com.example.model.ThemeMode
import com.example.state.MillesMusicViewModel
import com.example.state.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `formatDuration formats seconds into mm ss`() {
        assertEquals("00:45", formatDuration(45))
        assertEquals("03:52", formatDuration(232))
    }

    @Test
    fun `view model handles tabs, playlist creation, theme switching, and playback`() {
        val vm = MillesMusicViewModel()
        vm.selectTab(MainTab.LIBRARY)
        assertEquals(MainTab.LIBRARY, vm.uiState.value.activeTab)

        val initialCount = vm.uiState.value.userPlaylists.size
        vm.createPlaylist("Midnight Groove", "Late night drive")
        assertEquals(initialCount + 1, vm.uiState.value.userPlaylists.size)
        assertEquals("Midnight Groove", vm.uiState.value.userPlaylists.first().title)

        vm.selectThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)

        vm.togglePlayPause()
        assertTrue(vm.uiState.value.isPlaying)
    }
}

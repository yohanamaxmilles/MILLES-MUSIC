package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ThemeMode
import com.example.state.MillesMusicViewModel
import com.example.ui.components.MillesTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches MILLES MUSIC`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MILLES MUSIC", appName)
    }

    @Test
    fun `view model handles playlist creation, favorites, and theme switching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vm = MillesMusicViewModel.factory(context).create(MillesMusicViewModel::class.java)
        vm.selectTab(MillesTab.Library)
        assertEquals(MillesTab.Library, vm.uiState.value.activeTab)

        val initialCount = vm.uiState.value.userPlaylists.size
        vm.createPlaylist("Midnight Groove", "Late night drive")
        assertEquals(initialCount + 1, vm.uiState.value.userPlaylists.size)
        assertEquals("Midnight Groove", vm.uiState.value.userPlaylists.first().title)

        vm.selectThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
        assertTrue(vm.uiState.value.favoriteTrackIds.isEmpty())
    }
}

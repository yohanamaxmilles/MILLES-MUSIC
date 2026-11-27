package com.example.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

/**
 * Creates a controller connection from the app/UI layer to the playback service.
 * Keep the returned future for the lifetime of the host and release it when the
 * host is destroyed using MediaController.releaseFuture(future).
 */
object MillesPlaybackConnector {
    fun connect(context: Context): ListenableFuture<MediaController> {
        val appContext = context.applicationContext
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MillesPlaybackService::class.java)
        )
        return MediaController.Builder(appContext, sessionToken).buildAsync()
    }
}

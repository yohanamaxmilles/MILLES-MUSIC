package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.model.RepeatMode
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface PlaybackEngineEvent {
    data class IsPlayingChanged(val isPlaying: Boolean) : PlaybackEngineEvent
    data class BufferingChanged(val isBuffering: Boolean) : PlaybackEngineEvent
    data class TrackTransitioned(val trackId: String, val durationSeconds: Int?) : PlaybackEngineEvent
    data class ShuffleChanged(val enabled: Boolean) : PlaybackEngineEvent
    data class RepeatChanged(val mode: RepeatMode) : PlaybackEngineEvent
    data class PlaybackError(val message: String) : PlaybackEngineEvent
    data object QueueEnded : PlaybackEngineEvent
}

interface PlaybackEngine {
    val events: Flow<PlaybackEngineEvent>

    fun setQueueAndPlay(tracks: List<Track>, startIndex: Int)
    fun play()
    fun pause()
    fun seekToNext()
    fun seekToPrevious()
    fun seekToMs(positionMs: Long)
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: RepeatMode)
    fun currentPositionSeconds(): Int
    fun currentDurationSeconds(): Int?
    fun isPlaying(): Boolean
    fun release()
}

class MillesMediaController(context: Context) : PlaybackEngine {

    private val appContext = context.applicationContext
    private val _events = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 32)
    override val events: Flow<PlaybackEngineEvent> = _events.asSharedFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private var pendingTracks: List<Track>? = null
    private var pendingStartIndex: Int = 0
    private var pendingShuffle: Boolean? = null
    private var pendingRepeat: RepeatMode? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _events.tryEmit(PlaybackEngineEvent.IsPlayingChanged(isPlaying))
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _events.tryEmit(
                PlaybackEngineEvent.BufferingChanged(playbackState == Player.STATE_BUFFERING)
            )
            if (playbackState == Player.STATE_READY) {
                val mediaId = controller?.currentMediaItem?.mediaId
                val durSec = currentDurationSeconds()
                if (!mediaId.isNullOrBlank()) {
                    _events.tryEmit(PlaybackEngineEvent.TrackTransitioned(mediaId, durSec))
                }
            } else if (playbackState == Player.STATE_ENDED) {
                _events.tryEmit(PlaybackEngineEvent.QueueEnded)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: return
            _events.tryEmit(PlaybackEngineEvent.TrackTransitioned(id, currentDurationSeconds()))
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _events.tryEmit(PlaybackEngineEvent.ShuffleChanged(shuffleModeEnabled))
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            val mapped = when (repeatMode) {
                Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                else -> RepeatMode.OFF
            }
            _events.tryEmit(PlaybackEngineEvent.RepeatChanged(mapped))
        }

        override fun onPlayerError(error: PlaybackException) {
            val message = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network error while streaming track. Check connection and retry."
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                    "Track stream unavailable from Jamendo."
                else -> "Unable to play this audio stream."
            }
            _events.tryEmit(PlaybackEngineEvent.PlaybackError(message))
        }
    }

    init {
        connectIfNeeded()
    }

    private fun connectIfNeeded() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, MillesPlaybackService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val connected = future.get()
                    controller = connected
                    connected.addListener(playerListener)
                    pendingShuffle?.let {
                        connected.shuffleModeEnabled = it
                        pendingShuffle = null
                    }
                    pendingRepeat?.let {
                        connected.repeatMode = it.toMedia3RepeatMode()
                        pendingRepeat = null
                    }
                    pendingTracks?.let { queued ->
                        val start = pendingStartIndex
                        pendingTracks = null
                        applyQueueAndPlay(connected, queued, start)
                    }
                } catch (_: Throwable) {
                    _events.tryEmit(
                        PlaybackEngineEvent.PlaybackError("Playback service unavailable.")
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    override fun setQueueAndPlay(tracks: List<Track>, startIndex: Int) {
        val playable = tracks.filter { !it.streamUrl.isNullOrBlank() }
        if (playable.isEmpty()) {
            _events.tryEmit(PlaybackEngineEvent.PlaybackError("No playable stream URL for this track."))
            return
        }
        val targetTrack = tracks.getOrNull(startIndex)
        val resolvedIndex = if (targetTrack != null) {
            playable.indexOfFirst { it.id == targetTrack.id }.coerceAtLeast(0)
        } else {
            0
        }

        val activeController = controller
        if (activeController == null) {
            pendingTracks = playable
            pendingStartIndex = resolvedIndex
            connectIfNeeded()
        } else {
            applyQueueAndPlay(activeController, playable, resolvedIndex)
        }
    }

    private fun applyQueueAndPlay(
        activeController: MediaController,
        playable: List<Track>,
        startIndex: Int
    ) {
        val mediaItems = playable.map { track ->
            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .apply {
                    track.coverUrl?.takeIf { it.isNotBlank() }?.let { setArtworkUri(Uri.parse(it)) }
                }
                .build()
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(Uri.parse(track.streamUrl))
                .setMediaMetadata(metadata)
                .build()
        }
        activeController.setMediaItems(mediaItems, startIndex, 0L)
        activeController.prepare()
        activeController.play()
    }

    override fun play() {
        controller?.play()
    }

    override fun pause() {
        controller?.pause()
    }

    override fun seekToNext() {
        controller?.let {
            if (it.hasNextMediaItem()) {
                it.seekToNextMediaItem()
                it.play()
            } else if (it.mediaItemCount > 0) {
                it.seekToDefaultPosition(0)
                it.play()
            }
        }
    }

    override fun seekToPrevious() {
        controller?.let {
            if (it.currentPosition > 4_000L) {
                it.seekTo(0L)
            } else if (it.hasPreviousMediaItem()) {
                it.seekToPreviousMediaItem()
                it.play()
            } else {
                it.seekTo(0L)
            }
        }
    }

    override fun seekToMs(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        val activeController = controller
        if (activeController == null) {
            pendingShuffle = enabled
        } else {
            activeController.shuffleModeEnabled = enabled
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        val activeController = controller
        if (activeController == null) {
            pendingRepeat = mode
        } else {
            activeController.repeatMode = mode.toMedia3RepeatMode()
        }
    }

    override fun currentPositionSeconds(): Int {
        val posMs = controller?.currentPosition ?: 0L
        return (posMs / 1000L).toInt().coerceAtLeast(0)
    }

    override fun currentDurationSeconds(): Int? {
        val durMs = controller?.duration ?: C.TIME_UNSET
        if (durMs == C.TIME_UNSET || durMs <= 0L) return null
        return (durMs / 1000L).toInt().coerceAtLeast(1)
    }

    override fun isPlaying(): Boolean = controller?.isPlaying == true

    override fun release() {
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun RepeatMode.toMedia3RepeatMode(): Int = when (this) {
        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }
}

package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer

interface AudioStreamPlayer {
    fun playStream(
        url: String,
        onPrepared: (durationSeconds: Int) -> Unit,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    )

    fun pause()
    fun resume()
    fun seekToMs(positionMs: Int)
    fun currentPositionSeconds(): Int?
    fun isActivelyPlaying(): Boolean
    fun stopAndReset()
    fun release()
}

class JamendoStreamPlayer : AudioStreamPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var prepared: Boolean = false

    override fun playStream(
        url: String,
        onPrepared: (durationSeconds: Int) -> Unit,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    ) {
        stopAndReset()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { mp ->
                    prepared = true
                    val durationSec = (mp.duration / 1000).coerceAtLeast(1)
                    mp.start()
                    onPrepared(durationSec)
                }
                setOnCompletionListener {
                    prepared = false
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    prepared = false
                    onError("Unable to stream audio track.")
                    true
                }
                setDataSource(url)
                prepareAsync()
            }
            mediaPlayer = player
        } catch (_: Throwable) {
            prepared = false
            onError("Audio playback unavailable on this device.")
        }
    }

    override fun pause() {
        try {
            val player = mediaPlayer ?: return
            if (prepared && player.isPlaying) {
                player.pause()
            }
        } catch (_: Throwable) {
        }
    }

    override fun resume() {
        try {
            val player = mediaPlayer ?: return
            if (prepared && !player.isPlaying) {
                player.start()
            }
        } catch (_: Throwable) {
        }
    }

    override fun seekToMs(positionMs: Int) {
        try {
            val player = mediaPlayer ?: return
            if (prepared) {
                player.seekTo(positionMs.coerceAtLeast(0))
            }
        } catch (_: Throwable) {
        }
    }

    override fun currentPositionSeconds(): Int? {
        return try {
            val player = mediaPlayer ?: return null
            if (!prepared) return null
            (player.currentPosition / 1000).coerceAtLeast(0)
        } catch (_: Throwable) {
            null
        }
    }

    override fun isActivelyPlaying(): Boolean {
        return try {
            val player = mediaPlayer ?: return false
            prepared && player.isPlaying
        } catch (_: Throwable) {
            false
        }
    }

    override fun stopAndReset() {
        prepared = false
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Throwable) {
        } finally {
            mediaPlayer = null
        }
    }

    override fun release() {
        stopAndReset()
    }
}

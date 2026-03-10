package com.asuka.player.app

import android.content.Context
import android.media.AudioManager
import android.view.Window
import com.asuka.player.core.PlaybackDeviceController
import com.asuka.player.core.PlaybackDeviceControllerFactory
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.PlaybackUiPersistence

class PlaybackStateUiPersistence(
    private val playbackStateRepository: PlaybackStateRepository,
    private val playbackBehaviorRepository: PlaybackBehaviorRepository,
) : PlaybackUiPersistence {
    override fun readZoom(mediaId: String): Float? = playbackStateRepository.readResumeState(mediaId).zoom

    override fun savePlaybackSpeed(mediaId: String, speed: Float) {
        playbackStateRepository.savePlaybackSpeed(mediaId, speed)
    }

    override fun saveAudioTrack(mediaId: String, trackId: String) {
        playbackStateRepository.saveAudioTrack(mediaId, trackId)
    }

    override fun saveSubtitleTrack(mediaId: String, trackId: String) {
        playbackStateRepository.saveSubtitleTrack(mediaId, trackId)
    }

    override fun disableSubtitles(mediaId: String) {
        playbackStateRepository.disableSubtitles(mediaId)
    }

    override fun saveZoom(mediaId: String, zoom: Float) {
        playbackStateRepository.saveZoom(mediaId, zoom)
    }

    override fun readRememberedBrightness(): Float? = playbackBehaviorRepository.rememberedBrightness

    override fun saveRememberedBrightness(brightness: Float) {
        playbackBehaviorRepository.rememberedBrightness = brightness
    }
}

object DefaultPlaybackDeviceControllerFactory : PlaybackDeviceControllerFactory {
    override fun create(
        context: Context,
        window: Window,
    ): PlaybackDeviceController {
        return WindowPlaybackDeviceController(
            appContext = context.applicationContext,
            window = window,
        )
    }
}

private class WindowPlaybackDeviceController(
    private val appContext: Context,
    private val window: Window,
) : PlaybackDeviceController {
    override fun currentVolumePercent(): Int {
        val audioManager = appContext.getSystemService(AudioManager::class.java) ?: return 50
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max) * 100f).toInt().coerceIn(0, 100)
    }

    override fun setVolumePercent(percent: Int) {
        val audioManager = appContext.getSystemService(AudioManager::class.java) ?: return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val level = ((percent.coerceIn(0, 100) / 100f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
    }

    override fun currentBrightnessPercent(): Int {
        val current = window.attributes.screenBrightness
        return if (current >= 0f) {
            (current * 100f).toInt().coerceIn(0, 100)
        } else {
            50
        }
    }

    override fun setBrightnessPercent(percent: Int) {
        val attrs = window.attributes
        attrs.screenBrightness = (percent.coerceIn(0, 100) / 100f).coerceIn(0f, 1f)
        window.attributes = attrs
    }
}

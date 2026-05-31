package com.asuka.player.runtime

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.Window
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.platform.isTransientPlaybackMediaId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackStateUiPersistence(
    private val playbackStateRepository: PlaybackStateRepository,
    private val playbackBehaviorRepository: PlaybackBehaviorRepository,
    private val scope: CoroutineScope,
) : PlaybackUiPersistence {
    private val brightnessWriteMutex = Mutex()

    override suspend fun readZoom(mediaId: String): Float? {
        if (mediaId.isTransientPlaybackMediaId()) return null
        return playbackStateRepository.readResumeState(mediaId).zoom
    }

    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        if (mediaId.isTransientPlaybackMediaId()) return
        playbackStateRepository.saveZoom(mediaId, zoom)
    }

    override fun readRememberedBrightness(): Float? = playbackBehaviorRepository.rememberedBrightness

    override fun saveRememberedBrightness(brightness: Float) {
        scope.launch(Dispatchers.IO) {
            brightnessWriteMutex.withLock {
                playbackBehaviorRepository.setRememberedBrightness(brightness)
            }
        }
    }
}

object DefaultPlaybackDeviceControllerFactory : PlaybackDeviceControllerFactory {
    override fun create(activity: Activity): PlaybackDeviceController {
        return WindowPlaybackDeviceController(
            appContext = activity.applicationContext,
            window = activity.window,
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

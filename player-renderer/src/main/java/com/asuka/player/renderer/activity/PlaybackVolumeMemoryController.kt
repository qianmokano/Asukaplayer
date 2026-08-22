package com.asuka.player.renderer.activity

import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.PlayerSettings

internal class PlaybackVolumeMemoryController(
    private val playbackUiPersistence: PlaybackUiPersistence,
    private val playbackDeviceController: PlaybackDeviceController,
) {
    private var restoreCompleted = false
    private var playbackWasReady = false

    suspend fun restoreIfNeeded(
        settings: PlayerSettings,
        isPlaybackReady: Boolean,
    ) {
        if (!isPlaybackReady || restoreCompleted) return
        playbackWasReady = true
        if (!settings.rememberVolume) return

        val rememberedVolume = runCatching {
            playbackUiPersistence.readRememberedVolumePercent()
        }.getOrNull()

        if (rememberedVolume == null) {
            restoreCompleted = true
            return
        }

        restoreCompleted = runCatching {
            playbackDeviceController.setVolumePercent(rememberedVolume.coerceIn(0, 100))
        }.isSuccess
    }

    fun saveIfNeeded(settings: PlayerSettings) {
        if (!settings.rememberVolume || !playbackWasReady) return

        val currentVolume = runCatching {
            playbackDeviceController.currentVolumePercent()
        }.getOrNull() ?: return

        runCatching {
            playbackUiPersistence.saveRememberedVolumePercent(currentVolume.coerceIn(0, 100))
        }
    }
}

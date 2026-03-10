package com.asuka.player.ui.controller

import com.asuka.player.core.PlaybackController
import com.asuka.player.core.PlaybackUiPersistence

/**
 * Connects overlay actions to persistence when applicable.
 */
class OverlayActions(
    private val controller: PlaybackController,
    private val playbackPersistence: PlaybackUiPersistence,
    private val mediaIdProvider: () -> String?,
) {
    fun setSpeed(value: Float) {
        controller.setPlaybackSpeed(value)
        mediaIdProvider()?.let { playbackPersistence.savePlaybackSpeed(it, value) }
    }

    fun setScale(mode: com.asuka.player.core.VideoScaleMode) {
        controller.setVideoScaleMode(mode)
    }
}

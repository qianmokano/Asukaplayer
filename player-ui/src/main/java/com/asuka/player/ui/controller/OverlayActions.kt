package com.asuka.player.ui.controller

import com.asuka.player.core.PlaybackController
import com.asuka.player.data.PlaybackStore

/**
 * Connects overlay actions to persistence when applicable.
 */
class OverlayActions(
    private val controller: PlaybackController,
    private val store: PlaybackStore,
    private val mediaIdProvider: () -> String?,
) {
    fun setSpeed(value: Float) {
        controller.setPlaybackSpeed(value)
        mediaIdProvider()?.let { store.savePlaybackSpeed(it, value) }
    }

    fun setScale(mode: com.asuka.player.core.VideoScaleMode) {
        controller.setVideoScaleMode(mode)
    }
}

package com.asuka.player.ui.controller

import com.asuka.player.core.PlaybackController
import com.asuka.player.core.PlaybackStateRepository

/**
 * Connects overlay actions to persistence when applicable.
 */
class OverlayActions(
    private val controller: PlaybackController,
    private val playbackStateRepository: PlaybackStateRepository,
    private val mediaIdProvider: () -> String?,
) {
    fun setSpeed(value: Float) {
        controller.setPlaybackSpeed(value)
        mediaIdProvider()?.let { playbackStateRepository.savePlaybackSpeed(it, value) }
    }

    fun setScale(mode: com.asuka.player.core.VideoScaleMode) {
        controller.setVideoScaleMode(mode)
    }
}

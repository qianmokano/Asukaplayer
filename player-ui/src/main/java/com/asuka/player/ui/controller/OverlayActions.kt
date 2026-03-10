package com.asuka.player.ui.controller

import com.asuka.player.core.PlaybackController

/**
 * Connects overlay actions to playback commands.
 */
class OverlayActions(
    private val controller: PlaybackController,
) {
    fun setSpeed(value: Float) {
        controller.setPlaybackSpeed(value)
    }

    fun setScale(mode: com.asuka.player.core.VideoScaleMode) {
        controller.setVideoScaleMode(mode)
    }
}

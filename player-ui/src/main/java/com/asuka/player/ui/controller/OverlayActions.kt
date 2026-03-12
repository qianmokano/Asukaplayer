package com.asuka.player.ui.controller

import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController

/**
 * Connects overlay actions to playback commands.
 */
class OverlayActions(
    private val controller: PlaybackController,
) {
    fun setSpeed(value: Float) {
        controller.setPlaybackSpeed(value)
    }

    fun setScale(mode: com.asuka.player.contract.VideoScaleMode) {
        controller.setVideoScaleMode(mode)
    }

    fun setLoopMode(mode: LoopMode) {
        controller.setLoopMode(mode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        controller.setShuffleEnabled(enabled)
    }
}

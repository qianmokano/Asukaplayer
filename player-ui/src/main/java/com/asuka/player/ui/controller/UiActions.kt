package com.asuka.player.ui.controller

import com.asuka.player.core.LoopMode
import com.asuka.player.core.PlaybackController

class UiActions(private val controller: PlaybackController) {
    fun onLoop() {
        val nextMode = when (controller.getRepeatMode()) {
            LoopMode.OFF -> LoopMode.ONE
            LoopMode.ONE -> LoopMode.ALL
            LoopMode.ALL -> LoopMode.OFF
        }
        controller.setLoopMode(nextMode)
    }

    fun onShuffle() {
        controller.setShuffleEnabled(!controller.isShuffleEnabled())
    }
}

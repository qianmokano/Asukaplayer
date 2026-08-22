package com.asuka.player.ui.controller

import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.SeekIntervalPolicy

class PlaybackSeekActions(
    private val controller: PlaybackController,
    seekBackwardSec: Int,
    seekForwardSec: Int,
) {
    private val backwardMs = SeekIntervalPolicy.toMilliseconds(seekBackwardSec)
    private val forwardMs = SeekIntervalPolicy.toMilliseconds(seekForwardSec)

    fun backward() {
        controller.seekBy(-backwardMs)
    }

    fun forward() {
        controller.seekBy(forwardMs)
    }
}

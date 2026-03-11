package com.asuka.player.ui.controller

import com.asuka.player.contract.PlaybackController

class QueueActions(
    private val controller: PlaybackController,
) {
    fun next() {
        controller.skipToNext()
        controller.play()
    }

    fun previous() {
        controller.skipToPrevious()
        controller.play()
    }
}

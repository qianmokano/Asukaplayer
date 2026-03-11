package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.asuka.player.contract.VideoScaleMode

class ScaleState {
    var mode: VideoScaleMode by mutableStateOf(VideoScaleMode.FIT)
        private set

    fun updateMode(value: VideoScaleMode) {
        mode = value
    }
}

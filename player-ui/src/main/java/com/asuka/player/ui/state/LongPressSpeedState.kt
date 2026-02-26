package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class LongPressSpeedState {
    var active: Boolean by mutableStateOf(false)
        private set

    var speed: Float by mutableFloatStateOf(1.0f)
        private set

    fun start(value: Float) {
        speed = value
        active = true
    }

    fun end() {
        active = false
    }
}

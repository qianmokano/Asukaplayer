package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TapFeedbackState {
    enum class Direction {
        LEFT,
        RIGHT,
    }

    var visible: Boolean by mutableStateOf(false)
        private set

    var deltaMs: Long by mutableLongStateOf(0L)
        private set

    var eventId: Int by mutableIntStateOf(0)
        private set

    var direction: Direction by mutableStateOf(Direction.RIGHT)
        private set

    fun show(delta: Long) {
        deltaMs = delta
        direction = if (delta < 0L) Direction.LEFT else Direction.RIGHT
        visible = true
        eventId += 1
    }

    fun hide() {
        visible = false
    }
}

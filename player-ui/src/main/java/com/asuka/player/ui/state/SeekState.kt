package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SeekState {
    var seeking: Boolean by mutableStateOf(false)
        private set

    var deltaMs: Long by mutableLongStateOf(0L)
        private set

    var previewPositionMs: Long by mutableLongStateOf(0L)
        private set

    fun start(initialPositionMs: Long) {
        seeking = true
        previewPositionMs = initialPositionMs.coerceAtLeast(0L)
    }

    fun update(delta: Long, previewPositionMs: Long) {
        deltaMs = delta
        this.previewPositionMs = previewPositionMs.coerceAtLeast(0L)
    }

    fun end() {
        seeking = false
        deltaMs = 0L
        previewPositionMs = 0L
    }
}

package com.asuka.player.ui.controller

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SeekState {
    var seeking: Boolean by mutableStateOf(false)
        private set

    var deltaMs: Long by mutableLongStateOf(0L)
        private set

    fun start() {
        seeking = true
    }

    fun update(delta: Long) {
        deltaMs = delta
    }

    fun end() {
        seeking = false
        deltaMs = 0L
    }
}

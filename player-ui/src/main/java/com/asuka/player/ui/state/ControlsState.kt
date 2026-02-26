package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class ControlsState(
    private val scope: CoroutineScope,
    private val hideAfter: Duration,
) {
    private var hideJob: Job? = null

    var visible: Boolean by mutableStateOf(true)
        private set

    var locked: Boolean by mutableStateOf(false)
        private set

    fun show() {
        visible = true
        scheduleHide()
    }

    fun hide() {
        hideJob?.cancel()
        visible = false
    }

    fun toggle() {
        if (visible) hide() else show()
    }

    fun lock() {
        locked = true
        hide()
    }

    fun unlock() {
        locked = false
        show()
    }

    private fun scheduleHide() {
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(hideAfter)
            if (!locked) {
                visible = false
            }
        }
    }
}

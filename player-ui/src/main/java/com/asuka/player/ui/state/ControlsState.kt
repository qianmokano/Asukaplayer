package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * Manages the visibility and lock state of the playback controls overlay.
 *
 * Auto-hide is implemented with a **generation counter**: each call to [show]
 * increments [generation] and launches a delayed dismissal that checks whether
 * the generation has changed before hiding. This avoids coroutine-job bookkeeping
 * and naturally coalesces rapid show/hide calls.
 *
 * @param scope          Coroutine scope for auto-hide timers (typically the
 *                       enclosing Composable's [rememberCoroutineScope]).
 * @param autoHideDelay  How long after the last [show] call the controls will
 *                       automatically dismiss themselves.
 */
class ControlsState(
    private val scope: CoroutineScope,
    private val autoHideDelay: Duration,
) {
    private var generation = 0
    private var interactionHoldCount = 0

    var visible: Boolean by mutableStateOf(true)
        private set

    var locked: Boolean by mutableStateOf(false)
        private set

    fun show() {
        visible = true
        if (interactionHoldCount == 0) {
            armAutoDismiss()
        } else {
            generation++
        }
    }

    fun hide() {
        generation++          // invalidate any in-flight auto-dismiss
        visible = false
    }

    fun toggle() {
        if (visible) hide() else show()
    }

    fun lock() {
        interactionHoldCount = 0
        locked = true
        hide()
    }

    fun unlock() {
        locked = false
        show()
    }

    fun beginInteractionVisibilityHold() {
        interactionHoldCount++
        generation++
        visible = true
    }

    fun setInteractionVisibilityHold(active: Boolean) {
        if (active) beginInteractionVisibilityHold() else endInteractionVisibilityHold()
    }

    fun endInteractionVisibilityHold() {
        if (interactionHoldCount == 0) return
        interactionHoldCount--
        if (interactionHoldCount == 0 && visible && !locked) {
            armAutoDismiss()
        }
    }

    private fun armAutoDismiss() {
        val snapshot = ++generation
        // If `scope` is already cancelled (e.g. the enclosing Composable has left composition),
        // `scope.launch` returns an already-cancelled Job without throwing. The auto-hide
        // simply does not fire, which is correct — a disposed composable has no visible state
        // to hide. This is intentional behaviour, not a silent failure.
        scope.launch {
            delay(autoHideDelay)
            if (generation == snapshot && !locked && interactionHoldCount == 0) {
                visible = false
            }
        }
    }
}

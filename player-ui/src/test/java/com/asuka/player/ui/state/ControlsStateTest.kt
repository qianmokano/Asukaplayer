package com.asuka.player.ui.state

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ControlsStateTest {

    @Test
    fun interactionHold_keepsControlsVisibleUntilReleased() {
        val state = testControlsState()

        state.show()
        state.beginInteractionVisibilityHold()
        Thread.sleep(80L)

        assertTrue(state.visible)

        state.endInteractionVisibilityHold()
        Thread.sleep(80L)

        assertFalse(state.visible)
    }

    @Test
    fun interactionHold_makesHiddenControlsVisibleImmediately() {
        val state = testControlsState()

        state.hide()
        state.beginInteractionVisibilityHold()

        assertTrue(state.visible)
    }

    private fun testControlsState(): ControlsState {
        return ControlsState(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            autoHideDelay = 40.milliseconds,
        )
    }
}

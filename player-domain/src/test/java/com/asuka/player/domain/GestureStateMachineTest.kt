package com.asuka.player.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GestureStateMachineTest {

    @Test
    fun disableBlocksTransitions() {
        val machine = GestureStateMachine()
        machine.onEvent(GestureStateMachine.Event.Disable)
        assertEquals(GestureStateMachine.State.DISABLED, machine.state)
        machine.onEvent(GestureStateMachine.Event.Tap)
        assertEquals(GestureStateMachine.State.DISABLED, machine.state)
    }

    @Test
    fun horizontalDragTransitions() {
        val machine = GestureStateMachine()
        machine.onEvent(GestureStateMachine.Event.HorizontalStart)
        assertEquals(GestureStateMachine.State.HORIZONTAL_SEEK, machine.state)
        machine.onEvent(GestureStateMachine.Event.HorizontalEnd)
        assertEquals(GestureStateMachine.State.IDLE, machine.state)
    }
}

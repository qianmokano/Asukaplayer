package com.asuka.player.domain

import com.asuka.player.domain.GestureStateMachine.Event
import com.asuka.player.domain.GestureStateMachine.State
import kotlin.test.Test
import kotlin.test.assertEquals

class GestureStateMachineTest {

    // ── Disabled state ──────────────────────────────────────────────────────

    @Test
    fun disableBlocksTransitions() {
        val m = GestureStateMachine()
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
        m.onEvent(Event.Tap)
        assertEquals(State.DISABLED, m.state)
    }

    @Test
    fun disabledIgnoresAllGestureEvents() {
        val m = GestureStateMachine()
        m.onEvent(Event.Disable)
        listOf(
            Event.Tap, Event.DoubleTap, Event.LongPressStart,
            Event.HorizontalStart, Event.VerticalStart, Event.TransformStart,
            Event.Cancel,
        ).forEach { event ->
            m.onEvent(event)
            assertEquals(State.DISABLED, m.state, "DISABLED should ignore $event")
        }
    }

    @Test
    fun enableFromDisabledGoesIdle() {
        val m = GestureStateMachine()
        m.onEvent(Event.Disable)
        m.onEvent(Event.Enable)
        assertEquals(State.IDLE, m.state)
    }

    // ── IDLE transitions ────────────────────────────────────────────────────

    @Test
    fun idleToTap() {
        val m = GestureStateMachine()
        m.onEvent(Event.Tap)
        assertEquals(State.TAP, m.state)
    }

    @Test
    fun idleToDoubleTap() {
        val m = GestureStateMachine()
        m.onEvent(Event.DoubleTap)
        assertEquals(State.DOUBLE_TAP, m.state)
    }

    @Test
    fun idleToLongPress() {
        val m = GestureStateMachine()
        m.onEvent(Event.LongPressStart)
        assertEquals(State.LONG_PRESS, m.state)
    }

    @Test
    fun idleToHorizontalSeek() {
        val m = GestureStateMachine()
        m.onEvent(Event.HorizontalStart)
        assertEquals(State.HORIZONTAL_SEEK, m.state)
    }

    @Test
    fun idleToVerticalAdjust() {
        val m = GestureStateMachine()
        m.onEvent(Event.VerticalStart)
        assertEquals(State.VERTICAL_ADJUST, m.state)
    }

    @Test
    fun idleToTransformZoom() {
        val m = GestureStateMachine()
        m.onEvent(Event.TransformStart)
        assertEquals(State.TRANSFORM_ZOOM, m.state)
    }

    @Test
    fun idleToDisabled() {
        val m = GestureStateMachine()
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
    }

    @Test
    fun idleIgnoresEndEvents() {
        val m = GestureStateMachine()
        listOf(Event.HorizontalEnd, Event.VerticalEnd, Event.TransformEnd, Event.LongPressEnd, Event.Cancel).forEach {
            m.onEvent(it)
            assertEquals(State.IDLE, m.state, "IDLE should ignore $it")
        }
    }

    // ── Horizontal seek lifecycle ───────────────────────────────────────────

    @Test
    fun horizontalDragTransitions() {
        val m = GestureStateMachine()
        m.onEvent(Event.HorizontalStart)
        assertEquals(State.HORIZONTAL_SEEK, m.state)
        m.onEvent(Event.HorizontalEnd)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun horizontalSeekIgnoresOtherStarts() {
        val m = GestureStateMachine()
        m.onEvent(Event.HorizontalStart)
        listOf(Event.VerticalStart, Event.TransformStart, Event.Tap, Event.DoubleTap).forEach {
            m.onEvent(it)
            assertEquals(State.HORIZONTAL_SEEK, m.state, "HORIZONTAL_SEEK should ignore $it")
        }
    }

    @Test
    fun horizontalSeekCancelGoesIdle() {
        val m = GestureStateMachine()
        m.onEvent(Event.HorizontalStart)
        m.onEvent(Event.Cancel)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun horizontalSeekDisableGoesDisabled() {
        val m = GestureStateMachine()
        m.onEvent(Event.HorizontalStart)
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
    }

    // ── Vertical adjust lifecycle ───────────────────────────────────────────

    @Test
    fun verticalAdjustLifecycle() {
        val m = GestureStateMachine()
        m.onEvent(Event.VerticalStart)
        assertEquals(State.VERTICAL_ADJUST, m.state)
        m.onEvent(Event.VerticalEnd)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun verticalAdjustCancelGoesIdle() {
        val m = GestureStateMachine()
        m.onEvent(Event.VerticalStart)
        m.onEvent(Event.Cancel)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun verticalAdjustDisableGoesDisabled() {
        val m = GestureStateMachine()
        m.onEvent(Event.VerticalStart)
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
    }

    // ── Transform zoom lifecycle ────────────────────────────────────────────

    @Test
    fun transformZoomLifecycle() {
        val m = GestureStateMachine()
        m.onEvent(Event.TransformStart)
        assertEquals(State.TRANSFORM_ZOOM, m.state)
        m.onEvent(Event.TransformEnd)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun transformZoomCancelGoesIdle() {
        val m = GestureStateMachine()
        m.onEvent(Event.TransformStart)
        m.onEvent(Event.Cancel)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun transformZoomDisableGoesDisabled() {
        val m = GestureStateMachine()
        m.onEvent(Event.TransformStart)
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
    }

    // ── Long press lifecycle ────────────────────────────────────────────────

    @Test
    fun longPressLifecycle() {
        val m = GestureStateMachine()
        m.onEvent(Event.LongPressStart)
        assertEquals(State.LONG_PRESS, m.state)
        m.onEvent(Event.LongPressEnd)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun longPressCancelGoesIdle() {
        val m = GestureStateMachine()
        m.onEvent(Event.LongPressStart)
        m.onEvent(Event.Cancel)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun longPressDisableGoesDisabled() {
        val m = GestureStateMachine()
        m.onEvent(Event.LongPressStart)
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
    }

    // ── TAP / DOUBLE_TAP transient states ───────────────────────────────────

    @Test
    fun tapTransitionsToGestureStates() {
        val m = GestureStateMachine()
        m.onEvent(Event.Tap)
        assertEquals(State.TAP, m.state)

        // From TAP, a horizontal start should transition
        m.onEvent(Event.HorizontalStart)
        assertEquals(State.HORIZONTAL_SEEK, m.state)
    }

    @Test
    fun doubleTapTransitionsToGestureStates() {
        val m = GestureStateMachine()
        m.onEvent(Event.DoubleTap)
        assertEquals(State.DOUBLE_TAP, m.state)

        m.onEvent(Event.VerticalStart)
        assertEquals(State.VERTICAL_ADJUST, m.state)
    }

    @Test
    fun tapFallsBackToIdleOnUnknownEvent() {
        val m = GestureStateMachine()
        m.onEvent(Event.Tap)
        m.onEvent(Event.Cancel)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun tapToDisabled() {
        val m = GestureStateMachine()
        m.onEvent(Event.Tap)
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
    }

    // ── Full lifecycle sequences ────────────────────────────────────────────

    @Test
    fun fullDisableEnableSeekCycle() {
        val m = GestureStateMachine()
        m.onEvent(Event.Disable)
        assertEquals(State.DISABLED, m.state)
        m.onEvent(Event.Enable)
        assertEquals(State.IDLE, m.state)
        m.onEvent(Event.HorizontalStart)
        assertEquals(State.HORIZONTAL_SEEK, m.state)
        m.onEvent(Event.HorizontalEnd)
        assertEquals(State.IDLE, m.state)
    }

    @Test
    fun multipleGesturesInSequence() {
        val m = GestureStateMachine()

        m.onEvent(Event.Tap)
        assertEquals(State.TAP, m.state)
        m.onEvent(Event.HorizontalStart)
        assertEquals(State.HORIZONTAL_SEEK, m.state)
        m.onEvent(Event.HorizontalEnd)
        assertEquals(State.IDLE, m.state)

        m.onEvent(Event.VerticalStart)
        assertEquals(State.VERTICAL_ADJUST, m.state)
        m.onEvent(Event.VerticalEnd)
        assertEquals(State.IDLE, m.state)

        m.onEvent(Event.TransformStart)
        assertEquals(State.TRANSFORM_ZOOM, m.state)
        m.onEvent(Event.TransformEnd)
        assertEquals(State.IDLE, m.state)
    }
}

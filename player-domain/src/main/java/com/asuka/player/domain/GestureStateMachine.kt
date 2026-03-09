package com.asuka.player.domain

/**
 * Gesture state machine (UI-agnostic). The UI layer should translate pointer events into these events.
 *
 * ## State transitions
 * ```
 *                       Disable
 *          ┌──────────────────────────────────────────────────────┐
 *          │                                                      ▼
 *   ┌──────────────┐  Enable   ┌──────────┐
 *   │   DISABLED   │ ────────► │   IDLE   │◄─────────────────────────────────────┐
 *   └──────────────┘           └──────────┘                                      │
 *          ▲                        │  Tap / DoubleTap / LongPressStart /        │
 *          │              ┌─────────┤  HorizontalStart / VerticalStart /         │
 *          │              │         │  TransformStart                            │
 *          │              ▼         ▼                                            │
 *          │   ┌─────┐  ┌──────────────┐   HorizontalStart/VerticalStart/  ┌─────────────────┐
 *          │   │ TAP │  │ DOUBLE_TAP   │   TransformStart/LongPressStart   │ HORIZONTAL_SEEK │
 *          │   └─────┘  └──────────────┘ ──────────────────────────────►  │ VERTICAL_ADJUST │
 *          │     (any other event → IDLE)                                  │ TRANSFORM_ZOOM  │
 *          │                                                               │ LONG_PRESS      │
 *          │                                                               └─────────────────┘
 *          │                                                                       │
 *          └───────────────────── Disable ────────────────────────────────────────┘
 *                                                    MatchingEnd / Cancel → IDLE
 * ```
 *
 * Each active state (HORIZONTAL_SEEK, VERTICAL_ADJUST, TRANSFORM_ZOOM, LONG_PRESS) ignores
 * unrelated events (the `else` branch) and stays in the current state, enforcing gesture
 * exclusivity — only one gesture type can be active at a time.
 *
 * TAP and DOUBLE_TAP are transient: any event that does not start a new gesture returns to IDLE.
 *
 * **Threading:** This class is NOT thread-safe. All calls to [onEvent] must be made from the same
 * thread (typically the main/UI thread). No internal synchronization is performed.
 */
class GestureStateMachine {
    enum class State {
        IDLE,
        TAP,
        DOUBLE_TAP,
        LONG_PRESS,
        HORIZONTAL_SEEK,
        VERTICAL_ADJUST,
        TRANSFORM_ZOOM,
        DISABLED,
    }

    sealed interface Event {
        data object Disable : Event
        data object Enable : Event
        data object Tap : Event
        data object DoubleTap : Event
        data object LongPressStart : Event
        data object LongPressEnd : Event
        data object HorizontalStart : Event
        data object HorizontalEnd : Event
        data object VerticalStart : Event
        data object VerticalEnd : Event
        data object TransformStart : Event
        data object TransformEnd : Event
        data object Cancel : Event
    }

    var state: State = State.IDLE
        private set

    fun onEvent(event: Event): State {
        state = when (state) {
            State.DISABLED -> when (event) {
                Event.Enable -> State.IDLE
                else -> State.DISABLED
            }
            State.IDLE -> when (event) {
                Event.Disable -> State.DISABLED
                Event.Tap -> State.TAP
                Event.DoubleTap -> State.DOUBLE_TAP
                Event.LongPressStart -> State.LONG_PRESS
                Event.HorizontalStart -> State.HORIZONTAL_SEEK
                Event.VerticalStart -> State.VERTICAL_ADJUST
                Event.TransformStart -> State.TRANSFORM_ZOOM
                else -> State.IDLE
            }
            State.TAP, State.DOUBLE_TAP -> when (event) {
                Event.Disable -> State.DISABLED
                Event.HorizontalStart -> State.HORIZONTAL_SEEK
                Event.VerticalStart -> State.VERTICAL_ADJUST
                Event.TransformStart -> State.TRANSFORM_ZOOM
                Event.LongPressStart -> State.LONG_PRESS
                else -> State.IDLE
            }
            State.LONG_PRESS -> when (event) {
                Event.LongPressEnd, Event.Cancel -> State.IDLE
                Event.Disable -> State.DISABLED
                else -> State.LONG_PRESS
            }
            State.HORIZONTAL_SEEK -> when (event) {
                Event.HorizontalEnd, Event.Cancel -> State.IDLE
                Event.Disable -> State.DISABLED
                else -> State.HORIZONTAL_SEEK
            }
            State.VERTICAL_ADJUST -> when (event) {
                Event.VerticalEnd, Event.Cancel -> State.IDLE
                Event.Disable -> State.DISABLED
                else -> State.VERTICAL_ADJUST
            }
            State.TRANSFORM_ZOOM -> when (event) {
                Event.TransformEnd, Event.Cancel -> State.IDLE
                Event.Disable -> State.DISABLED
                else -> State.TRANSFORM_ZOOM
            }
        }
        return state
    }
}

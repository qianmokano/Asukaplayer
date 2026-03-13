package com.asuka.player.ui.components

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.PointerGestureDetector
import kotlin.math.max

private const val landscapeEdgeGuardMinDp = 32

/**
 * Minimal gesture layer wiring for M1 (tap + double-tap).
 * Drag/transform will be extended next.
 */
@Composable
fun GestureLayer(
    modifier: Modifier = Modifier,
    coordinator: GestureCoordinator,
    pointerDetector: PointerGestureDetector,
) {
    val configuration = LocalConfiguration.current
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val systemGesturePadding = WindowInsets.systemGestures.asPaddingValues()
    val dragStartGuard = if (configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
        PointerGestureDetector.DragStartGuard.None
    } else {
        val minEdgePx = with(density) { landscapeEdgeGuardMinDp.dp.toPx() }
        PointerGestureDetector.DragStartGuard(
            leftEdgePx = max(
                with(density) { systemGesturePadding.calculateLeftPadding(layoutDirection).toPx() },
                minEdgePx,
            ),
            rightEdgePx = max(
                with(density) { systemGesturePadding.calculateRightPadding(layoutDirection).toPx() },
                minEdgePx,
            ),
            topEdgePx = max(
                with(density) { systemGesturePadding.calculateTopPadding().toPx() },
                minEdgePx,
            ),
            bottomEdgePx = max(
                with(density) { systemGesturePadding.calculateBottomPadding().toPx() },
                minEdgePx,
            ),
        )
    }
    val suppressTapUntilMs = remember { mutableLongStateOf(0L) }
    fun suppressTapWindow() {
        suppressTapUntilMs.longValue = SystemClock.uptimeMillis() + 220L
    }
    fun isTapSuppressed(): Boolean = SystemClock.uptimeMillis() < suppressTapUntilMs.longValue

    DisposableEffect(coordinator) {
        onDispose { coordinator.cancelActiveGesture() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(coordinator, dragStartGuard) {
                var longPressStarted = false
                detectTapGestures(
                    onTap = { offset ->
                        if (!isTapSuppressed() &&
                            !dragStartGuard.blocksDragStart(offset, size.width, size.height)
                        ) {
                            coordinator.onTap()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (!isTapSuppressed() &&
                            !dragStartGuard.blocksDragStart(offset, size.width, size.height)
                        ) {
                            coordinator.onDoubleTap(offset, size)
                        }
                    },
                    onLongPress = { offset ->
                        if (!isTapSuppressed() &&
                            !dragStartGuard.blocksDragStart(offset, size.width, size.height)
                        ) {
                            longPressStarted = true
                            coordinator.onLongPressStart()
                        }
                    },
                    onPress = { offset ->
                        longPressStarted = false
                        val blocked = dragStartGuard.blocksDragStart(offset, size.width, size.height)
                        tryAwaitRelease()
                        if (!blocked && longPressStarted) coordinator.onLongPressEnd()
                    },
                )
            }
            .pointerInput(coordinator, pointerDetector) {
                pointerDetector.run {
                    detectDirectionalDrag(
                        onHorizontalStart = { x -> coordinator.onHorizontalDragStart(currentPositionMs(), x) },
                        onHorizontalDrag = { x -> coordinator.onHorizontalDrag(x, durationMs(), seekSensitivity) },
                        onHorizontalEnd = coordinator::onHorizontalDragEnd,
                        onVerticalStart = { offset, size ->
                            coordinator.onVerticalDragStart(offset, size, volumePercent(), brightnessPercent())
                        },
                        onVerticalDrag = { start, current, mode ->
                            coordinator.onVerticalDrag(
                                startValue = start.value,
                                startY = start.startY,
                                currentY = current,
                                sensitivity = verticalSensitivity,
                                maxValue = start.maxValue,
                                mode = mode,
                            )
                        },
                        onVerticalEnd = coordinator::onVerticalDragEnd,
                        dragStartGuard = dragStartGuard,
                        onGestureCaptured = { suppressTapWindow() },
                    )
                }
            }
            .pointerInput(coordinator, dragStartGuard) {
                awaitEachGesture {
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    if (dragStartGuard.blocksDragStart(firstDown.position, size.width, size.height)) {
                        do {
                            val event = awaitPointerEvent()
                            if (!event.changes.any { it.pressed }) {
                                break
                            }
                        } while (true)
                        return@awaitEachGesture
                    }
                    var transformStarted = false
                    do {
                        val event = awaitPointerEvent()
                        val activePointers = event.changes.count { it.pressed }
                        if (activePointers < 2) {
                            continue
                        }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                            transformStarted = true
                            suppressTapWindow()
                            coordinator.onTransformGesture(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                panChange,
                                zoomChange,
                            )
                            event.changes.forEach {
                                if (it.positionChanged()) it.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (transformStarted) {
                        coordinator.onTransformEnd()
                    }
                }
            },
    ) {}
}

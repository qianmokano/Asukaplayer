package com.asuka.player.ui.components

import android.os.SystemClock
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.PointerGestureDetector

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
    val suppressTapUntilMs = remember { mutableLongStateOf(0L) }
    fun suppressTapWindow() {
        suppressTapUntilMs.longValue = SystemClock.uptimeMillis() + 220L
    }
    fun isTapSuppressed(): Boolean = SystemClock.uptimeMillis() < suppressTapUntilMs.longValue

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var longPressStarted = false
                detectTapGestures(
                    onTap = { if (!isTapSuppressed()) coordinator.onTap() },
                    onDoubleTap = { offset ->
                        if (!isTapSuppressed()) coordinator.onDoubleTap(offset, size)
                    },
                    onLongPress = {
                        if (!isTapSuppressed()) {
                            longPressStarted = true
                            coordinator.onLongPressStart()
                        }
                    },
                    onPress = {
                        longPressStarted = false
                        tryAwaitRelease()
                        if (longPressStarted) coordinator.onLongPressEnd()
                    },
                )
            }
            .pointerInput(Unit) {
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
                        onGestureCaptured = { suppressTapWindow() },
                    )
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
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

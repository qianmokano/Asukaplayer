package com.asuka.player.ui.controller

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.ViewConfiguration
import com.asuka.player.ui.state.VolumeBrightnessState

/**
 * Thin pointer detector to keep Compose gesture code isolated.
 */
class PointerGestureDetector(
    val seekSensitivity: Float = 0.5f,
    val verticalSensitivity: Float = 0.5f,
    private val positionProvider: () -> Long,
    private val durationProvider: () -> Long,
    private val volumeProvider: () -> Int,
    private val brightnessProvider: () -> Int,
) {
    data class DragStartGuard(
        val leftEdgePx: Float = 0f,
        val rightEdgePx: Float = 0f,
        val topEdgePx: Float = 0f,
        val bottomEdgePx: Float = 0f,
    ) {
        fun blocksDragStart(startPosition: Offset, width: Int, height: Int): Boolean {
            if (width <= 0 || height <= 0) return false
            return startPosition.x <= leftEdgePx ||
                startPosition.x >= (width - rightEdgePx) ||
                startPosition.y <= topEdgePx ||
                startPosition.y >= (height - bottomEdgePx)
        }

        companion object {
            val None = DragStartGuard()
        }
    }

    data class VerticalStart(
        val startY: Float,
        val value: Int,
        val maxValue: Int,
    )

    fun currentPositionMs(): Long = positionProvider()
    fun durationMs(): Long = durationProvider()
    fun volumePercent(): Int = volumeProvider()
    fun brightnessPercent(): Int = brightnessProvider()

    suspend fun PointerInputScope.detectDirectionalDrag(
        onHorizontalStart: (x: Float) -> Unit,
        onHorizontalDrag: (x: Float) -> Unit,
        onHorizontalEnd: () -> Unit,
        onVerticalStart: (offset: Offset, size: androidx.compose.ui.unit.IntSize) -> Unit,
        onVerticalDrag: (start: VerticalStart, currentY: Float, mode: VolumeBrightnessState.Mode) -> Unit,
        onVerticalEnd: () -> Unit,
        dragStartGuard: DragStartGuard = DragStartGuard.None,
        onGestureCaptured: () -> Unit = {},
    ) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val size = this.size
            val touchSlop = viewConfiguration.touchSlop
            var pointerId: PointerId = down.id
            val startPosition = down.position
            var dragKind: DragKind? = null
            var verticalStart: VerticalStart? = null
            var verticalMode: VolumeBrightnessState.Mode? = null
            var horizontalStarted = false

            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == pointerId }
                if (change == null || !change.pressed) break

                val dx = change.position.x - startPosition.x
                val dy = change.position.y - startPosition.y

                if (dragKind == null) {
                    val moved = kotlin.math.abs(dx) > touchSlop || kotlin.math.abs(dy) > touchSlop
                    if (!moved) continue
                    if (dragStartGuard.blocksDragStart(startPosition, size.width, size.height)) {
                        dragKind = DragKind.BLOCKED
                        continue
                    }
                    dragKind = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                        DragKind.HORIZONTAL
                    } else {
                        DragKind.VERTICAL
                    }
                    when (dragKind) {
                        DragKind.HORIZONTAL -> {
                            onGestureCaptured()
                            horizontalStarted = true
                            onHorizontalStart(startPosition.x)
                            onHorizontalDrag(change.position.x)
                        }
                        DragKind.VERTICAL -> {
                            onGestureCaptured()
                            val mode = if (startPosition.x < size.width / 2f) {
                                VolumeBrightnessState.Mode.BRIGHTNESS
                            } else {
                                VolumeBrightnessState.Mode.VOLUME
                            }
                            val startValue = if (mode == VolumeBrightnessState.Mode.VOLUME) {
                                volumeProvider()
                            } else {
                                brightnessProvider()
                            }
                            val vStart = VerticalStart(
                                startY = startPosition.y,
                                value = startValue,
                                maxValue = 100,
                            )
                            verticalMode = mode
                            verticalStart = vStart
                            onVerticalStart(startPosition, size)
                            onVerticalDrag(vStart, change.position.y, mode)
                        }
                        DragKind.BLOCKED -> Unit
                    }
                } else {
                    when (dragKind) {
                        DragKind.HORIZONTAL -> onHorizontalDrag(change.position.x)
                        DragKind.VERTICAL -> {
                            val start = verticalStart ?: continue
                            val mode = verticalMode ?: continue
                            onVerticalDrag(start, change.position.y, mode)
                        }
                        DragKind.BLOCKED -> Unit
                    }
                }

                if (dragKind != DragKind.BLOCKED && change.positionChanged()) {
                    change.consume()
                }
            }
            when (dragKind) {
                DragKind.HORIZONTAL -> {
                    if (horizontalStarted) onHorizontalEnd()
                }
                DragKind.VERTICAL -> onVerticalEnd()
                DragKind.BLOCKED -> Unit
                null -> Unit
            }
        }
    }

    private enum class DragKind {
        HORIZONTAL,
        VERTICAL,
        BLOCKED,
    }
}

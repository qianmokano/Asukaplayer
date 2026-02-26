package com.asuka.player.ui.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.asuka.player.core.PlaybackController
import com.asuka.player.domain.GestureAlgorithms
import com.asuka.player.domain.GestureStateMachine
import com.asuka.player.ui.PlayerRuntimeSettings
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import kotlin.math.abs

/**
 * UI-side gesture coordinator; translates pointer input into playback actions.
 */
class GestureCoordinator(
    private val controller: PlaybackController,
    private val controlsState: ControlsState,
    private val volumeBrightnessState: VolumeBrightnessState,
    private val seekState: SeekState,
    private val zoomState: ZoomState,
    private val onZoomEnd: (Float) -> Unit = {},
    private val playbackSpeedProvider: () -> Float = { 1.0f },
    private val onDoubleTapFeedback: (Long) -> Unit = {},
    private val onLongPressFeedback: (Boolean, Float) -> Unit = { _, _ -> },
    private val onVolumeChanged: (Int) -> Unit = {},
    private val onBrightnessChanged: (Int) -> Unit = {},
    private val minSeekDeltaMs: Long = 250L,
    val enableSeekGesture: Boolean = true,
    val enableBrightnessGesture: Boolean = true,
    val enableVolumeGesture: Boolean = true,
    val enableZoomGesture: Boolean = true,
    val enablePanGesture: Boolean = true,
    val enableDoubleTapGesture: Boolean = true,
    val doubleTapAction: PlayerRuntimeSettings.DoubleTapAction = PlayerRuntimeSettings.DoubleTapAction.Seek,
    val enableLongPressGesture: Boolean = true,
    val doubleTapSeekDeltaMs: Long = 10_000L,
    val longPressSpeed: Float = 2.0f,
) {
    private val machine = GestureStateMachine()

    private var seekStartPositionMs: Long = 0L
    private var seekStartX: Float = 0f
    private var lastSeekDeltaMs: Long = 0L
    private var pendingSeekPositionMs: Long? = null

    private var savedSpeed: Float = 1.0f
    private var longPressActive: Boolean = false
    private var transformGestureStarted: Boolean = false

    fun onTap() {
        if (machine.state == GestureStateMachine.State.DISABLED || controlsState.locked) return
        machine.onEvent(GestureStateMachine.Event.Tap)
        controlsState.toggle()
    }

    fun onDoubleTap(offset: Offset, size: IntSize) {
        if (machine.state == GestureStateMachine.State.DISABLED || controlsState.locked || !enableDoubleTapGesture) return
        machine.onEvent(GestureStateMachine.Event.DoubleTap)
        if (doubleTapAction == PlayerRuntimeSettings.DoubleTapAction.TogglePlayPause) {
            controller.togglePlayPause()
            return
        }
        val center = size.width / 2f
        val delta = doubleTapSeekDeltaMs
        if (doubleTapAction == PlayerRuntimeSettings.DoubleTapAction.Both) {
            val centerBandHalfWidth = size.width / 6f
            if (abs(offset.x - center) <= centerBandHalfWidth) {
                controller.togglePlayPause()
                return
            }
        }
        if (offset.x < center) {
            controller.seekBy(-delta)
            onDoubleTapFeedback(-delta)
        } else {
            controller.seekBy(delta)
            onDoubleTapFeedback(delta)
        }
    }

    fun onLongPressStart() {
        if (machine.state == GestureStateMachine.State.DISABLED || controlsState.locked || !enableLongPressGesture) return
        machine.onEvent(GestureStateMachine.Event.LongPressStart)
        savedSpeed = playbackSpeedProvider()
        longPressActive = true
        controlsState.hide()
        controller.setPlaybackSpeed(longPressSpeed)
        onLongPressFeedback(true, longPressSpeed)
    }

    fun onLongPressEnd() {
        if (!longPressActive) return
        if (machine.state == GestureStateMachine.State.DISABLED) return
        machine.onEvent(GestureStateMachine.Event.LongPressEnd)
        longPressActive = false
        controller.setPlaybackSpeed(savedSpeed)
        onLongPressFeedback(false, savedSpeed)
    }

    fun onHorizontalDragStart(positionMs: Long, x: Float) {
        if (machine.state == GestureStateMachine.State.DISABLED || controlsState.locked || !enableSeekGesture) return
        machine.onEvent(GestureStateMachine.Event.HorizontalStart)
        seekStartPositionMs = positionMs
        seekStartX = x
        lastSeekDeltaMs = 0L
        pendingSeekPositionMs = null
        seekState.start()
    }

    fun onHorizontalDrag(currentX: Float, durationMs: Long, sensitivity: Float) {
        if (machine.state != GestureStateMachine.State.HORIZONTAL_SEEK) return
        if (durationMs <= 0L) {
            lastSeekDeltaMs = 0L
            pendingSeekPositionMs = null
            seekState.update(0L)
            return
        }
        val result = GestureAlgorithms.calculateSeek(
            GestureAlgorithms.SeekInput(
                startPositionMs = seekStartPositionMs,
                startX = seekStartX,
                currentX = currentX,
                sensitivity = sensitivity,
                durationMs = durationMs,
            ),
        )
        lastSeekDeltaMs = result.deltaMs
        pendingSeekPositionMs = result.newPositionMs
        seekState.update(result.deltaMs)
        if (abs(result.deltaMs) >= minSeekDeltaMs) {
            controller.seekTo(result.newPositionMs)
        }
    }

    fun onHorizontalDragEnd() {
        if (machine.state != GestureStateMachine.State.HORIZONTAL_SEEK) return
        val target = pendingSeekPositionMs
        if (target != null && abs(lastSeekDeltaMs) >= minSeekDeltaMs) {
            controller.seekTo(target)
        }
        pendingSeekPositionMs = null
        machine.onEvent(GestureStateMachine.Event.HorizontalEnd)
        seekState.end()
    }

    fun onVerticalDragStart(offset: Offset, size: IntSize, currentVolume: Int, currentBrightness: Int) {
        if (machine.state == GestureStateMachine.State.DISABLED || controlsState.locked) return
        val mode = if (offset.x < size.width / 2f) {
            VolumeBrightnessState.Mode.BRIGHTNESS
        } else {
            VolumeBrightnessState.Mode.VOLUME
        }
        val enabled = when (mode) {
            VolumeBrightnessState.Mode.VOLUME -> enableVolumeGesture
            VolumeBrightnessState.Mode.BRIGHTNESS -> enableBrightnessGesture
        }
        if (!enabled) return
        machine.onEvent(GestureStateMachine.Event.VerticalStart)
        volumeBrightnessState.start(mode, currentVolume, currentBrightness)
    }

    fun onVerticalDrag(
        startValue: Int,
        startY: Float,
        currentY: Float,
        sensitivity: Float,
        maxValue: Int,
        mode: VolumeBrightnessState.Mode,
    ): Int {
        if (machine.state != GestureStateMachine.State.VERTICAL_ADJUST) return startValue
        val result = GestureAlgorithms.calculateVerticalAdjust(
            GestureAlgorithms.VerticalAdjustInput(
                startValue = startValue,
                startY = startY,
                currentY = currentY,
                sensitivity = sensitivity,
                maxValue = maxValue,
            ),
        )
        when (mode) {
            VolumeBrightnessState.Mode.VOLUME -> {
                volumeBrightnessState.updateVolume(result.newValue)
                onVolumeChanged(result.newValue)
            }
            VolumeBrightnessState.Mode.BRIGHTNESS -> {
                volumeBrightnessState.updateBrightness(result.newValue)
                onBrightnessChanged(result.newValue)
            }
        }
        return result.newValue
    }

    fun onVerticalDragEnd() {
        if (machine.state != GestureStateMachine.State.VERTICAL_ADJUST) return
        machine.onEvent(GestureStateMachine.Event.VerticalEnd)
        volumeBrightnessState.end()
    }

    fun onTransformGesture(viewWidth: Float, viewHeight: Float, panChange: Offset, zoomChange: Float) {
        if (machine.state == GestureStateMachine.State.DISABLED || controlsState.locked || !enableZoomGesture) return
        if (!transformGestureStarted) {
            machine.onEvent(GestureStateMachine.Event.TransformStart)
            if (machine.state != GestureStateMachine.State.TRANSFORM_ZOOM) return
            transformGestureStarted = true
        }
        val effectivePanChange = if (enablePanGesture) panChange else Offset.Zero
        val newZoom = GestureAlgorithms.clampZoom(
            GestureAlgorithms.ZoomInput(
                currentZoom = zoomState.zoom,
                zoomChange = zoomChange,
            ),
        )
        val pan = GestureAlgorithms.clampPan(
            GestureAlgorithms.PanClampInput(
                zoom = newZoom,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                currentX = zoomState.offset.x,
                currentY = zoomState.offset.y,
                panX = effectivePanChange.x,
                panY = effectivePanChange.y,
            ),
        )
        zoomState.update(newZoom, Offset(pan.clampedX, pan.clampedY), isZooming = true)
    }

    fun onTransformEnd() {
        transformGestureStarted = false
        if (machine.state != GestureStateMachine.State.TRANSFORM_ZOOM) return
        machine.onEvent(GestureStateMachine.Event.TransformEnd)
        zoomState.update(zoomState.zoom, zoomState.offset, isZooming = false)
        onZoomEnd(zoomState.zoom)
    }

    fun disableAllGestures() {
        machine.onEvent(GestureStateMachine.Event.Disable)
    }

    fun enableAllGestures() {
        machine.onEvent(GestureStateMachine.Event.Enable)
    }
}

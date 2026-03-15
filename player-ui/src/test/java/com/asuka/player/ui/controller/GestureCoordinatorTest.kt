package com.asuka.player.ui.controller

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.contract.VideoScaleMode
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GestureCoordinatorTest {

    // -- helpers --

    private fun testScope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private fun coordinator(
        controller: RecordingPlaybackController = RecordingPlaybackController(),
        controlsState: ControlsState = ControlsState(scope = testScope(), autoHideDelay = 30.seconds),
        volumeBrightnessState: VolumeBrightnessState = VolumeBrightnessState(),
        seekState: SeekState = SeekState(),
        zoomState: ZoomState = ZoomState(),
        config: GestureConfig = GestureConfig(),
        onDoubleTapFeedback: (Long) -> Unit = {},
        onLongPressFeedback: (Boolean, Float) -> Unit = { _, _ -> },
        onVolumeChanged: (Int) -> Unit = {},
        onBrightnessChanged: (Int) -> Unit = {},
        playbackSpeedProvider: () -> Float = { 1.0f },
        onZoomEnd: (Float) -> Unit = {},
    ) = GestureCoordinator(
        controller = controller,
        controlsState = controlsState,
        volumeBrightnessState = volumeBrightnessState,
        seekState = seekState,
        zoomState = zoomState,
        config = config,
        onDoubleTapFeedback = onDoubleTapFeedback,
        onLongPressFeedback = onLongPressFeedback,
        onVolumeChanged = onVolumeChanged,
        onBrightnessChanged = onBrightnessChanged,
        playbackSpeedProvider = playbackSpeedProvider,
        onZoomEnd = onZoomEnd,
    )

    // -- tap --

    @Test
    fun tap_togglesControlsVisibility() {
        val controls = ControlsState(scope = testScope(), autoHideDelay = 30.seconds)
        val c = coordinator(controlsState = controls)
        assertTrue(controls.visible)
        c.onTap()
        assertFalse(controls.visible)
        c.onTap()
        assertTrue(controls.visible)
    }

    @Test
    fun tap_ignoredWhenDisabled() {
        val controls = ControlsState(scope = testScope(), autoHideDelay = 30.seconds)
        val c = coordinator(controlsState = controls)
        c.disableAllGestures()
        c.onTap()
        assertTrue(controls.visible)
    }

    @Test
    fun tap_ignoredWhenLocked() {
        val controls = ControlsState(scope = testScope(), autoHideDelay = 30.seconds)
        controls.lock()
        val c = coordinator(controlsState = controls)
        c.onTap()
        assertFalse(controls.visible)
    }

    // -- doubleTap: TogglePlayPause --

    @Test
    fun doubleTap_togglePlayPause_callsToggle() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(doubleTapAction = PlayerSettings.DoubleTapAction.TogglePlayPause),
        )
        c.onDoubleTap(Offset(100f, 100f), IntSize(400, 300))
        assertEquals(1, controller.togglePlayPauseCalls)
    }

    // -- doubleTap: Seek --

    @Test
    fun doubleTap_seek_leftSideSeeksBackward() {
        val controller = RecordingPlaybackController()
        val feedback = mutableListOf<Long>()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(doubleTapAction = PlayerSettings.DoubleTapAction.Seek, doubleTapSeekDeltaMs = 10_000L),
            onDoubleTapFeedback = { feedback += it },
        )
        c.onDoubleTap(Offset(50f, 100f), IntSize(400, 300))
        assertEquals(listOf(-10_000L), controller.seekByCalls)
        assertEquals(listOf(-10_000L), feedback)
    }

    @Test
    fun doubleTap_seek_rightSideSeeksForward() {
        val controller = RecordingPlaybackController()
        val feedback = mutableListOf<Long>()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(doubleTapAction = PlayerSettings.DoubleTapAction.Seek, doubleTapSeekDeltaMs = 10_000L),
            onDoubleTapFeedback = { feedback += it },
        )
        c.onDoubleTap(Offset(350f, 100f), IntSize(400, 300))
        assertEquals(listOf(10_000L), controller.seekByCalls)
        assertEquals(listOf(10_000L), feedback)
    }

    // -- doubleTap: Both --

    @Test
    fun doubleTap_both_centerTogglesPlayPause() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(doubleTapAction = PlayerSettings.DoubleTapAction.Both),
        )
        c.onDoubleTap(Offset(300f, 100f), IntSize(600, 400))
        assertEquals(1, controller.togglePlayPauseCalls)
        assertTrue(controller.seekByCalls.isEmpty())
    }

    @Test
    fun doubleTap_both_leftEdgeSeeksBackward() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(doubleTapAction = PlayerSettings.DoubleTapAction.Both, doubleTapSeekDeltaMs = 5_000L),
        )
        c.onDoubleTap(Offset(50f, 100f), IntSize(600, 400))
        assertEquals(listOf(-5_000L), controller.seekByCalls)
        assertEquals(0, controller.togglePlayPauseCalls)
    }

    @Test
    fun doubleTap_ignoredWhenDisabledInConfig() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(enableDoubleTapGesture = false),
        )
        c.onDoubleTap(Offset(100f, 100f), IntSize(400, 300))
        assertEquals(0, controller.togglePlayPauseCalls)
    }

    // -- long press --

    @Test
    fun longPress_setsSpeedAndRestoresOnEnd() {
        val controller = RecordingPlaybackController()
        val feedbacks = mutableListOf<Pair<Boolean, Float>>()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(longPressSpeed = 3.0f),
            playbackSpeedProvider = { 1.5f },
            onLongPressFeedback = { active, speed -> feedbacks += active to speed },
        )

        c.onLongPressStart()
        assertEquals(listOf(3.0f), controller.speedCalls)
        assertEquals(listOf(true to 3.0f), feedbacks)

        c.onLongPressEnd()
        assertEquals(listOf(3.0f, 1.5f), controller.speedCalls)
        assertEquals(listOf(true to 3.0f, false to 1.5f), feedbacks)
    }

    @Test
    fun longPress_endWithoutStartIsNoop() {
        val controller = RecordingPlaybackController()
        val c = coordinator(controller = controller)
        c.onLongPressEnd()
        assertTrue(controller.speedCalls.isEmpty())
    }

    @Test
    fun longPress_ignoredWhenDisabledInConfig() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(enableLongPressGesture = false),
        )
        c.onLongPressStart()
        assertTrue(controller.speedCalls.isEmpty())
    }

    // -- horizontal drag (seek) --

    @Test
    fun horizontalDrag_seeksOnlyWhenGestureEnds() {
        val controller = RecordingPlaybackController()
        val c = coordinator(controller = controller)

        c.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        c.onHorizontalDrag(currentX = 100f, durationMs = 100_000L, sensitivity = 1f)

        assertEquals(emptyList(), controller.seekToCalls)

        c.onHorizontalDragEnd()

        assertEquals(listOf(45_000L), controller.seekToCalls)
    }

    @Test
    fun horizontalDrag_belowMinDeltaDoesNotSeek() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(minSeekDeltaMs = 1_000L),
        )

        c.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        // 1px * 350ms/px = 350ms delta, below 1000ms threshold
        c.onHorizontalDrag(currentX = 1f, durationMs = 100_000L, sensitivity = 1f)
        c.onHorizontalDragEnd()

        assertTrue(controller.seekToCalls.isEmpty())
    }

    @Test
    fun horizontalDrag_zeroDurationResetsSeekDelta() {
        val controller = RecordingPlaybackController()
        val c = coordinator(controller = controller)

        c.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        c.onHorizontalDrag(currentX = 100f, durationMs = 0L, sensitivity = 1f)
        c.onHorizontalDragEnd()

        assertTrue(controller.seekToCalls.isEmpty())
    }

    @Test
    fun horizontalDrag_ignoredWhenSeekDisabled() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(enableSeekGesture = false),
        )
        c.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        c.onHorizontalDrag(currentX = 100f, durationMs = 100_000L, sensitivity = 1f)
        c.onHorizontalDragEnd()
        assertTrue(controller.seekToCalls.isEmpty())
    }

    // -- vertical drag --

    @Test
    fun verticalDrag_rightSideAdjustsVolume() {
        val vbState = VolumeBrightnessState()
        val volumes = mutableListOf<Int>()
        val c = coordinator(
            volumeBrightnessState = vbState,
            onVolumeChanged = { volumes += it },
        )

        c.onVerticalDragStart(Offset(300f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        assertEquals(VolumeBrightnessState.Mode.VOLUME, vbState.activeMode)

        val result = c.onVerticalDrag(
            startValue = 50,
            startY = 200f,
            currentY = 130f,
            sensitivity = 1f,
            maxValue = 100,
            mode = VolumeBrightnessState.Mode.VOLUME,
        )
        assertEquals(60, result)
        assertEquals(listOf(60), volumes)
    }

    @Test
    fun verticalDrag_leftSideAdjustsBrightness() {
        val vbState = VolumeBrightnessState()
        val brightness = mutableListOf<Int>()
        val c = coordinator(
            volumeBrightnessState = vbState,
            onBrightnessChanged = { brightness += it },
        )

        c.onVerticalDragStart(Offset(50f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        assertEquals(VolumeBrightnessState.Mode.BRIGHTNESS, vbState.activeMode)

        val result = c.onVerticalDrag(
            startValue = 50,
            startY = 200f,
            currentY = 130f,
            sensitivity = 1f,
            maxValue = 100,
            mode = VolumeBrightnessState.Mode.BRIGHTNESS,
        )
        assertEquals(60, result)
        assertEquals(listOf(60), brightness)
    }

    @Test
    fun verticalDrag_volumeDisabled_silentlyIgnored() {
        val vbState = VolumeBrightnessState()
        val c = coordinator(
            volumeBrightnessState = vbState,
            config = GestureConfig(enableVolumeGesture = false),
        )
        c.onVerticalDragStart(Offset(300f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        assertEquals(null, vbState.activeMode)
    }

    @Test
    fun verticalDrag_brightnessDisabled_silentlyIgnored() {
        val vbState = VolumeBrightnessState()
        val c = coordinator(
            volumeBrightnessState = vbState,
            config = GestureConfig(enableBrightnessGesture = false),
        )
        c.onVerticalDragStart(Offset(50f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        assertEquals(null, vbState.activeMode)
    }

    @Test
    fun verticalDragEnd_clearsActiveMode() {
        val vbState = VolumeBrightnessState()
        val c = coordinator(volumeBrightnessState = vbState)

        c.onVerticalDragStart(Offset(300f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        c.onVerticalDragEnd()
        assertEquals(null, vbState.activeMode)
    }

    // -- transform / zoom --

    @Test
    fun transformGesture_updatesZoomState() {
        val zoomState = ZoomState()
        val c = coordinator(zoomState = zoomState)

        c.onTransformGesture(viewWidth = 1080f, viewHeight = 1920f, panChange = Offset.Zero, zoomChange = 1.5f)
        assertEquals(1.5f, zoomState.scale)
        assertTrue(zoomState.pinching)
    }

    @Test
    fun transformEnd_clearsPinching() {
        val zoomState = ZoomState()
        val zoomEnds = mutableListOf<Float>()
        val c = coordinator(zoomState = zoomState, onZoomEnd = { zoomEnds += it })

        c.onTransformGesture(viewWidth = 1080f, viewHeight = 1920f, panChange = Offset.Zero, zoomChange = 2.0f)
        c.onTransformEnd()
        assertFalse(zoomState.pinching)
        assertEquals(listOf(2.0f), zoomEnds)
    }

    @Test
    fun transformGesture_ignoredWhenZoomDisabled() {
        val zoomState = ZoomState()
        val c = coordinator(
            zoomState = zoomState,
            config = GestureConfig(enableZoomGesture = false),
        )
        c.onTransformGesture(viewWidth = 1080f, viewHeight = 1920f, panChange = Offset.Zero, zoomChange = 2.0f)
        assertEquals(1f, zoomState.scale)
    }

    @Test
    fun transformGesture_panDisabled_usesPanZero() {
        val zoomState = ZoomState()
        val c = coordinator(
            zoomState = zoomState,
            config = GestureConfig(enablePanGesture = false),
        )
        c.onTransformGesture(viewWidth = 1080f, viewHeight = 1920f, panChange = Offset(100f, 100f), zoomChange = 2.0f)
        assertEquals(Offset.Zero, zoomState.panOffset)
        assertEquals(2.0f, zoomState.scale)
    }

    // -- disabled state --

    @Test
    fun disableAllGestures_blocksAllInputs() {
        val controller = RecordingPlaybackController()
        val c = coordinator(controller = controller)

        c.disableAllGestures()

        c.onTap()
        c.onDoubleTap(Offset(100f, 100f), IntSize(400, 300))
        c.onLongPressStart()
        c.onHorizontalDragStart(positionMs = 0L, x = 0f)

        assertEquals(0, controller.togglePlayPauseCalls)
        assertTrue(controller.seekByCalls.isEmpty())
        assertTrue(controller.speedCalls.isEmpty())
    }

    @Test
    fun enableAllGestures_restoresAfterDisable() {
        val controller = RecordingPlaybackController()
        val c = coordinator(controller = controller)

        c.disableAllGestures()
        c.enableAllGestures()
        c.onDoubleTap(Offset(100f, 100f), IntSize(400, 300))
        assertEquals(1, controller.togglePlayPauseCalls)
    }

    // -- cancelActiveGesture --

    @Test
    fun cancelActiveGesture_restoresLongPressSpeed() {
        val controller = RecordingPlaybackController()
        val c = coordinator(
            controller = controller,
            config = GestureConfig(longPressSpeed = 3.0f),
            playbackSpeedProvider = { 1.0f },
        )

        c.onLongPressStart()
        c.cancelActiveGesture()

        assertEquals(listOf(3.0f, 1.0f), controller.speedCalls)
    }

    @Test
    fun cancelActiveGesture_endsSeekWithoutSeeking() {
        val controller = RecordingPlaybackController()
        val seekState = SeekState()
        val c = coordinator(controller = controller, seekState = seekState)

        c.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        c.onHorizontalDrag(currentX = 100f, durationMs = 100_000L, sensitivity = 1f)
        c.cancelActiveGesture()

        assertTrue(controller.seekToCalls.isEmpty())
        assertFalse(seekState.seeking)
    }

    @Test
    fun cancelActiveGesture_endsVerticalAdjust() {
        val vbState = VolumeBrightnessState()
        val c = coordinator(volumeBrightnessState = vbState)

        c.onVerticalDragStart(Offset(300f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        c.cancelActiveGesture()
        assertEquals(null, vbState.activeMode)
    }

    @Test
    fun cancelActiveGesture_endsTransformZoom() {
        val zoomState = ZoomState()
        val zoomEnds = mutableListOf<Float>()
        val c = coordinator(zoomState = zoomState, onZoomEnd = { zoomEnds += it })

        c.onTransformGesture(viewWidth = 1080f, viewHeight = 1920f, panChange = Offset.Zero, zoomChange = 2.0f)
        c.cancelActiveGesture()
        assertFalse(zoomState.pinching)
        assertEquals(listOf(2.0f), zoomEnds)
    }

    // -- gesture exclusivity --

    @Test
    fun gestureExclusivity_horizontalSeekBlocksVerticalDrag() {
        val controller = RecordingPlaybackController()
        val vbState = VolumeBrightnessState()
        val c = coordinator(controller = controller, volumeBrightnessState = vbState)

        c.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        c.onVerticalDragStart(Offset(300f, 200f), IntSize(400, 600), currentVolume = 50, currentBrightness = 50)
        // Machine stays in HORIZONTAL_SEEK, so vertical drag processing is blocked
        val result = c.onVerticalDrag(
            startValue = 50,
            startY = 200f,
            currentY = 100f,
            sensitivity = 1f,
            maxValue = 100,
            mode = VolumeBrightnessState.Mode.VOLUME,
        )
        assertEquals(50, result) // unchanged because machine is not in VERTICAL_ADJUST
    }
}

private class RecordingPlaybackController : PlaybackController {
    val seekToCalls = mutableListOf<Long>()
    val seekByCalls = mutableListOf<Long>()
    val speedCalls = mutableListOf<Float>()
    var togglePlayPauseCalls = 0

    override fun prepare() {}
    override fun play() {}
    override fun pause() {}
    override fun togglePlayPause() { togglePlayPauseCalls++ }
    override fun seekTo(positionMs: Long) { seekToCalls += positionMs }
    override fun seekBy(deltaMs: Long) { seekByCalls += deltaMs }
    override fun setPlaybackSpeed(speed: Float) { speedCalls += speed }
    override fun setSubtitleEnabled(enabled: Boolean, preferredGroupIndex: Int, preferredTrackIndex: Int) {}
    override fun addExternalSubtitle(uri: String, label: String?) {}
    override fun setVideoScaleMode(mode: VideoScaleMode) {}
    override fun setLoopMode(mode: LoopMode) {}
    override fun setShuffleEnabled(enabled: Boolean) {}
    override fun skipToNext() {}
    override fun skipToPrevious() {}
    override fun getRepeatMode(): LoopMode = LoopMode.OFF
    override fun isShuffleEnabled(): Boolean = false
}

package com.asuka.player.ui.controller

import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.VideoScaleMode
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GestureCoordinatorTest {

    @Test
    fun horizontalDrag_seeksOnlyWhenGestureEnds() {
        val controller = RecordingPlaybackController()
        val coordinator = GestureCoordinator(
            controller = controller,
            controlsState = ControlsState(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
                autoHideDelay = 30.seconds,
            ),
            volumeBrightnessState = VolumeBrightnessState(),
            seekState = SeekState(),
            zoomState = ZoomState(),
        )

        coordinator.onHorizontalDragStart(positionMs = 10_000L, x = 0f)
        coordinator.onHorizontalDrag(currentX = 100f, durationMs = 100_000L, sensitivity = 1f)

        assertEquals(emptyList(), controller.seekToCalls)

        coordinator.onHorizontalDragEnd()

        assertEquals(listOf(45_000L), controller.seekToCalls)
    }
}

private class RecordingPlaybackController : PlaybackController {
    val seekToCalls = mutableListOf<Long>()

    override fun prepare() {}
    override fun play() {}
    override fun pause() {}
    override fun togglePlayPause() {}
    override fun seekTo(positionMs: Long) {
        seekToCalls += positionMs
    }
    override fun seekBy(deltaMs: Long) {}
    override fun setPlaybackSpeed(speed: Float) {}
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

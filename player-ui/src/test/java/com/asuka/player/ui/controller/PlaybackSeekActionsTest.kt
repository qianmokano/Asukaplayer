package com.asuka.player.ui.controller

import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.VideoScaleMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackSeekActionsTest {
    @Test
    fun backwardAndForward_useIndependentIntervals() {
        val controller = RecordingController()
        val actions = PlaybackSeekActions(
            controller = controller,
            seekBackwardSec = 5,
            seekForwardSec = 15,
        )

        actions.backward()
        actions.forward()

        assertEquals(listOf(-5_000L, 15_000L), controller.seekByCalls)
    }

    @Test
    fun invalidIntervals_useDocumentedDefaults() {
        val controller = RecordingController()
        val actions = PlaybackSeekActions(
            controller = controller,
            seekBackwardSec = 0,
            seekForwardSec = 61,
        )

        actions.backward()
        actions.forward()

        assertEquals(listOf(-10_000L, 10_000L), controller.seekByCalls)
    }
}

private class RecordingController : PlaybackController {
    val seekByCalls = mutableListOf<Long>()

    override fun prepare() = Unit
    override fun play() = Unit
    override fun pause() = Unit
    override fun togglePlayPause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun seekBy(deltaMs: Long) {
        seekByCalls += deltaMs
    }
    override fun setPlaybackSpeed(speed: Float) = Unit
    override fun setSubtitleEnabled(enabled: Boolean, preferredGroupIndex: Int, preferredTrackIndex: Int) = Unit
    override fun addExternalSubtitle(uri: String, label: String?) = Unit
    override fun setVideoScaleMode(mode: VideoScaleMode) = Unit
    override fun setLoopMode(mode: LoopMode) = Unit
    override fun setShuffleEnabled(enabled: Boolean) = Unit
    override fun skipToNext() = Unit
    override fun skipToPrevious() = Unit
    override fun getRepeatMode(): LoopMode = LoopMode.OFF
    override fun isShuffleEnabled(): Boolean = false
}

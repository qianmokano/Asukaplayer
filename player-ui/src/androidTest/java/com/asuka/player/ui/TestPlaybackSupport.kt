package com.asuka.player.ui

import android.net.Uri
import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.VideoScaleMode
import com.asuka.player.ui.controller.PlaybackTrackSelectionController
import com.asuka.player.ui.controller.PlaybackTrackUiState
import com.asuka.player.ui.state.PlayerUiState

internal object TestPlaybackController : PlaybackController {
    override fun prepare() {}

    override fun play() {}

    override fun pause() {}

    override fun togglePlayPause() {}

    override fun seekTo(positionMs: Long) {}

    override fun seekBy(deltaMs: Long) {}

    override fun setPlaybackSpeed(speed: Float) {}

    override fun setSubtitleEnabled(
        enabled: Boolean,
        preferredGroupIndex: Int,
        preferredTrackIndex: Int,
    ) {}

    override fun addExternalSubtitle(uri: Uri, label: String?) {}

    override fun setVideoScaleMode(mode: VideoScaleMode) {}

    override fun setLoopMode(mode: LoopMode) {}

    override fun setShuffleEnabled(enabled: Boolean) {}

    override fun skipToNext() {}

    override fun skipToPrevious() {}

    override fun getRepeatMode(): LoopMode = LoopMode.OFF

    override fun isShuffleEnabled(): Boolean = false
}

internal fun testPlaybackUiPersistence(): PlaybackUiPersistence {
    return object : PlaybackUiPersistence {
        private val zooms = mutableMapOf<String, Float>()

        override fun readZoom(mediaId: String): Float? = zooms[mediaId]

        override fun saveZoom(mediaId: String, zoom: Float) {
            zooms[mediaId] = zoom
        }

        override fun readRememberedBrightness(): Float? = null

        override fun saveRememberedBrightness(brightness: Float) {}
    }
}

internal object TestPlaybackDeviceController : PlaybackDeviceController {
    override fun currentVolumePercent(): Int = 50

    override fun setVolumePercent(percent: Int) {}

    override fun currentBrightnessPercent(): Int = 50

    override fun setBrightnessPercent(percent: Int) {}
}

internal object TestPlaybackTrackSelectionController : PlaybackTrackSelectionController {
    override fun setAudioTrack(groupIndex: Int, trackIndex: Int) {}

    override fun setSubtitleTrack(groupIndex: Int, trackIndex: Int) {}

    override fun disableSubtitles() {}
}

internal fun testPlaybackScreenModel(
    uiState: PlayerUiState = PlayerUiState(),
): PlaybackScreenModel {
    return PlaybackScreenModel(
        uiState = uiState,
        trackUiState = PlaybackTrackUiState(),
    )
}

internal fun testPlaybackScreenDependencies(): PlaybackScreenDependencies {
    return PlaybackScreenDependencies(
        controller = TestPlaybackController,
        trackSelectionController = TestPlaybackTrackSelectionController,
        playbackPersistence = testPlaybackUiPersistence(),
        deviceController = TestPlaybackDeviceController,
    )
}

package com.asuka.player.ui.controller

import com.asuka.player.contract.PlaybackTrackSelectionController

class OverlayTrackActions(
    private val trackSelectionController: PlaybackTrackSelectionController,
) {
    fun setAudioTrack(track: TrackOption) {
        trackSelectionController.setAudioTrack(track.groupIndex, track.trackIndex)
    }

    fun setSubtitleTrack(track: TrackOption) {
        trackSelectionController.setSubtitleTrack(track.groupIndex, track.trackIndex)
    }

    fun disableSubtitles() {
        trackSelectionController.disableSubtitles()
    }
}

package com.asuka.player.ui.controller

class OverlayTrackActions(
    private val trackSelectionController: PlaybackTrackSelectionController,
    private val playbackPersistence: PlaybackUiPersistence,
    private val mediaIdProvider: () -> String?,
) {
    fun setAudioTrack(track: TrackOption) {
        trackSelectionController.setAudioTrack(track.groupIndex, track.trackIndex)
        mediaIdProvider()?.let { id ->
            playbackPersistence.saveAudioTrack(id, track.selectionId)
        }
    }

    fun setSubtitleTrack(track: TrackOption) {
        trackSelectionController.setSubtitleTrack(track.groupIndex, track.trackIndex)
        mediaIdProvider()?.let { id ->
            playbackPersistence.saveSubtitleTrack(id, track.selectionId)
        }
    }

    fun disableSubtitles() {
        trackSelectionController.disableSubtitles()
        mediaIdProvider()?.let { id ->
            playbackPersistence.disableSubtitles(id)
        }
    }
}

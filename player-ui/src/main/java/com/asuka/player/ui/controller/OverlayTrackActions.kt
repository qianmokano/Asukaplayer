package com.asuka.player.ui.controller

import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.TrackInfoReader
import com.asuka.player.core.TrackSelectionFacade

class OverlayTrackActions(
    private val trackSelection: TrackSelectionFacade,
    private val playbackStateRepository: PlaybackStateRepository,
    private val mediaIdProvider: () -> String?,
) {
    fun setAudioTrack(track: TrackInfoReader.TrackInfo) {
        trackSelection.setAudioTrack(track.groupIndex, track.trackIndex)
        mediaIdProvider()?.let { id ->
            playbackStateRepository.saveAudioTrack(id, track.selectionId)
        }
    }

    fun setSubtitleTrack(track: TrackInfoReader.TrackInfo) {
        trackSelection.setSubtitleTrack(track.groupIndex, track.trackIndex)
        mediaIdProvider()?.let { id ->
            playbackStateRepository.saveSubtitleTrack(id, track.selectionId)
        }
    }

    fun disableSubtitles() {
        trackSelection.disableSubtitles()
        mediaIdProvider()?.let { id ->
            playbackStateRepository.disableSubtitles(id)
        }
    }
}

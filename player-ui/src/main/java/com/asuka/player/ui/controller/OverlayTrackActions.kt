package com.asuka.player.ui.controller

import com.asuka.player.core.TrackIndexCodec
import com.asuka.player.core.TrackSelectionFacade
import com.asuka.player.data.PlaybackStore

class OverlayTrackActions(
    private val trackSelection: TrackSelectionFacade,
    private val store: PlaybackStore,
    private val mediaIdProvider: () -> String?,
) {
    fun setAudioTrack(groupIndex: Int, trackIndex: Int) {
        trackSelection.setAudioTrack(groupIndex, trackIndex)
        mediaIdProvider()?.let { id ->
            store.saveAudioTrack(id, TrackIndexCodec.encode(groupIndex, trackIndex))
        }
    }

    fun setSubtitleTrack(groupIndex: Int, trackIndex: Int) {
        trackSelection.setSubtitleTrack(groupIndex, trackIndex)
        mediaIdProvider()?.let { id ->
            store.saveSubtitleTrack(id, TrackIndexCodec.encode(groupIndex, trackIndex))
        }
    }

    fun disableSubtitles() {
        trackSelection.disableSubtitles()
        mediaIdProvider()?.let { id -> store.saveSubtitleTrack(id, -1) }
    }
}

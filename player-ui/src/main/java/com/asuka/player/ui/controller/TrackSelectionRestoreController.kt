package com.asuka.player.ui.controller

import androidx.media3.common.C
import com.asuka.player.contract.PersistedTrackSelection
import com.asuka.player.platform.TrackInfoReader
import com.asuka.player.contract.TrackSelectionRestoreRequest

internal class TrackSelectionRestoreController(
    private val currentMediaIdProvider: () -> String?,
    private val tracksReadyProvider: () -> Boolean,
    private val availableTracksProvider: () -> List<TrackInfoReader.TrackInfo>,
    private val applyAudioTrack: (groupIndex: Int, trackIndex: Int) -> Unit,
    private val applySubtitleTrack: (groupIndex: Int, trackIndex: Int) -> Unit,
    private val disableSubtitles: () -> Unit,
) {
    private var pending: TrackSelectionRestoreRequest? = null

    fun schedule(request: TrackSelectionRestoreRequest?) {
        pending = request
    }

    fun clear() {
        pending = null
    }

    fun applyIfReady(): Boolean {
        val request = pending ?: return false
        if (currentMediaIdProvider() != request.mediaId) return false
        if (!tracksReadyProvider()) return false

        pending = null
        val availableTracks = availableTracksProvider()

        when (val subtitleSelection = request.subtitleTrackSelection) {
            null -> Unit
            else -> restoreSubtitleSelection(subtitleSelection, availableTracks)
        }

        request.audioTrackSelection?.let { audioSelection ->
            restoreSelection(
                selection = audioSelection,
                tracks = availableTracks,
                type = C.TRACK_TYPE_AUDIO,
                applyTrack = applyAudioTrack,
            )
        }

        return true
    }

    private fun restoreSubtitleSelection(
        selection: PersistedTrackSelection,
        tracks: List<TrackInfoReader.TrackInfo>,
    ) {
        if (selection.isDisabledSubtitle) {
            disableSubtitles()
            return
        }
        restoreSelection(
            selection = selection,
            tracks = tracks,
            type = C.TRACK_TYPE_TEXT,
            applyTrack = applySubtitleTrack,
        )
    }

    private fun restoreSelection(
        selection: PersistedTrackSelection,
        tracks: List<TrackInfoReader.TrackInfo>,
        type: Int,
        applyTrack: (groupIndex: Int, trackIndex: Int) -> Unit,
    ) {
        val track = tracks.firstOrNull { it.type == type && it.selectionId == selection.stableId } ?: return
        applyTrack(track.groupIndex, track.trackIndex)
    }
}

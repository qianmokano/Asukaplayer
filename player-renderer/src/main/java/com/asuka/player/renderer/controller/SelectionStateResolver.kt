package com.asuka.player.renderer.controller

import com.asuka.player.contract.TrackIndexCodec
import com.asuka.player.platform.TrackSelectionStateReader

internal object SelectionStateResolver {
    fun audioSelection(selected: TrackSelectionStateReader.Selected?): Int? {
        return selected?.let { TrackIndexCodec.encode(it.groupIndex, it.trackIndex) }
    }

    fun subtitleSelection(
        selected: TrackSelectionStateReader.Selected?,
        subtitlesDisabled: Boolean,
        hasTextTracks: Boolean,
    ): Int? {
        return when {
            selected != null -> TrackIndexCodec.encode(selected.groupIndex, selected.trackIndex)
            subtitlesDisabled && hasTextTracks -> TrackIndexCodec.SUBTITLE_DISABLED
            else -> null
        }
    }
}

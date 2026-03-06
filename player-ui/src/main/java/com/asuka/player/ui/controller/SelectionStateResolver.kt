package com.asuka.player.ui.controller

import com.asuka.player.core.TrackIndexCodec
import com.asuka.player.core.TrackSelectionStateReader

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

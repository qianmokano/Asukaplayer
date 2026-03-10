package com.asuka.player.core

import androidx.media3.common.C
import androidx.media3.common.Player

/**
 * Reads current selection override for audio/subtitle.
 */
class TrackSelectionStateReader(private val player: Player) {
    data class Selected(val type: Int, val groupIndex: Int, val trackIndex: Int)

    fun read(): List<Selected> {
        val params = player.trackSelectionParameters
        val results = mutableListOf<Selected>()
        params.overrides.values.forEach { override ->
            // TrackGroup.equals() is content-based (compares id + all Format entries), not
            // reference-based, so this lookup is correct even if Media3 wraps the same
            // underlying group in a new object between calls.
            val groupIndex = player.currentTracks.groups.indexOfFirst { it.mediaTrackGroup == override.mediaTrackGroup }
            if (groupIndex < 0) return@forEach
            val type = player.currentTracks.groups[groupIndex].type
            if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) return@forEach
            val trackIndex = override.trackIndices.firstOrNull() ?: return@forEach
            results += Selected(type, groupIndex, trackIndex)
        }
        return results
    }
}

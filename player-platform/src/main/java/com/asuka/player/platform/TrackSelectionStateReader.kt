package com.asuka.player.platform

import androidx.media3.common.C
import androidx.media3.common.Player

class TrackSelectionStateReader(private val player: Player) {
    data class Selected(val type: Int, val groupIndex: Int, val trackIndex: Int)

    fun read(): List<Selected> {
        val results = mutableListOf<Selected>()
        player.currentTracks.groups.forEachIndexed { groupIndex, group ->
            val type = group.type
            if (type != C.TRACK_TYPE_AUDIO && type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            val trackIndex = (0 until group.length).firstOrNull(group::isTrackSelected)
                ?: return@forEachIndexed
            results += Selected(type, groupIndex, trackIndex)
        }
        return results
    }
}

package com.asuka.player.core

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi

/**
 * Extracts simple track info for UI.
 */
@OptIn(UnstableApi::class)
class TrackInfoReader(private val player: Player) {

    data class TrackInfo(
        val groupIndex: Int,
        val trackIndex: Int,
        val type: Int,
        val label: String,
        val language: String?,
        val selectionId: String,
    )

    fun listTracks(): List<TrackInfo> {
        val tracks = player.currentTracks
        val results = mutableListOf<TrackInfo>()
        // Per-type counter used to generate "Audio 1", "Audio 2", "Sub 1" fallback labels.
        // Every track from every group is added to results (no type filtering), so the counter
        // increments exactly once per track per type, keeping ordinals consecutive within each type.
        val typeCounters = mutableMapOf<Int, Int>()
        tracks.groups.forEachIndexed { groupIndex, group ->
            val type = group.type
            val trackGroup = group.mediaTrackGroup
            val count = trackGroup.length
            for (trackIndex in 0 until count) {
                val format = trackGroup.getFormat(trackIndex)
                val typeOrdinal = (typeCounters[type] ?: 0) + 1
                typeCounters[type] = typeOrdinal
                val label = format.label ?: when (type) {
                    C.TRACK_TYPE_AUDIO -> "Audio $typeOrdinal"
                    C.TRACK_TYPE_TEXT -> "Sub $typeOrdinal"
                    else -> "Track $typeOrdinal"
                }
                results += TrackInfo(
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                    type = type,
                    label = label,
                    language = format.language,
                    selectionId = TrackSelectionIdentity.create(type, format),
                )
            }
        }
        return results
    }
}

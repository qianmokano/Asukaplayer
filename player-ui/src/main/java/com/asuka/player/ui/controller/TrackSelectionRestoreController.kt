package com.asuka.player.ui.controller

import com.asuka.player.core.TrackIndexCodec
import com.asuka.player.core.TrackSelectionRestoreRequest

internal class TrackSelectionRestoreController(
    private val currentMediaIdProvider: () -> String?,
    private val trackGroupCountProvider: () -> Int,
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
        if (trackGroupCountProvider() <= 0) return false

        pending = null

        when (val subtitleIndex = request.subtitleTrackIndex) {
            null -> Unit
            TrackIndexCodec.SUBTITLE_DISABLED -> disableSubtitles()
            else -> {
                val (groupIndex, trackIndex) = TrackIndexCodec.decode(subtitleIndex)
                applySubtitleTrack(groupIndex, trackIndex)
            }
        }

        request.audioTrackIndex?.let { encodedIndex ->
            val (groupIndex, trackIndex) = TrackIndexCodec.decode(encodedIndex)
            applyAudioTrack(groupIndex, trackIndex)
        }

        return true
    }
}

package com.asuka.player.core

import com.asuka.player.data.PlaybackStore

/**
 * Reads persisted state for a given media id.
 */
class PlaybackStateRestorer(
    private val store: PlaybackStore,
) {
    data class ResumeInfo(
        val positionMs: Long,
        val speed: Float,
        val audioTrackIndex: Int?,
        val subtitleTrackIndex: Int?,
        val zoom: Float?,
    )

    fun read(mediaId: String): ResumeInfo {
        return ResumeInfo(
            positionMs = store.loadPosition(mediaId) ?: 0L,
            speed = store.loadPlaybackSpeed(mediaId) ?: 1.0f,
            audioTrackIndex = store.loadAudioTrack(mediaId),
            subtitleTrackIndex = store.loadSubtitleTrack(mediaId),
            zoom = store.loadZoom(mediaId),
        )
    }
}

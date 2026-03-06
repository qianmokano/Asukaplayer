package com.asuka.player.core

import com.asuka.player.data.PlaybackStore

data class ResumeState(
    val positionMs: Long,
    val speed: Float?,
    val audioTrackIndex: Int?,
    val subtitleTrackIndex: Int?,
    val zoom: Float?,
)

class PlaybackStateRepository(
    private val store: PlaybackStore,
) {
    /**
     * Safe to call from any thread as long as the provided [PlaybackStore]
     * implementation honors the store contract.
     */
    fun readResumeState(mediaId: String): ResumeState {
        return ResumeState(
            positionMs = store.loadPosition(mediaId) ?: 0L,
            speed = store.loadPlaybackSpeed(mediaId),
            audioTrackIndex = store.loadAudioTrack(mediaId),
            subtitleTrackIndex = store.loadSubtitleTrack(mediaId),
            zoom = store.loadZoom(mediaId),
        )
    }

    fun recentMediaIds(limit: Int = 50): List<String> = store.recentMediaIds(limit)

    fun savePlaybackSpeed(mediaId: String, speed: Float) {
        store.savePlaybackSpeed(mediaId, speed)
    }

    fun saveAudioTrack(mediaId: String, trackIndex: Int) {
        store.saveAudioTrack(mediaId, trackIndex)
    }

    fun saveSubtitleTrack(mediaId: String, trackIndex: Int) {
        store.saveSubtitleTrack(mediaId, trackIndex)
    }

    fun saveZoom(mediaId: String, zoom: Float) {
        store.saveZoom(mediaId, zoom)
    }
}

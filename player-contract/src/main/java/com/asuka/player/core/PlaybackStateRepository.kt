package com.asuka.player.contract

data class ResumeState(
    val positionMs: Long,
    val speed: Float?,
    val audioTrackSelection: PersistedTrackSelection?,
    val subtitleTrackSelection: PersistedTrackSelection?,
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
            audioTrackSelection = store.loadAudioTrackId(mediaId)
                ?.takeIf { it.isNotBlank() }
                ?.let(::PersistedTrackSelection),
            subtitleTrackSelection = store.loadSubtitleTrackId(mediaId)
                ?.takeIf { it.isNotBlank() }
                ?.let(::PersistedTrackSelection),
            zoom = store.loadZoom(mediaId),
        )
    }

    fun recentMediaIds(limit: Int = 50): List<String> = store.recentMediaIds(limit)

    fun savePlaybackSpeed(mediaId: String, speed: Float) {
        store.savePlaybackSpeed(mediaId, speed)
    }

    fun saveAudioTrack(mediaId: String, trackId: String) {
        store.saveAudioTrackId(mediaId, trackId)
    }

    fun saveSubtitleTrack(mediaId: String, trackId: String) {
        store.saveSubtitleTrackId(mediaId, trackId)
    }

    fun disableSubtitles(mediaId: String) {
        store.saveSubtitleTrackId(mediaId, PersistedTrackSelection.DISABLED_SUBTITLE_ID)
    }

    fun saveZoom(mediaId: String, zoom: Float) {
        store.saveZoom(mediaId, zoom)
    }
}

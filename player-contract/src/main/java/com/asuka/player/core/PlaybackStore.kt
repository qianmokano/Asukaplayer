package com.asuka.player.contract

/**
 * Persistence abstraction for playback state.
 *
 * Each method is keyed by [mediaId], which is typically the media URI string
 * (e.g. `"content://media/external/video/media/42"`). Implementations must
 * accept any non-empty string.
 *
 * **Null semantics:** `load*` methods return `null` when no value has been
 * persisted for the given [mediaId]. A `save*` call with a valid value must
 * make it available to the corresponding `load*` immediately on the same store
 * instance, even if the write has not yet been flushed to disk.
 *
 * **Thread safety:** Implementations must be safe to call from any thread.
 * Callers should not need to confine access to a specific dispatcher or looper
 * just to avoid crashes.
 */
interface PlaybackStore {
    /**
     * Returns most-recently-used media ids, newest first.
     *
     * Not all store implementations persist a recent list. The default
     * implementation returns an empty list.
     */
    suspend fun recentMediaIds(limit: Int = 50): List<String> = emptyList()

    suspend fun loadPosition(mediaId: String): Long?
    suspend fun savePosition(mediaId: String, positionMs: Long)

    suspend fun loadPlaybackSpeed(mediaId: String): Float?
    suspend fun savePlaybackSpeed(mediaId: String, speed: Float)

    suspend fun loadAudioTrackId(mediaId: String): String?
    suspend fun saveAudioTrackId(mediaId: String, trackId: String)

    suspend fun loadSubtitleTrackId(mediaId: String): String?
    suspend fun saveSubtitleTrackId(mediaId: String, trackId: String)

    suspend fun loadZoom(mediaId: String): Float?
    suspend fun saveZoom(mediaId: String, zoom: Float)
}

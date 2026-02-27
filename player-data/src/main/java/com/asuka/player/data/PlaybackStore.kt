package com.asuka.player.data

/**
 * Persistence abstraction for playback state.
 *
 * Each method is keyed by [mediaId], which is typically the media URI string
 * (e.g. `"content://media/external/video/media/42"`). Implementations must
 * accept any non-empty string.
 *
 * **Null semantics:** `load*` methods return `null` when no value has been
 * persisted for the given [mediaId]. A `save*` call with a valid value must
 * make it available to the corresponding `load*` immediately (within the same
 * thread context), even if the write has not yet been flushed to disk.
 *
 * **Thread safety:** Implementations are not required to be thread-safe.
 * Callers must ensure that reads and writes for a given store instance are
 * serialized (e.g. confined to the main thread). See concrete implementations
 * for their specific threading guarantees.
 */
interface PlaybackStore {
    fun loadPosition(mediaId: String): Long?
    fun savePosition(mediaId: String, positionMs: Long)

    fun loadPlaybackSpeed(mediaId: String): Float?
    fun savePlaybackSpeed(mediaId: String, speed: Float)

    fun loadAudioTrack(mediaId: String): Int?
    fun saveAudioTrack(mediaId: String, trackIndex: Int)

    fun loadSubtitleTrack(mediaId: String): Int?
    fun saveSubtitleTrack(mediaId: String, trackIndex: Int)

    fun loadZoom(mediaId: String): Float?
    fun saveZoom(mediaId: String, zoom: Float)
}

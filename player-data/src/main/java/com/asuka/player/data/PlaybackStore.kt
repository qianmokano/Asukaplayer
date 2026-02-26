package com.asuka.player.data

/**
 * Persistence abstraction for playback state.
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

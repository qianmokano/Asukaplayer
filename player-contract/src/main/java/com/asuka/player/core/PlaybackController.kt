package com.asuka.player.contract

/**
 * UI-facing controller for playback actions.
 * Implemented by the core module, used by UI without direct player access.
 */
interface PlaybackController {
    fun prepare()
    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun setPlaybackSpeed(speed: Float)
    fun setSubtitleEnabled(enabled: Boolean, preferredGroupIndex: Int = -1, preferredTrackIndex: Int = 0)
    fun addExternalSubtitle(uri: String, label: String? = null)
    fun setVideoScaleMode(mode: VideoScaleMode)
    fun setLoopMode(mode: LoopMode)
    fun setShuffleEnabled(enabled: Boolean)
    fun skipToNext()
    fun skipToPrevious()
    fun getRepeatMode(): LoopMode
    fun isShuffleEnabled(): Boolean
}

enum class VideoScaleMode {
    FIT,
    FILL,
    CROP,
    STRETCH,
}

enum class LoopMode {
    OFF,
    ONE,
    ALL,
}

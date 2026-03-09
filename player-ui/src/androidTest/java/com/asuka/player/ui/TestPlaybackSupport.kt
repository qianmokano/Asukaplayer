package com.asuka.player.ui

import android.net.Uri
import com.asuka.player.core.LoopMode
import com.asuka.player.core.PlaybackController
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.VideoScaleMode
import com.asuka.player.data.InMemoryPlaybackStore

internal object TestPlaybackController : PlaybackController {
    override fun prepare() {}

    override fun play() {}

    override fun pause() {}

    override fun togglePlayPause() {}

    override fun seekTo(positionMs: Long) {}

    override fun seekBy(deltaMs: Long) {}

    override fun setPlaybackSpeed(speed: Float) {}

    override fun setSubtitleEnabled(
        enabled: Boolean,
        preferredGroupIndex: Int,
        preferredTrackIndex: Int,
    ) {}

    override fun addExternalSubtitle(uri: Uri, label: String?) {}

    override fun setVideoScaleMode(mode: VideoScaleMode) {}

    override fun setLoopMode(mode: LoopMode) {}

    override fun setShuffleEnabled(enabled: Boolean) {}

    override fun skipToNext() {}

    override fun skipToPrevious() {}

    override fun getRepeatMode(): LoopMode = LoopMode.OFF

    override fun isShuffleEnabled(): Boolean = false
}

internal fun testPlaybackStateRepository(): PlaybackStateRepository {
    return PlaybackStateRepository(InMemoryPlaybackStore())
}

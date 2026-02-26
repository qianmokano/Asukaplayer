package com.asuka.player.core.impl

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import com.asuka.player.core.LoopMode
import com.asuka.player.core.PlaybackController
import com.asuka.player.core.TrackSelectionFacade
import com.asuka.player.core.VideoScaleMode

/**
 * Media3-backed controller. Keeps UI independent of player APIs.
 */
@OptIn(UnstableApi::class)
class Media3PlaybackController(
    private val controller: MediaController,
) : PlaybackController {

    private val trackSelection = TrackSelectionFacade(controller)
    override fun play() = controller.play()

    override fun pause() = controller.pause()

    override fun togglePlayPause() {
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    override fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    override fun seekBy(deltaMs: Long) {
        val duration = controller.duration
        val target = if (duration == C.TIME_UNSET || duration <= 0L) {
            (controller.currentPosition + deltaMs).coerceAtLeast(0L)
        } else {
            (controller.currentPosition + deltaMs).coerceIn(0L, duration)
        }
        controller.seekTo(target)
    }

    override fun setPlaybackSpeed(speed: Float) {
        controller.setPlaybackSpeed(speed)
    }

    override fun setSubtitleEnabled(enabled: Boolean) {
        if (!enabled) {
            trackSelection.disableSubtitles()
            return
        }
        val groupIndex = controller.currentTracks.groups.indexOfFirst { it.type == C.TRACK_TYPE_TEXT }.takeIf { it >= 0 }
        if (groupIndex != null) {
            trackSelection.setSubtitleTrack(groupIndex, 0)
        }
    }

    override fun addExternalSubtitle(uri: Uri, label: String?) {
        val current = controller.currentMediaItem ?: run {
            Log.w(TAG, "addExternalSubtitle: no current media item, ignoring uri=$uri")
            return
        }
        val builder = current.buildUpon()
        val configuration = MediaItem.SubtitleConfiguration.Builder(uri)
            .setLabel(label)
            .setMimeType(subtitleMimeType(uri))
            .build()
        val existing = current.localConfiguration?.subtitleConfigurations ?: emptyList()
        builder.setSubtitleConfigurations(existing + configuration)
        controller.replaceMediaItem(controller.currentMediaItemIndex, builder.build())
    }

    private fun subtitleMimeType(uri: Uri): String =
        when (uri.lastPathSegment?.substringAfterLast('.')?.lowercase()) {
            "srt" -> "application/x-subrip"
            "vtt", "webvtt" -> "text/vtt"
            "ass", "ssa" -> "text/x-ssa"
            "ttml", "dfxp" -> "application/ttml+xml"
            else -> "application/x-subrip"
        }

    override fun setVideoScaleMode(mode: VideoScaleMode) {
        val scaleType = when (mode) {
            VideoScaleMode.FIT, VideoScaleMode.STRETCH -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            VideoScaleMode.FILL, VideoScaleMode.CROP -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }
        val args = Bundle().apply { putInt(ARG_SCALE_TYPE, scaleType) }
        controller.sendCustomCommand(SessionCommand(CMD_SET_VIDEO_SCALE_TYPE, Bundle.EMPTY), args)
    }

    override fun setLoopMode(mode: LoopMode) {
        controller.repeatMode = when (mode) {
            LoopMode.OFF -> Player.REPEAT_MODE_OFF
            LoopMode.ONE -> Player.REPEAT_MODE_ONE
            LoopMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        controller.shuffleModeEnabled = enabled
    }

    override fun skipToNext() {
        if (controller.hasNextMediaItem()) controller.seekToNext()
    }

    override fun skipToPrevious() {
        if (controller.hasPreviousMediaItem()) controller.seekToPrevious()
    }

    override fun getRepeatMode(): LoopMode = when (controller.repeatMode) {
        Player.REPEAT_MODE_OFF -> LoopMode.OFF
        Player.REPEAT_MODE_ONE -> LoopMode.ONE
        else -> LoopMode.ALL
    }

    override fun isShuffleEnabled(): Boolean = controller.shuffleModeEnabled

    companion object {
        private const val TAG = "Media3PlaybackController"
        internal const val CMD_SET_VIDEO_SCALE_TYPE = "cmd_set_video_scale_type"
        internal const val ARG_SCALE_TYPE = "scale_type"
    }
}

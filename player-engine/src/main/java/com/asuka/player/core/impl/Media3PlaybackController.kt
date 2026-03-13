package com.asuka.player.engine

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.asuka.player.contract.LoopMode
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.contract.VideoScaleMode
import com.asuka.player.platform.DefaultPlaybackTrackSelectionController
import com.asuka.player.platform.PlaybackControllerConnector
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.asuka.player.platform.PlaybackCustomCommands
import com.asuka.player.platform.TrackSelectionFacade
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
class Media3PlaybackController(
    private val controller: MediaController,
) : PlaybackController {
    private val trackSelection = TrackSelectionFacade(controller)

    private inline fun ifConnected(block: () -> Unit) {
        if (controller.isConnected) block()
    }

    override fun prepare() = ifConnected { controller.prepare() }

    override fun play() = ifConnected { controller.play() }

    override fun pause() = ifConnected { controller.pause() }

    override fun togglePlayPause() = ifConnected {
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    override fun seekTo(positionMs: Long) = ifConnected { controller.seekTo(positionMs) }

    override fun seekBy(deltaMs: Long) = ifConnected {
        val duration = controller.duration
        val target = if (duration == C.TIME_UNSET || duration <= 0L) {
            (controller.currentPosition + deltaMs).coerceAtLeast(0L)
        } else {
            (controller.currentPosition + deltaMs).coerceIn(0L, duration)
        }
        controller.seekTo(target)
    }

    override fun setPlaybackSpeed(speed: Float) = ifConnected {
        controller.setPlaybackSpeed(speed)
    }

    override fun setSubtitleEnabled(enabled: Boolean, preferredGroupIndex: Int, preferredTrackIndex: Int) = ifConnected {
        if (!enabled) {
            trackSelection.disableSubtitles()
            return@ifConnected
        }
        if (preferredGroupIndex >= 0) {
            val groups = controller.currentTracks.groups
            if (preferredGroupIndex < groups.size && groups[preferredGroupIndex].type == C.TRACK_TYPE_TEXT) {
                trackSelection.setSubtitleTrack(preferredGroupIndex, preferredTrackIndex)
                return@ifConnected
            }
        }
        val groupIndex = controller.currentTracks.groups.indexOfFirst { it.type == C.TRACK_TYPE_TEXT }.takeIf { it >= 0 }
        if (groupIndex != null) {
            trackSelection.setSubtitleTrack(groupIndex, 0)
        }
    }

    override fun addExternalSubtitle(uri: String, label: String?) = ifConnected {
        val subtitleUri = android.net.Uri.parse(uri)
        val current = controller.currentMediaItem ?: run {
            Log.w(TAG, "addExternalSubtitle: no current media item, ignoring uri=$uri")
            return@ifConnected
        }
        val builder = current.buildUpon()
        val configuration = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setLabel(label)
            .setMimeType(subtitleMimeType(subtitleUri))
            .build()
        val existing = current.localConfiguration?.subtitleConfigurations ?: emptyList()
        builder.setSubtitleConfigurations(existing + configuration)
        controller.replaceMediaItem(controller.currentMediaItemIndex, builder.build())
    }

    private fun subtitleMimeType(uri: android.net.Uri): String =
        when (uri.lastPathSegment?.substringAfterLast('.')?.lowercase()) {
            "srt" -> "application/x-subrip"
            "vtt", "webvtt" -> "text/vtt"
            "ass", "ssa" -> "text/x-ssa"
            "ttml", "dfxp" -> "application/ttml+xml"
            else -> "application/x-subrip"
        }

    override fun setVideoScaleMode(mode: VideoScaleMode) = ifConnected {
        val scaleType = when (mode) {
            VideoScaleMode.FIT -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            VideoScaleMode.FILL, VideoScaleMode.CROP, VideoScaleMode.STRETCH ->
                C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        }
        val args = Bundle().apply { putInt(PlaybackCustomCommands.ARG_SCALE_TYPE, scaleType) }
        controller.sendCustomCommand(
            SessionCommand(PlaybackCustomCommands.CMD_SET_VIDEO_SCALE_TYPE, Bundle.EMPTY),
            args,
        )
    }

    override fun setLoopMode(mode: LoopMode) = ifConnected {
        controller.repeatMode = when (mode) {
            LoopMode.OFF -> Player.REPEAT_MODE_OFF
            LoopMode.ONE -> Player.REPEAT_MODE_ONE
            LoopMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    override fun setShuffleEnabled(enabled: Boolean) = ifConnected {
        controller.shuffleModeEnabled = enabled
    }

    override fun skipToNext() = ifConnected {
        if (controller.hasNextMediaItem()) controller.seekToNext()
    }

    override fun skipToPrevious() = ifConnected {
        if (controller.hasPreviousMediaItem()) controller.seekToPrevious()
    }

    override fun getRepeatMode(): LoopMode {
        if (!controller.isConnected) return LoopMode.OFF
        return when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> LoopMode.OFF
            Player.REPEAT_MODE_ONE -> LoopMode.ONE
            else -> LoopMode.ALL
        }
    }

    override fun isShuffleEnabled(): Boolean {
        if (!controller.isConnected) return false
        return controller.shuffleModeEnabled
    }

    companion object {
        private const val TAG = "Media3PlaybackController"
    }
}

class Media3PlaybackControllerConnector(
    private val context: Context,
    private val playbackServiceComponent: ComponentName,
) : PlaybackControllerConnector {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun buildAsync(): ListenableFuture<MediaController> {
        val future = MediaController.Builder(
            context,
            SessionToken(context, playbackServiceComponent),
        ).buildAsync()
        controllerFuture = future
        return future
    }

    override fun asPlaybackController(mediaController: MediaController): PlaybackController {
        return Media3PlaybackController(mediaController)
    }

    override fun asTrackSelectionController(mediaController: MediaController): PlaybackTrackSelectionController {
        return DefaultPlaybackTrackSelectionController(mediaController)
    }

    override fun release() {
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
    }
}

object Media3PlaybackControllerConnectorFactory : PlaybackControllerConnectorFactory {
    override fun create(
        context: Context,
        playbackServiceComponent: ComponentName,
    ): PlaybackControllerConnector {
        return Media3PlaybackControllerConnector(
            context = context,
            playbackServiceComponent = playbackServiceComponent,
        )
    }
}

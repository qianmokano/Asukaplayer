package com.asuka.player.ui.controller

import android.content.Intent
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.core.IntentQueueReader
import com.asuka.player.core.PlaybackSessionPlan
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.PlaybackStartupPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackSessionCoordinator(
    private val mediaController: MediaController,
    private val controllerBindings: ControllerBindings,
    private val sessionPlanner: PlaybackSessionPlanner,
    private val titleResolver: suspend (Uri) -> String?,
) : Player.Listener {
    private val trackSelectionRestoreController = TrackSelectionRestoreController(
        currentMediaIdProvider = { mediaController.currentMediaItem?.mediaId },
        trackGroupCountProvider = { mediaController.currentTracks.groups.size },
        applyAudioTrack = { groupIndex, trackIndex ->
            controllerBindings.trackSelection.setAudioTrack(groupIndex, trackIndex)
        },
        applySubtitleTrack = { groupIndex, trackIndex ->
            controllerBindings.trackSelection.setSubtitleTrack(groupIndex, trackIndex)
        },
        disableSubtitles = {
            controllerBindings.trackSelection.disableSubtitles()
        },
    )

    fun attach() {
        mediaController.addListener(this)
    }

    fun detach() {
        mediaController.removeListener(this)
        trackSelectionRestoreController.clear()
    }

    suspend fun start(
        targetUri: Uri?,
        launchIntent: Intent?,
        autoplay: Boolean,
        policy: PlaybackStartupPolicy,
    ): PlaybackSessionPlan? {
        val target = targetUri ?: return null
        val launchNeighbors = launchIntent?.let { IntentQueueReader.read(it) }.orEmpty()
        val resolvedTitles = withContext(Dispatchers.IO) {
            (listOf(target) + launchNeighbors)
                .distinct()
                .associateWith { uri -> titleResolver(uri) }
        }
        val plan = sessionPlanner.plan(
            targetUri = target,
            launchNeighbors = launchNeighbors,
            resolvedTitles = resolvedTitles,
            policy = policy,
        )
        applyPlan(plan, autoplay)
        return plan
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        trackSelectionRestoreController.applyIfReady()
    }

    private fun applyPlan(
        plan: PlaybackSessionPlan,
        autoplay: Boolean,
    ) {
        mediaController.setMediaItems(
            plan.queue.items,
            plan.queue.startIndex,
            plan.resumePositionMs,
        )
        mediaController.setPlaybackSpeed(plan.playbackSpeed)
        mediaController.prepare()
        trackSelectionRestoreController.schedule(plan.trackSelectionRestoreRequest)
        if (autoplay) {
            mediaController.play()
        } else {
            mediaController.pause()
        }
    }
}

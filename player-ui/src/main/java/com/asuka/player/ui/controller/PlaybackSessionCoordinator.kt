package com.asuka.player.ui.controller

import android.content.Intent
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.platform.IntentQueueReader
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionPlan
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.platform.toMediaItems
import com.asuka.player.platform.TrackInfoReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackSessionCoordinator(
    private val mediaController: MediaController,
    private val trackSelectionController: PlaybackTrackSelectionController,
    private val sessionPlanner: PlaybackSessionPlanner,
    private val titleResolver: suspend (Uri) -> String?,
) : Player.Listener {
    private val trackInfoReader = TrackInfoReader(mediaController)
    private val trackSelectionRestoreController = TrackSelectionRestoreController(
        currentMediaIdProvider = { mediaController.currentMediaItem?.mediaId },
        tracksReadyProvider = { mediaController.currentTracks.groups.isNotEmpty() },
        availableTracksProvider = { trackInfoReader.listTracks() },
        applyAudioTrack = { groupIndex, trackIndex ->
            trackSelectionController.setAudioTrack(groupIndex, trackIndex)
        },
        applySubtitleTrack = { groupIndex, trackIndex ->
            trackSelectionController.setSubtitleTrack(groupIndex, trackIndex)
        },
        disableSubtitles = {
            trackSelectionController.disableSubtitles()
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
        val targetMediaId = launchIntent?.let(IntentQueueReader::readTargetMediaId) ?: target.toString()
        val launchNeighbors = launchIntent?.let(IntentQueueReader::readEntries).orEmpty()
        val resolvedTitles = withContext(Dispatchers.IO) {
            (listOf(target.toString()) + launchNeighbors.map(PlaybackQueueEntry::uri))
                .distinct()
                .associateWith { uri -> titleResolver(Uri.parse(uri)) }
        }
        val plan = sessionPlanner.plan(
            target = PlaybackQueueEntry(mediaId = targetMediaId, uri = target.toString()),
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
            plan.queue.toMediaItems(),
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

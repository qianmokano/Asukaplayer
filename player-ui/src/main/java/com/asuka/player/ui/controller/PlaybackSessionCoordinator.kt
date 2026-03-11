package com.asuka.player.ui.controller

import android.content.Intent
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionPlan
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.platform.PlaybackIntentPayloadCodec
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
        val payload = launchIntent?.let(PlaybackIntentPayloadCodec::readPlaybackIntent)
        val targetEntry = payload?.targetEntry ?: PlaybackQueueEntry(
            mediaId = target.toString(),
            uri = target.toString(),
        )
        val launchNeighbors = payload?.queueEntries.orEmpty()
        val plan = withContext(Dispatchers.IO) {
            (listOf(targetEntry.uri) + launchNeighbors.map(PlaybackQueueEntry::uri))
                .distinct()
                .associateWith { uri -> titleResolver(Uri.parse(uri)) }
                .let { resolvedTitles ->
                    sessionPlanner.plan(
                        target = targetEntry,
                        launchNeighbors = launchNeighbors,
                        resolvedTitles = resolvedTitles,
                        policy = policy,
                    )
                }
        }
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

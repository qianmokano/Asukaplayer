package com.asuka.player.renderer.controller

import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionPlan
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.platform.toMediaItems
import com.asuka.player.platform.TrackInfoReader
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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

    data class StartResult(
        val request: PlaybackSessionRequest,
        val plan: PlaybackSessionPlan,
    )

    suspend fun prepareStart(
        request: PlaybackSessionRequest,
    ): StartResult? {
        val policy = request.startupPolicy ?: return null
        val targetEntry = request.targetEntry
        val launchNeighbors = request.queueEntries
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
        coroutineContext.ensureActive()
        return StartResult(
            request = request,
            plan = plan,
        )
    }

    fun applyStart(result: StartResult) {
        applyPlan(
            plan = result.plan,
            request = result.request,
        )
    }

    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
        trackSelectionRestoreController.applyIfReady()
    }

    private fun applyPlan(
        plan: PlaybackSessionPlan,
        request: PlaybackSessionRequest,
    ) {
        mediaController.setMediaItems(
            plan.queue.toMediaItems(
                targetMediaId = request.targetEntry.mediaId,
                targetPlaybackUri = request.playbackUri,
            ),
            plan.queue.startIndex,
            plan.resumePositionMs,
        )
        mediaController.setPlaybackSpeed(plan.playbackSpeed)
        mediaController.prepare()
        trackSelectionRestoreController.schedule(plan.trackSelectionRestoreRequest)
        if (request.autoplay == true) {
            mediaController.play()
        } else {
            mediaController.pause()
        }
    }
}

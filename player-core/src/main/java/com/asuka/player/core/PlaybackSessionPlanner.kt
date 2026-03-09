package com.asuka.player.core

import android.net.Uri

data class PlaybackStartupPolicy(
    val resumePlayback: Boolean,
    val defaultPlaybackSpeed: Float,
    val rememberTrackSelections: Boolean,
)

data class TrackSelectionRestoreRequest(
    val mediaId: String,
    val audioTrackSelection: PersistedTrackSelection?,
    val subtitleTrackSelection: PersistedTrackSelection?,
)

data class PlaybackSessionPlan(
    val queue: QueueBuilder.Queue,
    val resumePositionMs: Long,
    val playbackSpeed: Float,
    val trackSelectionRestoreRequest: TrackSelectionRestoreRequest?,
)

class PlaybackSessionPlanner(
    private val playbackStateRepository: PlaybackStateRepository,
) {
    fun plan(
        targetUri: Uri,
        launchNeighbors: List<Uri>,
        resolvedTitles: Map<Uri, String?> = emptyMap(),
        policy: PlaybackStartupPolicy,
    ): PlaybackSessionPlan {
        val queueUris = QueuePlanner.plan(
            current = targetUri,
            neighbors = launchNeighbors,
        )
        val queue = QueueBuilder.build(
            uris = queueUris,
            startUri = targetUri,
            titleResolver = { uri -> resolvedTitles[uri] },
        )
        val mediaId = targetUri.toString()
        val resumeState = playbackStateRepository.readResumeState(mediaId)

        val restoreRequest = if (policy.rememberTrackSelections) {
            TrackSelectionRestoreRequest(
                mediaId = mediaId,
                audioTrackSelection = resumeState.audioTrackSelection,
                subtitleTrackSelection = resumeState.subtitleTrackSelection,
            ).takeIf { it.audioTrackSelection != null || it.subtitleTrackSelection != null }
        } else {
            null
        }

        return PlaybackSessionPlan(
            queue = queue,
            resumePositionMs = if (policy.resumePlayback) resumeState.positionMs else 0L,
            playbackSpeed = resumeState.speed ?: policy.defaultPlaybackSpeed,
            trackSelectionRestoreRequest = restoreRequest,
        )
    }
}

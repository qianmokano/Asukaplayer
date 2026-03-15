package com.asuka.player.contract

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
    val queue: PlaybackQueue,
    val resumePositionMs: Long,
    val playbackSpeed: Float,
    val trackSelectionRestoreRequest: TrackSelectionRestoreRequest?,
)

class PlaybackSessionPlanner(
    private val resumeStateReader: suspend (String) -> ResumeState,
) {
    constructor(playbackStateRepository: PlaybackStateRepository) : this(
        resumeStateReader = playbackStateRepository::readResumeState,
    )

    suspend fun plan(
        target: PlaybackQueueEntry,
        launchNeighbors: List<PlaybackQueueEntry>,
        resolvedTitles: Map<String, String?> = emptyMap(),
        policy: PlaybackStartupPolicy,
    ): PlaybackSessionPlan {
        val queueEntries = QueuePlanner.plan(
            current = target,
            neighbors = launchNeighbors,
        )
        val queue = QueueBuilder.build(
            entries = queueEntries,
            startMediaId = target.mediaId,
            titleResolver = { entryUri -> resolvedTitles[entryUri] },
        )
        val mediaId = target.mediaId
        val resumeState = resumeStateReader(mediaId)

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

    suspend fun plan(
        targetUri: String,
        launchNeighbors: List<String>,
        resolvedTitles: Map<String, String?> = emptyMap(),
        policy: PlaybackStartupPolicy,
    ): PlaybackSessionPlan {
        return plan(
            target = PlaybackQueueEntry(mediaId = targetUri.toString(), uri = targetUri),
            launchNeighbors = launchNeighbors.map { uri ->
                PlaybackQueueEntry(mediaId = uri.toString(), uri = uri)
            },
            resolvedTitles = resolvedTitles,
            policy = policy,
        )
    }
}

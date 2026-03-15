package com.asuka.player.contract

data class PlaybackSessionRequest(
    val queueEntries: List<PlaybackQueueEntry>,
    val startIndex: Int,
    val playbackUri: String,
    val requestId: Long = 0L,
    val startupPolicy: PlaybackStartupPolicy? = null,
    val autoplay: Boolean? = null,
) {
    init {
        require(queueEntries.isNotEmpty()) { "queueEntries must not be empty" }
        require(startIndex in queueEntries.indices) { "startIndex must point to a queue entry" }
        require(playbackUri.isNotBlank()) { "playbackUri must not be blank" }
    }

    val targetEntry: PlaybackQueueEntry
        get() = queueEntries[startIndex]

    val originalUri: String
        get() = targetEntry.uri

    fun withPlaybackUri(uri: String): PlaybackSessionRequest {
        return copy(playbackUri = uri)
    }

    fun withRequestId(id: Long): PlaybackSessionRequest {
        return copy(requestId = id)
    }

    fun withRuntimePolicy(
        policy: PlaybackStartupPolicy,
        autoplay: Boolean,
    ): PlaybackSessionRequest {
        return copy(
            startupPolicy = policy,
            autoplay = autoplay,
        )
    }
}

package com.asuka.player.contract

/**
 * Queue planning: preserve an explicit queue when available; otherwise stay on the current item.
 *
 * History is intentionally not used as a fallback queue. Silently restoring recently-played
 * files as prev/next neighbors would surprise users who open a single file via a file manager
 * or share intent. If the caller wants a history-based queue it must pass those URIs explicitly
 * as [neighbors].
 */
object QueuePlanner {
    fun plan(
        current: PlaybackQueueEntry,
        neighbors: List<PlaybackQueueEntry> = emptyList(),
    ): List<PlaybackQueueEntry> {
        if (neighbors.isNotEmpty()) {
            val explicitQueue = neighbors.distinctBy(PlaybackQueueEntry::mediaId)
            return if (explicitQueue.any { it.mediaId == current.mediaId }) {
                explicitQueue
            } else {
                listOf(current) + explicitQueue
            }
        }
        return listOf(current)
    }

    fun plan(current: String, neighbors: List<String> = emptyList()): List<String> {
        return plan(
            current = PlaybackQueueEntry(mediaId = current.toString(), uri = current),
            neighbors = neighbors.map { uri -> PlaybackQueueEntry(mediaId = uri.toString(), uri = uri) },
        ).map(PlaybackQueueEntry::uri)
    }
}

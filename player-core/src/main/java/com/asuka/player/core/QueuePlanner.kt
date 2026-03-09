package com.asuka.player.core

import android.net.Uri

/**
 * Queue planning: preserve an explicit queue when available; otherwise stay on the current item.
 *
 * History is intentionally not used as a fallback queue. Silently restoring recently-played
 * files as prev/next neighbors would surprise users who open a single file via a file manager
 * or share intent. If the caller wants a history-based queue it must pass those URIs explicitly
 * as [neighbors].
 */
object QueuePlanner {
    fun plan(current: Uri, neighbors: List<Uri> = emptyList()): List<Uri> {
        if (neighbors.isNotEmpty()) {
            // When the caller provides an explicit queue (e.g. ClipData order), preserve it
            // verbatim if it already contains the current item. Otherwise prepend the current
            // item and keep the remaining order as provided.
            val explicitQueue = neighbors.distinct()
            return if (current in explicitQueue) {
                explicitQueue
            } else {
                listOf(current) + explicitQueue
            }
        }
        return listOf(current)
    }
}

package com.asuka.player.core

import android.net.Uri

/**
 * Queue planning: sort neighbors, prepend current, and optionally include history.
 */
object QueuePlanner {
    fun plan(current: Uri, neighbors: List<Uri> = emptyList(), history: List<Uri> = emptyList()): List<Uri> {
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
        val base = listOf(current)
        val baseSet = base.toHashSet()
        val remainingHistory = history.filter { it !in baseSet }
        return base + remainingHistory
    }
}

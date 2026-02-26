package com.asuka.player.core

import android.net.Uri

/**
 * Queue planning: sort neighbors, prepend current, and optionally include history.
 */
object QueuePlanner {
    fun plan(current: Uri, neighbors: List<Uri> = emptyList(), history: List<Uri> = emptyList()): List<Uri> {
        val sortedNeighbors = neighbors.sortedBy { it.lastPathSegment ?: it.toString() }
        val base = (listOf(current) + sortedNeighbors).distinct()
        val baseSet = base.toHashSet()
        val remainingHistory = history.filter { it !in baseSet }
        return base + remainingHistory
    }
}

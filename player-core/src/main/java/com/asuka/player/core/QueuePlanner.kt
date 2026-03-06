package com.asuka.player.core

import android.net.Uri

/**
 * Queue planning: sort neighbors, prepend current, and optionally include history.
 */
object QueuePlanner {
    fun plan(current: Uri, neighbors: List<Uri> = emptyList(), history: List<Uri> = emptyList()): List<Uri> {
        // When the caller provides an explicit queue (e.g. ClipData order), preserve it verbatim.
        // `distinct()` keeps the first occurrence so the current item stays at index 0.
        val base = (listOf(current) + neighbors).distinct()
        if (neighbors.isNotEmpty()) return base
        val baseSet = base.toHashSet()
        val remainingHistory = history.filter { it !in baseSet }
        return base + remainingHistory
    }
}

package com.asuka.player.core

import android.net.Uri

/**
 * Simple in-memory history for playlist navigation.
 *
 * **Thread safety:** All methods must be called from the main thread.
 * The internal [ArrayDeque] is not synchronized.
 */
class QueueHistoryStore(private val maxSize: Int = 50) {
    private val deque = ArrayDeque<Uri>()

    fun push(uri: Uri) {
        if (deque.lastOrNull() == uri) return
        deque.addLast(uri)
        while (deque.size > maxSize) {
            deque.removeFirst()
        }
    }

    fun items(): List<Uri> = deque.toList()
}

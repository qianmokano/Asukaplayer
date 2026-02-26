package com.asuka.player.core

import android.net.Uri
import java.util.ArrayDeque

/**
 * Simple in-memory history for playlist navigation.
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

package com.asuka.player.data

import android.content.Context
import android.net.Uri
import com.asuka.player.core.QueueHistoryStore

/**
 * In-memory history implementation used by tests and lightweight callers.
 */
class InMemoryQueueHistoryStore(private val maxSize: Int = 50) : QueueHistoryStore {
    private val lock = Any()
    private val deque = ArrayDeque<Uri>()

    override fun push(uri: Uri) {
        synchronized(lock) {
            if (deque.lastOrNull() == uri) return
            deque.addLast(uri)
            while (deque.size > maxSize) {
                deque.removeFirst()
            }
        }
    }

    override fun items(): List<Uri> {
        synchronized(lock) {
            return deque.toList()
        }
    }
}

/**
 * SharedPreferences-backed history so queue continuation survives process death.
 */
class SharedPreferencesQueueHistoryStore(
    context: Context,
    private val preferencesName: String = DEFAULT_PREFS_NAME,
    private val maxSize: Int = 50,
) : QueueHistoryStore {
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private val lock = Any()
    private var cachedItems: MutableList<Uri>? = null

    override fun push(uri: Uri) {
        synchronized(lock) {
            val items = getOrLoadItemsLocked()
            if (items.lastOrNull() == uri) return
            items.add(uri)
            while (items.size > maxSize) {
                items.removeAt(0)
            }
            prefs.edit()
                .putString(
                    KEY_HISTORY,
                    SharedPreferencesPlaybackStore.MediaIdListCodec.encode(items.map(Uri::toString)),
                )
                .apply()
        }
    }

    override fun items(): List<Uri> {
        synchronized(lock) {
            return getOrLoadItemsLocked().toList()
        }
    }

    private fun getOrLoadItemsLocked(): MutableList<Uri> {
        cachedItems?.let { return it }
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        val parsed = SharedPreferencesPlaybackStore.MediaIdListCodec.decode(raw)
            .map(Uri::parse)
            .toMutableList()
        cachedItems = parsed
        return parsed
    }

    private companion object {
        const val DEFAULT_PREFS_NAME = "asuka_queue_history"
        const val KEY_HISTORY = "queue_history_media_ids"
    }
}

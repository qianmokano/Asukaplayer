package com.asuka.player.data

import android.content.Context
import com.asuka.player.contract.QueueHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-memory history implementation used by tests and lightweight callers.
 */
class InMemoryQueueHistoryStore(private val maxSize: Int = 50) : QueueHistoryStore {
    private val lock = Any()
    private val deque = ArrayDeque<String>()

    override suspend fun push(mediaId: String) {
        synchronized(lock) {
            if (deque.lastOrNull() == mediaId) return
            deque.addLast(mediaId)
            while (deque.size > maxSize) {
                deque.removeFirst()
            }
        }
    }

    override suspend fun items(): List<String> {
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
    private var cachedItems: MutableList<String>? = null

    override suspend fun push(mediaId: String) {
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val items = getOrLoadItemsLocked()
                if (items.lastOrNull() == mediaId) return@synchronized
                items.add(mediaId)
                while (items.size > maxSize) {
                    items.removeAt(0)
                }
                prefs.edit()
                    .putString(
                        KEY_HISTORY,
                        SharedPreferencesPlaybackStore.MediaIdListCodec.encode(items),
                    )
                    .apply()
            }
        }
    }

    override suspend fun items(): List<String> {
        return withContext(Dispatchers.IO) {
            synchronized(lock) {
                getOrLoadItemsLocked().toList()
            }
        }
    }

    private fun getOrLoadItemsLocked(): MutableList<String> {
        cachedItems?.let { return it }
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        val parsed = SharedPreferencesPlaybackStore.MediaIdListCodec.decode(raw)
            .toMutableList()
        cachedItems = parsed
        return parsed
    }

    private companion object {
        const val DEFAULT_PREFS_NAME = "asuka_queue_history"
        const val KEY_HISTORY = "queue_history_media_ids"
    }
}

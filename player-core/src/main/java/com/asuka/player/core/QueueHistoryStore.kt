package com.asuka.player.core

import android.content.Context
import android.net.Uri

/**
 * History of played media used to seed new playback sessions.
 */
interface QueueHistoryStore {
    fun push(uri: Uri)
    fun items(): List<Uri>
}

/**
 * In-memory history implementation used by tests and lightweight callers.
 *
 * **Thread safety:** All methods must be called from the main thread.
 * The internal [ArrayDeque] is not synchronized.
 */
class InMemoryQueueHistoryStore(private val maxSize: Int = 50) : QueueHistoryStore {
    private val deque = ArrayDeque<Uri>()

    override fun push(uri: Uri) {
        if (deque.lastOrNull() == uri) return
        deque.addLast(uri)
        while (deque.size > maxSize) {
            deque.removeFirst()
        }
    }

    override fun items(): List<Uri> = deque.toList()
}

/**
 * SharedPreferences-backed history so queue continuation survives process death.
 *
 * **Thread safety:** All methods must be called from the main thread.
 */
class SharedPreferencesQueueHistoryStore(
    context: Context,
    private val preferencesName: String = DEFAULT_PREFS_NAME,
    private val maxSize: Int = 50,
) : QueueHistoryStore {
    private val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    private var cachedItems: MutableList<Uri>? = null

    override fun push(uri: Uri) {
        val items = getOrLoadItems()
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

    override fun items(): List<Uri> = getOrLoadItems().toList()

    private fun getOrLoadItems(): MutableList<Uri> {
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

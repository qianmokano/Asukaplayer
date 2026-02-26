package com.asuka.player.core

import android.content.Context
import com.asuka.player.data.PlaybackStore

/**
 * Provides the app-wide PlaybackStore and QueueHistoryStore.
 * Call init(context) once in both PlaybackActivity.onCreate and PlaybackService.onCreate.
 */
object PlaybackStoreProvider {
    @Volatile private var _store: PlaybackStore? = null

    val store: PlaybackStore
        get() = checkNotNull(_store) {
            "PlaybackStoreProvider not initialized — call init(context) in onCreate"
        }

    val history: QueueHistoryStore by lazy { QueueHistoryStore() }

    fun init(context: Context) {
        if (_store != null) return
        synchronized(this) {
            if (_store == null) {
                _store = SharedPreferencesPlaybackStore(context.applicationContext)
            }
        }
    }
}

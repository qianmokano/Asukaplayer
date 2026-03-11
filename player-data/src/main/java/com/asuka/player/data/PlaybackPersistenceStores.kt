package com.asuka.player.data

import android.content.Context
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.QueueHistoryStore

data class PlaybackPersistenceStores(
    val playbackStore: PlaybackStore,
    val queueHistoryStore: QueueHistoryStore,
)

object PlaybackPersistenceStoresFactory {
    fun create(context: Context): PlaybackPersistenceStores {
        val legacyPlaybackStore = SharedPreferencesPlaybackStore(context)
        val legacyQueueHistoryStore = SharedPreferencesQueueHistoryStore(context)
        val database = AsukaPlaybackRoomDatabase.open(
            context = context,
            legacyPlaybackStore = legacyPlaybackStore,
            legacyQueueHistoryStore = legacyQueueHistoryStore,
        )
        return PlaybackPersistenceStores(
            playbackStore = RoomPlaybackStore(database.playbackStateDao()),
            queueHistoryStore = RoomQueueHistoryStore(database.queueHistoryDao()),
        )
    }
}

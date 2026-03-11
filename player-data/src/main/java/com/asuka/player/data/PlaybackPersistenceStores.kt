package com.asuka.player.data

import android.content.Context
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.QueueHistoryStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PlaybackPersistenceStores(
    val playbackStore: PlaybackStore,
    val queueHistoryStore: QueueHistoryStore,
)

object PlaybackPersistenceStoresFactory {
    suspend fun create(context: Context): PlaybackPersistenceStores {
        return withContext(Dispatchers.IO) {
            val legacyPlaybackStore = SharedPreferencesPlaybackStore(context)
            val legacyQueueHistoryStore = SharedPreferencesQueueHistoryStore(context)
            val database = AsukaPlaybackRoomDatabase.open(
                context = context,
            )
            database.importLegacyDataIfNeeded(
                legacyPlaybackStore = legacyPlaybackStore,
                legacyQueueHistoryStore = legacyQueueHistoryStore,
            )
            PlaybackPersistenceStores(
                playbackStore = RoomPlaybackStore(database.playbackStateDao()),
                queueHistoryStore = RoomQueueHistoryStore(database.queueHistoryDao()),
            )
        }
    }
}

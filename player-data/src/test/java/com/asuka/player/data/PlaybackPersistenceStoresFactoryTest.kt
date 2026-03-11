package com.asuka.player.data

import android.content.Context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlaybackPersistenceStoresFactoryTest {

    @Test
    fun create_importsLegacySharedPreferencesIntoRoomStores() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        clearPersistence(context)

        SharedPreferencesPlaybackStore(context).apply {
            savePosition("media://one", 123L)
            savePlaybackSpeed("media://one", 1.25f)
            saveAudioTrackId("media://one", "audio-main")
            saveZoom("media://one", 2.0f)
            savePosition("media://two", 456L)
        }
        SharedPreferencesQueueHistoryStore(context).apply {
            push("media://one")
            push("media://two")
        }

        val stores = runBlocking(Dispatchers.IO) {
            PlaybackPersistenceStoresFactory.create(context)
        }

        assertEquals(123L, stores.playbackStore.loadPosition("media://one"))
        assertEquals(1.25f, stores.playbackStore.loadPlaybackSpeed("media://one"))
        assertEquals("audio-main", stores.playbackStore.loadAudioTrackId("media://one"))
        assertEquals(2.0f, stores.playbackStore.loadZoom("media://one"))
        assertEquals(listOf("media://two", "media://one"), stores.playbackStore.recentMediaIds(limit = 10))
        assertEquals(listOf("media://one", "media://two"), stores.queueHistoryStore.items())
    }

    private fun clearPersistence(context: Context) {
        context.getSharedPreferences("asuka_playback_state", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("asuka_queue_history", Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteDatabase(AsukaPlaybackRoomDatabase.DB_NAME)
    }
}

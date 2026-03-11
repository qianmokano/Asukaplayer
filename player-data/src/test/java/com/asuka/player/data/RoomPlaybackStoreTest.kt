package com.asuka.player.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomPlaybackStoreTest {

    @Test
    fun saveAndLoad_roundTrip() = runBlocking {
        val db = AsukaPlaybackRoomDatabase.inMemory(RuntimeEnvironment.getApplication())
        try {
            val store = RoomPlaybackStore(db.playbackStateDao(), nowMs = FixedTimeSource())

            store.savePosition("media://one", 1234L)
            store.savePlaybackSpeed("media://one", 1.5f)
            store.saveAudioTrackId("media://one", "audio-main")
            store.saveSubtitleTrackId("media://one", "subtitle-main")
            store.saveZoom("media://one", 2.25f)

            assertEquals(1234L, store.loadPosition("media://one"))
            assertEquals(1.5f, store.loadPlaybackSpeed("media://one"))
            assertEquals("audio-main", store.loadAudioTrackId("media://one"))
            assertEquals("subtitle-main", store.loadSubtitleTrackId("media://one"))
            assertEquals(2.25f, store.loadZoom("media://one"))
        } finally {
            db.close()
        }
    }

    @Test
    fun recentMediaIds_ordersByLastTouch_andPrunes() = runBlocking {
        val db = AsukaPlaybackRoomDatabase.inMemory(RuntimeEnvironment.getApplication())
        try {
            val store = RoomPlaybackStore(
                playbackStateDao = db.playbackStateDao(),
                maxEntries = 3,
                nowMs = FixedTimeSource(),
            )

            store.savePosition("media://a", 1L)
            store.savePlaybackSpeed("media://b", 1.0f)
            store.saveZoom("media://c", 2.0f)
            store.savePosition("media://d", 4L)

            assertEquals(
                listOf("media://d", "media://c", "media://b"),
                store.recentMediaIds(limit = 10),
            )
            assertNull(store.loadPosition("media://a"))
            assertEquals(listOf("media://d", "media://c"), store.recentMediaIds(limit = 2))
        } finally {
            db.close()
        }
    }

    private class FixedTimeSource : () -> Long {
        private var current = 1_000L

        override fun invoke(): Long = current++
    }
}

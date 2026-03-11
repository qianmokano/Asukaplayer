package com.asuka.player.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RoomQueueHistoryStoreTest {

    @Test
    fun push_deduplicatesConsecutive_andRespectsMaxSize() {
        val db = AsukaPlaybackRoomDatabase.inMemory(RuntimeEnvironment.getApplication())
        try {
            val store = RoomQueueHistoryStore(
                queueHistoryDao = db.queueHistoryDao(),
                maxSize = 3,
            )

            store.push("media://a")
            store.push("media://a")
            store.push("media://b")
            store.push("media://c")
            store.push("media://d")

            assertEquals(
                listOf("media://b", "media://c", "media://d"),
                store.items(),
            )
        } finally {
            db.close()
        }
    }
}

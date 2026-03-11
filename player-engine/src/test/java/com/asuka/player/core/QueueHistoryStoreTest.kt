package com.asuka.player.core

import android.net.Uri
import com.asuka.player.data.InMemoryQueueHistoryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueHistoryStoreTest {

    @Test
    fun push_deduplicatesConsecutive() = runBlocking {
        val store = InMemoryQueueHistoryStore(maxSize = 3)
        val a = Uri.parse("file:///a.mp4")
        store.push(a.toString())
        store.push(a.toString())
        assertEquals(listOf(a.toString()), store.items())
    }

    @Test
    fun push_respectsMaxSize() = runBlocking {
        val store = InMemoryQueueHistoryStore(maxSize = 2)
        store.push("file:///1.mp4")
        store.push("file:///2.mp4")
        store.push("file:///3.mp4")
        assertEquals(2, store.items().size)
        assertEquals("file:///2.mp4", store.items().first())
    }
}

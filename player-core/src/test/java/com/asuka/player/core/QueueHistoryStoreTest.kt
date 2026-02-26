package com.asuka.player.core

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueHistoryStoreTest {

    @Test
    fun push_deduplicatesConsecutive() {
        val store = QueueHistoryStore(maxSize = 3)
        val a = Uri.parse("file:///a.mp4")
        store.push(a)
        store.push(a)
        assertEquals(listOf(a), store.items())
    }

    @Test
    fun push_respectsMaxSize() {
        val store = QueueHistoryStore(maxSize = 2)
        store.push(Uri.parse("file:///1.mp4"))
        store.push(Uri.parse("file:///2.mp4"))
        store.push(Uri.parse("file:///3.mp4"))
        assertEquals(2, store.items().size)
        assertEquals(Uri.parse("file:///2.mp4"), store.items().first())
    }
}

package com.asuka.player.engine

import android.net.Uri
import androidx.media3.common.MediaItem
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.data.InMemoryQueueHistoryStore
import com.asuka.player.platform.QueueHistoryWriter
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueHistoryWriterTest {

    @Test
    fun onMediaItemTransition_prefersStableMediaIdUri_forHistory() = runBlocking {
        val original = Uri.parse("content://videos/original.mp4")
        val fallback = Uri.parse("file:///cache/fallback.mp4")
        val store = InMemoryQueueHistoryStore()
        val writer = QueueHistoryWriter(store)

        writer.onMediaItemTransition(
            MediaItem.Builder()
                .setMediaId(original.toString())
                .setUri(fallback)
                .build(),
            0,
        )
        writer.awaitIdle()

        assertEquals(listOf(original.toString()), store.items())
    }

    @Test
    fun onMediaItemTransition_preservesOpaqueStableMediaId_forHistory() = runBlocking {
        val stableMediaId = "media-store:42"
        val fallback = Uri.parse("content://videos/original.mp4")
        val store = InMemoryQueueHistoryStore()
        val writer = QueueHistoryWriter(store)

        writer.onMediaItemTransition(
            MediaItem.Builder()
                .setMediaId(stableMediaId)
                .setUri(fallback)
                .build(),
            0,
        )
        writer.awaitIdle()

        assertEquals(listOf(stableMediaId), store.items())
    }

    @Test
    fun onMediaItemTransition_returnsQuickly_whenHistoryStoreIsSlow() = runBlocking {
        val store = SlowQueueHistoryStore()
        val writer = QueueHistoryWriter(store)

        val elapsedMs = measureTimeMillis {
            writer.onMediaItemTransition(
                MediaItem.Builder()
                    .setMediaId("media-store:42")
                    .setUri(Uri.parse("content://videos/original.mp4"))
                    .build(),
                0,
            )
        }
        writer.awaitIdle()

        assertTrue(elapsedMs < 100, "Expected non-blocking callback, but took ${elapsedMs}ms")
    }
}

private class SlowQueueHistoryStore : QueueHistoryStore {
    override suspend fun push(mediaId: String) {
        delay(200)
    }

    override suspend fun items(): List<String> = emptyList()
}

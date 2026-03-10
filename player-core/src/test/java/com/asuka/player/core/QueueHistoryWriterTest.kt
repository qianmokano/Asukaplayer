package com.asuka.player.core

import android.net.Uri
import androidx.media3.common.MediaItem
import com.asuka.player.data.InMemoryQueueHistoryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueHistoryWriterTest {

    @Test
    fun onMediaItemTransition_prefersStableMediaIdUri_forHistory() {
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

        assertEquals(listOf(original), store.items())
    }
}

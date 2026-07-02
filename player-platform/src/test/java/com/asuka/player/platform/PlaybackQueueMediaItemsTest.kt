package com.asuka.player.platform

import com.asuka.player.contract.PlaybackQueue
import com.asuka.player.contract.PlaybackQueueItem
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackQueueMediaItemsTest {
    @Test
    fun toMediaItems_marksTemporaryContentGrantItemsTransient() {
        val queue = PlaybackQueue(
            items = listOf(
                PlaybackQueueItem(
                    mediaId = "content://videos/shared.mp4",
                    uri = "content://videos/shared.mp4",
                    title = "shared.mp4",
                    persistable = false,
                    readableInSession = true,
                ),
            ),
            startIndex = 0,
        )

        val item = queue.toMediaItems().single()

        assertEquals("transient:content://videos/shared.mp4", item.mediaId)
    }

    @Test
    fun toMediaItems_marksUnreadableItemsTransient() {
        val queue = PlaybackQueue(
            items = listOf(
                PlaybackQueueItem(
                    mediaId = "content://videos/shared.mp4",
                    uri = "content://videos/shared.mp4",
                    title = "shared.mp4",
                    persistable = false,
                    readableInSession = false,
                ),
            ),
            startIndex = 0,
        )

        val item = queue.toMediaItems().single()

        assertEquals("transient:content://videos/shared.mp4", item.mediaId)
    }

    @Test
    fun toMediaItems_preservesStableMediaIdForPersistableContentItems() {
        val queue = PlaybackQueue(
            items = listOf(
                PlaybackQueueItem(
                    mediaId = "content://videos/persisted.mp4",
                    uri = "content://videos/persisted.mp4",
                    title = "persisted.mp4",
                    persistable = true,
                    readableInSession = true,
                ),
            ),
            startIndex = 0,
        )

        val item = queue.toMediaItems().single()

        assertEquals("content://videos/persisted.mp4", item.mediaId)
    }

    @Test
    fun toMediaItems_preservesStableMediaIdForNetworkItems() {
        val queue = PlaybackQueue(
            items = listOf(
                PlaybackQueueItem(
                    mediaId = "https://example.com/video.mp4",
                    uri = "https://example.com/video.mp4",
                    title = "video.mp4",
                    persistable = false,
                    readableInSession = true,
                ),
            ),
            startIndex = 0,
        )

        val item = queue.toMediaItems().single()

        assertEquals("https://example.com/video.mp4", item.mediaId)
    }
}

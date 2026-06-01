package com.asuka.player.contract

import kotlin.test.Test
import kotlin.test.assertEquals

class QueueBuilderTest {
    @Test
    fun build_preservesStableMediaId_whenProvided() {
        val original = "content://videos/original.mp4"
        val fallback = "file:///cache/fallback.mp4"
        val queue = QueueBuilder.build(
            entries = listOf(
                PlaybackQueueEntry(
                    mediaId = original,
                    uri = fallback,
                ),
            ),
            startMediaId = original,
        ) { "My Video" }

        assertEquals(original, queue.items.single().mediaId)
        assertEquals(fallback, queue.items.single().uri)
    }

    @Test
    fun build_usesTitleResolver_whenProvided() {
        val uri = "content://media/external/video/media/42"
        val queue = QueueBuilder.buildFromUris(listOf(uri), startUri = uri) { "My Video" }
        assertEquals("My Video", queue.items.single().title)
    }

    @Test
    fun build_fallsBackToLastPathSegment_whenResolverBlank() {
        val uri = "file:///storage/emulated/0/Movies/sample.mp4"
        val queue = QueueBuilder.buildFromUris(listOf(uri), startUri = uri) { "   " }
        assertEquals("sample.mp4", queue.items.single().title)
    }

    @Test
    fun build_fallsBackToUriString_whenNoLastPathSegment() {
        val uri = "customscheme:"
        val queue = QueueBuilder.buildFromUris(listOf(uri), startUri = uri) { null }
        assertEquals(uri, queue.items.single().title)
    }

    @Test
    fun build_preservesSessionReadabilitySeparatelyFromPersistability() {
        val queue = QueueBuilder.build(
            entries = listOf(
                PlaybackQueueEntry(
                    mediaId = "content://videos/shared.mp4",
                    uri = "content://videos/shared.mp4",
                    persistable = false,
                    readableInSession = true,
                ),
            ),
            startMediaId = "content://videos/shared.mp4",
        )

        assertEquals(false, queue.items.single().persistable)
        assertEquals(true, queue.items.single().readableInSession)
    }
}

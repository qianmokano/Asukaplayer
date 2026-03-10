package com.asuka.player.core

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueueBuilderTest {
    @Test
    fun build_preservesStableMediaId_whenProvided() {
        val original = Uri.parse("content://videos/original.mp4")
        val fallback = Uri.parse("file:///cache/fallback.mp4")
        val queue = QueueBuilder.build(
            entries = listOf(
                PlaybackQueueEntry(
                    mediaId = original.toString(),
                    uri = fallback,
                ),
            ),
            startMediaId = original.toString(),
        ) { "My Video" }

        assertEquals(original.toString(), queue.items.single().mediaId)
        assertEquals(fallback, queue.items.single().localConfiguration?.uri)
    }

    @Test
    fun build_usesTitleResolver_whenProvided() {
        val uri = Uri.parse("content://media/external/video/media/42")
        val queue = QueueBuilder.build(listOf(uri), startUri = uri) { "My Video" }
        assertEquals("My Video", queue.items.single().mediaMetadata.title?.toString())
    }

    @Test
    fun build_fallsBackToLastPathSegment_whenResolverBlank() {
        val uri = Uri.parse("file:///storage/emulated/0/Movies/sample.mp4")
        val queue = QueueBuilder.build(listOf(uri), startUri = uri) { "   " }
        assertEquals("sample.mp4", queue.items.single().mediaMetadata.title?.toString())
    }

    @Test
    fun build_fallsBackToUriString_whenNoLastPathSegment() {
        val uri = Uri.parse("customscheme:")
        val queue = QueueBuilder.build(listOf(uri), startUri = uri) { null }
        assertEquals(uri.toString(), queue.items.single().mediaMetadata.title?.toString())
    }
}

package com.asuka.player.core

import android.net.Uri
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.QueueBuilder
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
                    uri = fallback.toString(),
                ),
            ),
            startMediaId = original.toString(),
        ) { "My Video" }

        assertEquals(original.toString(), queue.items.single().mediaId)
        assertEquals(fallback.toString(), queue.items.single().uri)
    }

    @Test
    fun build_usesTitleResolver_whenProvided() {
        val uri = Uri.parse("content://media/external/video/media/42")
        val queue = QueueBuilder.buildFromUris(listOf(uri.toString()), startUri = uri.toString()) { "My Video" }
        assertEquals("My Video", queue.items.single().title)
    }

    @Test
    fun build_fallsBackToLastPathSegment_whenResolverBlank() {
        val uri = Uri.parse("file:///storage/emulated/0/Movies/sample.mp4")
        val queue = QueueBuilder.buildFromUris(listOf(uri.toString()), startUri = uri.toString()) { "   " }
        assertEquals("sample.mp4", queue.items.single().title)
    }

    @Test
    fun build_fallsBackToUriString_whenNoLastPathSegment() {
        val uri = Uri.parse("customscheme:")
        val queue = QueueBuilder.buildFromUris(listOf(uri.toString()), startUri = uri.toString()) { null }
        assertEquals(uri.toString(), queue.items.single().title)
    }
}

package com.asuka.player.core

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentQueueReaderTest {

    @Test
    fun read_prefersClipDataOrder_whenQueueIsPresent() {
        val current = Uri.parse("file:///queue/current-fallback.mp4")
        val previous = Uri.parse("file:///queue/previous.mp4")
        val next = Uri.parse("file:///queue/next.mp4")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = current
            clipData = ClipData.newRawUri("queue", previous).apply {
                addItem(ClipData.Item(current))
                addItem(ClipData.Item(next))
            }
        }

        val result = IntentQueueReader.read(intent)

        assertEquals(listOf(previous, current, next), result)
    }

    @Test
    fun read_prependsData_whenClipDataDoesNotContainIt() {
        val current = Uri.parse("file:///queue/current.mp4")
        val next = Uri.parse("file:///queue/next.mp4")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = current
            clipData = ClipData.newRawUri("queue", next)
        }

        val result = IntentQueueReader.read(intent)

        assertEquals(listOf(current, next), result)
    }

    @Test
    fun readEntries_prefersStoredStableMediaIds_overRuntimeUris() {
        val original = Uri.parse("content://videos/current.mp4")
        val fallback = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = fallback
            clipData = ClipData.newRawUri("queue", fallback).apply {
                addItem(ClipData.Item(next))
            }
            IntentQueueReader.applyPlaybackIdentity(
                intent = this,
                mediaId = original.toString(),
                queueMediaIds = listOf(original.toString(), next.toString()),
            )
        }

        val result = IntentQueueReader.readEntries(intent)

        assertEquals(
            listOf(
                PlaybackQueueEntry(mediaId = original.toString(), uri = fallback),
                PlaybackQueueEntry(mediaId = next.toString(), uri = next),
            ),
            result,
        )
    }
}

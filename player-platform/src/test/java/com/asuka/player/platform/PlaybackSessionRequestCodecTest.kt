package com.asuka.player.platform

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackSessionRequestCodecTest {

    // -- fromExternalIntent --

    @Test
    fun fromExternalIntent_nullIntent_returnsNull() {
        assertNull(PlaybackSessionRequestCodec.fromExternalIntent(null))
    }

    @Test
    fun fromExternalIntent_emptyIntent_returnsNull() {
        assertNull(PlaybackSessionRequestCodec.fromExternalIntent(Intent()))
    }

    @Test
    fun fromExternalIntent_dataUri_createsRequest() {
        val intent = Intent().apply { data = Uri.parse("content://videos/1.mp4") }
        val request = PlaybackSessionRequestCodec.fromExternalIntent(intent)

        assertNotNull(request)
        assertEquals(1, request.queueEntries.size)
        assertEquals("content://videos/1.mp4", request.playbackUri)
        assertEquals(0, request.startIndex)
    }

    @Test
    fun fromExternalIntent_clipData_createsQueueFromClipItems() {
        val clip = ClipData.newRawUri("queue", Uri.parse("content://videos/1.mp4")).apply {
            addItem(ClipData.Item(Uri.parse("content://videos/2.mp4")))
            addItem(ClipData.Item(Uri.parse("content://videos/3.mp4")))
        }
        val intent = Intent().apply {
            data = Uri.parse("content://videos/2.mp4")
            clipData = clip
        }
        val request = PlaybackSessionRequestCodec.fromExternalIntent(intent)

        assertNotNull(request)
        assertEquals(3, request.queueEntries.size)
        assertEquals("content://videos/2.mp4", request.playbackUri)
        assertEquals(1, request.startIndex)
    }

    @Test
    fun fromExternalIntent_clipDataWithDataUri_prependsDataUri() {
        val clip = ClipData.newRawUri("queue", Uri.parse("content://videos/2.mp4"))
        val intent = Intent().apply {
            data = Uri.parse("content://videos/1.mp4")
            clipData = clip
        }
        val request = PlaybackSessionRequestCodec.fromExternalIntent(intent)

        assertNotNull(request)
        assertEquals("content://videos/1.mp4", request.playbackUri)
        assertEquals(0, request.startIndex)
        assertEquals("content://videos/1.mp4", request.queueEntries[0].uri)
    }

    // -- fromSelection --

    @Test
    fun fromSelection_createsRequestWithCorrectStartIndex() {
        val request = PlaybackSessionRequestCodec.fromSelection(
            targetMediaId = "media-2",
            queueMediaIds = listOf("media-1", "media-2", "media-3"),
        )

        assertEquals(3, request.queueEntries.size)
        assertEquals(1, request.startIndex)
        assertEquals("media-2", request.playbackUri)
    }

    @Test
    fun fromSelection_targetNotInQueue_prependsIt() {
        val request = PlaybackSessionRequestCodec.fromSelection(
            targetMediaId = "media-new",
            queueMediaIds = listOf("media-1", "media-2"),
        )

        assertEquals(3, request.queueEntries.size)
        assertEquals(0, request.startIndex)
        assertEquals("media-new", request.queueEntries[0].mediaId)
    }

    // -- fromQueueEntries --

    @Test
    fun fromQueueEntries_deduplicatesAndFilters() {
        val target = PlaybackQueueEntry(mediaId = "a", uri = "uri-a")
        val queue = listOf(
            PlaybackQueueEntry(mediaId = "a", uri = "uri-a"),
            PlaybackQueueEntry(mediaId = "b", uri = "uri-b"),
            PlaybackQueueEntry(mediaId = "a", uri = "uri-a-dup"), // duplicate mediaId
            PlaybackQueueEntry(mediaId = "", uri = "uri-blank"),   // blank mediaId
        )

        val request = PlaybackSessionRequestCodec.fromQueueEntries(target, queue)

        assertEquals(2, request.queueEntries.size)
        assertEquals(0, request.startIndex)
        assertEquals("uri-a", request.playbackUri)
    }

    @Test
    fun fromQueueEntries_targetNotInQueue_prependsIt() {
        val target = PlaybackQueueEntry(mediaId = "new", uri = "uri-new")
        val queue = listOf(
            PlaybackQueueEntry(mediaId = "a", uri = "uri-a"),
            PlaybackQueueEntry(mediaId = "b", uri = "uri-b"),
        )

        val request = PlaybackSessionRequestCodec.fromQueueEntries(target, queue)

        assertEquals(3, request.queueEntries.size)
        assertEquals(0, request.startIndex)
        assertEquals("new", request.queueEntries[0].mediaId)
    }

    // -- round-trip: applyPlaybackRequest + readPlaybackRequest --

    @Test
    fun roundTrip_applyAndRead_preservesRequest() {
        val original = PlaybackSessionRequest(
            queueEntries = listOf(
                PlaybackQueueEntry(mediaId = "id-1", uri = "uri-1"),
                PlaybackQueueEntry(mediaId = "id-2", uri = "uri-2"),
                PlaybackQueueEntry(mediaId = "id-3", uri = "uri-3"),
            ),
            startIndex = 1,
            playbackUri = "uri-2",
        )

        val intent = Intent()
        PlaybackSessionRequestCodec.applyPlaybackRequest(intent, original)
        val restored = PlaybackSessionRequestCodec.readPlaybackRequest(intent)

        assertNotNull(restored)
        assertEquals(original.queueEntries, restored.queueEntries)
        assertEquals(original.startIndex, restored.startIndex)
        assertEquals(original.playbackUri, restored.playbackUri)
    }

    // -- readPlaybackRequest --

    @Test
    fun readPlaybackRequest_nullIntent_returnsNull() {
        assertNull(PlaybackSessionRequestCodec.readPlaybackRequest(null))
    }

    @Test
    fun readPlaybackRequest_emptyIntent_returnsNull() {
        assertNull(PlaybackSessionRequestCodec.readPlaybackRequest(Intent()))
    }

    // -- buildClipData --

    @Test
    fun buildClipData_includesAllQueueEntries() {
        val request = PlaybackSessionRequest(
            queueEntries = listOf(
                PlaybackQueueEntry(mediaId = "a", uri = "content://a"),
                PlaybackQueueEntry(mediaId = "b", uri = "content://b"),
                PlaybackQueueEntry(mediaId = "c", uri = "content://c"),
            ),
            startIndex = 0,
            playbackUri = "content://a",
        )

        val clipData = PlaybackSessionRequestCodec.buildClipData(request)

        assertEquals(3, clipData.itemCount)
        assertEquals("content://a", clipData.getItemAt(0).uri.toString())
        assertEquals("content://b", clipData.getItemAt(1).uri.toString())
        assertEquals("content://c", clipData.getItemAt(2).uri.toString())
    }

    // -- remapPlaybackUri --

    @Test
    fun remapPlaybackUri_replacesPlaybackUriOnly() {
        val original = PlaybackSessionRequest(
            queueEntries = listOf(PlaybackQueueEntry(mediaId = "a", uri = "content://original")),
            startIndex = 0,
            playbackUri = "content://original",
        )

        val remapped = PlaybackSessionRequestCodec.remapPlaybackUri(original, Uri.parse("content://remapped"))

        assertEquals("content://remapped", remapped.playbackUri)
        assertEquals("content://original", remapped.queueEntries[0].uri)
    }

    // -- readRuntimeQueueUris --

    @Test
    fun readRuntimeQueueUris_fromClipData() {
        val clip = ClipData.newRawUri("queue", Uri.parse("content://1")).apply {
            addItem(ClipData.Item(Uri.parse("content://2")))
        }
        val intent = Intent().apply {
            data = Uri.parse("content://1")
            clipData = clip
        }

        val uris = PlaybackSessionRequestCodec.readRuntimeQueueUris(intent)
        assertEquals(listOf("content://1", "content://2"), uris)
    }

    @Test
    fun readRuntimeQueueUris_fallsBackToDataUri() {
        val intent = Intent().apply { data = Uri.parse("content://solo") }
        val uris = PlaybackSessionRequestCodec.readRuntimeQueueUris(intent)
        assertEquals(listOf("content://solo"), uris)
    }

    @Test
    fun readRuntimeQueueUris_emptyIntent_returnsEmpty() {
        assertEquals(emptyList(), PlaybackSessionRequestCodec.readRuntimeQueueUris(Intent()))
    }
}

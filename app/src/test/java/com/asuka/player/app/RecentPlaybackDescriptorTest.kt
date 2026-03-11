package com.asuka.player.app

import android.net.Uri
import com.asuka.player.contract.PlaybackQueueEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentPlaybackDescriptorTest {

    @Test
    fun from_knownIndexedVideo_prefersIndexedMetadataAndThumbnail() {
        val known = LocalVideoItem(
            id = 42L,
            uri = Uri.parse("content://videos/42"),
            title = "Known Title",
            durationMs = 65_000L,
            sizeBytes = 2_000L,
            folderName = "Movies",
            folderPath = "/storage/emulated/0/Movies",
            folderId = 7L,
            dateAddedSec = 10L,
        )

        val descriptor = RecentPlaybackDescriptor.from(
            mediaId = known.playbackMediaId,
            knownVideo = known,
            unavailableLabel = "Unavailable",
        )

        assertEquals(known.title, descriptor.fallbackTitle)
        assertEquals(known.folderPath, descriptor.description)
        assertEquals(known.uri, descriptor.thumbnailUri)
        assertEquals(known.id, descriptor.thumbnailId)
        assertEquals(known.durationLabel, descriptor.durationLabel)
        assertFalse(descriptor.shouldResolveDisplayName)
        assertEquals(
            known.toPlaybackQueueEntry(mediaIdOverride = known.playbackMediaId),
            descriptor.targetEntry,
        )
    }

    @Test
    fun from_contentUri_usesUriForThumbnailAndDisplayNameResolution() {
        val mediaId = "content://videos/43"
        val descriptor = RecentPlaybackDescriptor.from(
            mediaId = mediaId,
            knownVideo = null,
            unavailableLabel = "Unavailable",
        )

        assertEquals(Uri.parse(mediaId), descriptor.uri)
        assertEquals(Uri.parse(mediaId), descriptor.thumbnailUri)
        assertNull(descriptor.thumbnailId)
        assertTrue(descriptor.shouldResolveDisplayName)
        assertTrue(descriptor.isPlayable)
        assertEquals(mediaId, descriptor.description)
        assertEquals(
            PlaybackQueueEntry(mediaId = mediaId, uri = mediaId),
            descriptor.targetEntry,
        )
    }

    @Test
    fun from_remoteUri_keepsDescriptionButDisablesThumbnailResolution() {
        val mediaId = "https://example.com/video.mp4"
        val descriptor = RecentPlaybackDescriptor.from(
            mediaId = mediaId,
            knownVideo = null,
            unavailableLabel = "Unavailable",
        )

        assertEquals("video.mp4", descriptor.fallbackTitle)
        assertEquals(mediaId, descriptor.description)
        assertNull(descriptor.thumbnailUri)
        assertFalse(descriptor.shouldResolveDisplayName)
        assertTrue(descriptor.isPlayable)
    }

    @Test
    fun from_nonUriFallback_usesUnavailableDescription() {
        val descriptor = RecentPlaybackDescriptor.from(
            mediaId = "opaque-id",
            knownVideo = null,
            unavailableLabel = "Unavailable",
        )

        assertEquals("opaque-id", descriptor.fallbackTitle)
        assertEquals("Unavailable", descriptor.description)
        assertNull(descriptor.uri)
        assertNull(descriptor.thumbnailUri)
        assertFalse(descriptor.shouldResolveDisplayName)
        assertFalse(descriptor.isPlayable)
    }
}

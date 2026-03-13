package com.asuka.player.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.platform.PlaybackSessionRequestCodec
import com.asuka.player.renderer.activity.PlaybackActivity
import com.asuka.player.runtime.PlaybackLaunchCoordinator
import com.asuka.player.runtime.PlaybackUriResolver
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackLaunchCoordinatorTest {

    @Test
    fun prepareRequest_preservesQueueAndRemapsCurrentItem() {
        val current = Uri.parse("content://videos/current.mp4")
        val resolved = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val coordinator = PlaybackLaunchCoordinator(
            uriResolver = object : PlaybackUriResolver {
                override fun resolveForPlayback(sourceUri: Uri): Uri {
                    assertEquals(current, sourceUri)
                    return resolved
                }
            },
        )
        val sourceIntent = Intent(Intent.ACTION_VIEW).apply {
            data = current
            clipData = ClipData.newRawUri("queue", current).apply {
                addItem(ClipData.Item(next))
            }
        }

        val request = coordinator.prepareRequest(
            request = PlaybackSessionRequestCodec.fromExternalIntent(sourceIntent)!!,
        )

        assertEquals(current.toString(), request.originalUri)
        assertEquals(resolved.toString(), request.playbackUri)

        val playbackIntent = coordinator.createPlaybackIntent(
            context = RuntimeEnvironment.getApplication(),
            activityClass = PlaybackActivity::class.java,
            request = request,
        )

        assertEquals(resolved, playbackIntent.data)
        assertNotNull(playbackIntent.clipData)
        assertTrue((playbackIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals(current.toString(), playbackIntent.getStringExtra("com.asuka.player.extra.MEDIA_ID"))
        val queueMediaIds = playbackIntent
            .getStringArrayListExtra("com.asuka.player.extra.QUEUE_MEDIA_IDS")
            ?.toList()
        assertEquals(
            listOf(current.toString(), next.toString()),
            queueMediaIds,
        )
    }

    @Test
    fun prepareRequest_buildsClipDataFromExplicitQueueWhenNoSourceIntent() {
        val previous = Uri.parse("content://videos/previous.mp4")
        val current = Uri.parse("content://videos/current.mp4")
        val resolved = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val coordinator = PlaybackLaunchCoordinator(
            uriResolver = object : PlaybackUriResolver {
                override fun resolveForPlayback(sourceUri: Uri): Uri = resolved
            },
        )

        val request = coordinator.prepareRequest(
            request = PlaybackSessionRequestCodec.fromSelection(
                targetMediaId = current.toString(),
                queueMediaIds = listOf(previous.toString(), current.toString(), next.toString()),
            ),
        )

        assertEquals(current.toString(), request.originalUri)
        assertEquals(resolved.toString(), request.playbackUri)

        val clipData = PlaybackSessionRequestCodec.buildClipData(request)
        assertEquals(previous, clipData.getItemAt(0).uri)
        assertEquals(resolved, clipData.getItemAt(1)?.uri)
        assertEquals(next, clipData.getItemAt(2)?.uri)
    }

    @Test
    fun prepareRequest_prependsCurrentItemWhenExplicitQueueDoesNotContainIt() {
        val current = Uri.parse("content://videos/current.mp4")
        val resolved = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val coordinator = PlaybackLaunchCoordinator(
            uriResolver = object : PlaybackUriResolver {
                override fun resolveForPlayback(sourceUri: Uri): Uri = resolved
            },
        )

        val request = coordinator.prepareRequest(
            request = PlaybackSessionRequestCodec.fromSelection(
                targetMediaId = current.toString(),
                queueMediaIds = listOf(next.toString()),
            ),
        )

        val clipData = PlaybackSessionRequestCodec.buildClipData(request)
        assertEquals(resolved, clipData.getItemAt(0)?.uri)
        assertEquals(next, clipData.getItemAt(1)?.uri)
    }

    @Test
    fun prepareRequest_preservesStableMediaIds_whenQueueEntriesCarryOpaqueIdentity() {
        val current = PlaybackQueueEntry(
            mediaId = "media-store:1",
            uri = "content://videos/current.mp4",
        )
        val next = PlaybackQueueEntry(
            mediaId = "media-store:2",
            uri = "content://videos/next.mp4",
        )
        val coordinator = PlaybackLaunchCoordinator(
            uriResolver = object : PlaybackUriResolver {
                override fun resolveForPlayback(sourceUri: Uri): Uri = sourceUri
            },
        )

        val request = coordinator.prepareRequest(
            request = PlaybackSessionRequestCodec.fromQueueEntries(
                targetEntry = current,
                queueEntries = listOf(current, next),
            ),
        )

        assertEquals("media-store:1", request.targetEntry.mediaId)
        assertEquals(
            listOf("media-store:1", "media-store:2"),
            request.queueEntries.map(PlaybackQueueEntry::mediaId),
        )
    }
}

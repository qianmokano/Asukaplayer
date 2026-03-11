package com.asuka.player.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.asuka.player.platform.PlaybackIntentPayloadCodec
import com.asuka.player.runtime.PlaybackLaunchCoordinator
import com.asuka.player.runtime.PlaybackUriResolver
import com.asuka.player.ui.activity.PlaybackActivity
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
    fun createLaunchRequest_preservesQueueAndRemapsCurrentItem() {
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

        val request = coordinator.createLaunchRequest(
            payload = PlaybackIntentPayloadCodec.fromExternalIntent(sourceIntent)!!,
        )

        assertEquals(resolved, request.mediaUri)
        assertEquals(resolved, request.clipData.getItemAt(0).uri)
        assertEquals(next, request.clipData.getItemAt(1)?.uri)

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
    fun createLaunchRequest_buildsClipDataFromExplicitQueueWhenNoSourceIntent() {
        val previous = Uri.parse("content://videos/previous.mp4")
        val current = Uri.parse("content://videos/current.mp4")
        val resolved = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val coordinator = PlaybackLaunchCoordinator(
            uriResolver = object : PlaybackUriResolver {
                override fun resolveForPlayback(sourceUri: Uri): Uri = resolved
            },
        )

        val request = coordinator.createLaunchRequest(
            payload = PlaybackIntentPayloadCodec.fromSelection(
                targetMediaId = current.toString(),
                queueMediaIds = listOf(previous.toString(), current.toString(), next.toString()),
            ),
        )

        assertEquals(resolved, request.mediaUri)
        assertEquals(previous, request.clipData.getItemAt(0).uri)
        assertEquals(resolved, request.clipData.getItemAt(1)?.uri)
        assertEquals(next, request.clipData.getItemAt(2)?.uri)
    }

    @Test
    fun createLaunchRequest_prependsCurrentItemWhenExplicitQueueDoesNotContainIt() {
        val current = Uri.parse("content://videos/current.mp4")
        val resolved = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val coordinator = PlaybackLaunchCoordinator(
            uriResolver = object : PlaybackUriResolver {
                override fun resolveForPlayback(sourceUri: Uri): Uri = resolved
            },
        )

        val request = coordinator.createLaunchRequest(
            payload = PlaybackIntentPayloadCodec.fromSelection(
                targetMediaId = current.toString(),
                queueMediaIds = listOf(next.toString()),
            ),
        )

        assertEquals(resolved, request.clipData.getItemAt(0)?.uri)
        assertEquals(next, request.clipData.getItemAt(1)?.uri)
    }
}

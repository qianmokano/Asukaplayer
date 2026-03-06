package com.asuka.player.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
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
            mediaId = current.toString(),
            playerSettings = PlayerSettingsConfig(),
            keepConnectionInBackground = true,
            sourceIntent = sourceIntent,
        )

        assertEquals(resolved, request.mediaUri)
        assertEquals(resolved, request.clipData?.getItemAt(0)?.uri)
        assertEquals(next, request.clipData?.getItemAt(1)?.uri)
        assertTrue(request.runtimeSettings.keepSessionConnectionInBackground)

        val playbackIntent = coordinator.createPlaybackIntent(
            context = RuntimeEnvironment.getApplication(),
            request = request,
        )

        assertEquals(resolved, playbackIntent.data)
        assertNotNull(playbackIntent.clipData)
        assertTrue((playbackIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
    }
}

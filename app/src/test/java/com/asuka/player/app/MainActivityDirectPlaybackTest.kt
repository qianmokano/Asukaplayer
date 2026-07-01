package com.asuka.player.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Looper
import com.asuka.player.renderer.activity.PlaybackActivity
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(sdk = [34], application = AsuraPlayerApp::class)
@RunWith(RobolectricTestRunner::class)
class MainActivityDirectPlaybackTest {

    @Test
    fun directViewIntent_startsPlaybackActivityWithQueueAndFinishesCaller() {
        val current = Uri.parse("content://videos/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            data = current
            clipData = ClipData.newRawUri("queue", current).apply {
                addItem(ClipData.Item(next))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java, launchIntent)
        val activity = controller.setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertEquals(next, startedIntent.clipData?.getItemAt(1)?.uri)
        assertTrue((startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals(current.toString(), startedIntent.getStringExtra("com.asuka.player.extra.MEDIA_ID"))
        assertEquals(
            listOf(current.toString(), next.toString()),
            startedIntent.getStringArrayListExtra("com.asuka.player.extra.QUEUE_MEDIA_IDS")?.toList(),
        )
        assertTrue(activity.isFinishing)
    }

    @Test
    fun clipDataOnlyIntent_startsPlaybackActivityUsingFirstClipItem() {
        val current = Uri.parse("content://videos/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            clipData = ClipData.newRawUri("queue", current).apply {
                addItem(ClipData.Item(next))
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertEquals(next, startedIntent.clipData?.getItemAt(1)?.uri)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun fileViewIntent_startsPlaybackActivityWithQueue() {
        val current = Uri.parse("file:///storage/emulated/0/Movies/current.mp4")
        val next = Uri.parse("file:///storage/emulated/0/Movies/next.mp4")
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(current, "video/mp4")
            clipData = ClipData.newRawUri("queue", current).apply {
                addItem(ClipData.Item(next))
            }
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertEquals(next, startedIntent.clipData?.getItemAt(1)?.uri)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun httpsViewIntent_startsPlaybackActivity() {
        val current = Uri.parse("https://example.com/movies/current.m3u8")
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(current, "video/*")
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun rtspViewIntent_startsPlaybackActivity() {
        val current = Uri.parse("rtsp://example.com/live/stream")
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(current, "video/*")
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun octetStreamContentViewIntent_startsPlaybackActivity() {
        val current = Uri.parse("content://videos/current.bin")
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(current, "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertTrue((startedIntent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun mergedManifestDeclaresExpandedViewEntryPoints() {
        val manifest = File("build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml")
            .takeIf(File::exists)
            ?: File("build/intermediates/merged_manifests/debug/processDebugManifest/universal/AndroidManifest.xml")
        val xml = manifest.readText()

        listOf(
            "android:scheme=\"content\"" to "android:mimeType=\"video/*\"",
            "android:scheme=\"file\"" to "android:mimeType=\"video/*\"",
            "android:scheme=\"http\"" to "android:mimeType=\"video/*\"",
            "android:scheme=\"https\"" to "android:mimeType=\"video/*\"",
            "android:scheme=\"rtsp\"" to "android:mimeType=\"video/*\"",
            "android:scheme=\"content\"" to "android:mimeType=\"application/octet-stream\"",
            "android:scheme=\"file\"" to "android:mimeType=\"application/octet-stream\"",
        ).forEach { (scheme, mimeType) ->
            assertTrue(
                xml.contains(scheme) && xml.contains(mimeType),
                "Expected merged manifest to include $scheme with $mimeType",
            )
        }
        assertTrue(xml.contains("android.intent.category.BROWSABLE"))
    }

    @Test
    fun sendIntentWithExtraStream_startsPlaybackActivityAndFinishesCaller() {
        val current = Uri.parse("content://videos/current.mp4")
        val launchIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, current)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun sendMultipleIntent_preservesSharedQueueOrder() {
        val current = Uri.parse("content://videos/current.mp4")
        val next = Uri.parse("content://videos/next.mp4")
        val launchIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(current, next))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val activity = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup().get()

        val startedIntent = waitForStartedIntent(activity)
        assertEquals(PlaybackActivity::class.java.name, startedIntent.component?.className)
        assertEquals(current, startedIntent.data)
        assertEquals(current, startedIntent.clipData?.getItemAt(0)?.uri)
        assertEquals(next, startedIntent.clipData?.getItemAt(1)?.uri)
        assertTrue(activity.isFinishing)
    }

    private fun waitForStartedIntent(activity: MainActivity): Intent {
        repeat(50) {
            shadowOf(Looper.getMainLooper()).idle()
            shadowOf(activity).nextStartedActivity?.let { return it }
            Thread.sleep(10)
        }
        error("MainActivity did not start PlaybackActivity")
    }
}

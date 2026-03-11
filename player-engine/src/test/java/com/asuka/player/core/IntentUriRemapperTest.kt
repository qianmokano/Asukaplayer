package com.asuka.player.core

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.asuka.player.platform.copyIntentWithRemappedUri
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentUriRemapperTest {

    @Test
    fun copyIntentWithRemappedUri_updatesDataAndClipData() {
        val previous = Uri.parse("file:///queue/previous.mp4")
        val original = Uri.parse("content://queue/current.mp4")
        val replacement = Uri.parse("file:///cache/current.mp4")
        val next = Uri.parse("file:///queue/next.mp4")
        val originalIntent = Intent(Intent.ACTION_VIEW).apply {
            data = original
            putExtra("autoplay", true)
            clipData = ClipData.newRawUri("queue", previous).apply {
                addItem(ClipData.Item(original))
                addItem(ClipData.Item(next))
            }
        }

        val remapped = copyIntentWithRemappedUri(
            intent = originalIntent,
            originalUri = original,
            replacementUri = replacement,
        )

        assertEquals(replacement, remapped.data)
        assertEquals(true, remapped.getBooleanExtra("autoplay", false))
        val clipData = remapped.clipData ?: error("Expected clipData to be preserved")
        assertEquals(previous, clipData.getItemAt(0).uri)
        assertEquals(replacement, clipData.getItemAt(1).uri)
        assertEquals(next, clipData.getItemAt(2).uri)
    }
}

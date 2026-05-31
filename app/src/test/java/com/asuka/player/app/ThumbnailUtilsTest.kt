package com.asuka.player.app

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ThumbnailUtilsTest {

    @Test
    fun pruneVideoThumbnailCache_removesOldestFilesBySizeAndCountAndAge() {
        val context = RuntimeEnvironment.getApplication()
        val cacheDir = File(context.cacheDir, "video_thumb_cache").apply {
            deleteRecursively()
            mkdirs()
        }
        val now = 10_000L
        val expired = cacheDir.writeTestFile(name = "expired.jpg", bytes = 10, modifiedAt = 1_000L)
        cacheDir.writeTestFile(name = "old.jpg", bytes = 60, modifiedAt = 7_000L)
        cacheDir.writeTestFile(name = "middle.jpg", bytes = 60, modifiedAt = 8_000L)
        cacheDir.writeTestFile(name = "new.jpg", bytes = 60, modifiedAt = 9_000L)

        pruneVideoThumbnailCache(
            context = context,
            maxBytes = 120L,
            maxFiles = 2,
            maxAgeMs = 5_000L,
            nowMs = now,
        )

        assertFalse(expired.exists())
        assertEquals(
            listOf("middle.jpg", "new.jpg"),
            cacheDir.listFiles().orEmpty().map(File::getName).sorted(),
        )
    }

    private fun File.writeTestFile(
        name: String,
        bytes: Int,
        modifiedAt: Long,
    ): File {
        return resolve(name).also { file ->
            file.writeBytes(ByteArray(bytes))
            file.setLastModified(modifiedAt)
        }
    }
}

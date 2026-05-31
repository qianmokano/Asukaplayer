package com.asuka.player.platform

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SeekFallbackCopierTest {

    @Test
    fun copy_stopsWhenStreamExceedsFallbackLimit_evenWhenProviderSizeIsUnknown() {
        val context = RuntimeEnvironment.getApplication()
        val sourceUri = Uri.parse("content://fallback.test/huge-video.mp4")
        registerStreamProvider(sourceUri.authority.orEmpty()) {
            FixedLengthInputStream(1_025L)
        }
        val cacheDir = File(context.cacheDir, "seek-fallback-test").apply {
            deleteRecursively()
            mkdirs()
        }
        val copier = SeekFallbackCopier(
            contentResolver = context.contentResolver,
            cacheDir = cacheDir,
            maxFileBytes = 1_024L,
        )

        val copied = copier.copy(sourceUri)

        assertNull(copied)
        assertFalse(cacheDir.resolve("seek_fallback").listFiles().orEmpty().any { it.isFile })
    }

    private fun registerStreamProvider(
        authority: String,
        inputStreamFactory: () -> InputStream,
    ) {
        ShadowContentResolver.registerProviderInternal(
            authority,
            object : ContentProvider() {
                override fun onCreate(): Boolean = true

                override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
                    val pipe = ParcelFileDescriptor.createPipe()
                    Thread {
                        inputStreamFactory().use { input ->
                            FileOutputStream(pipe[1].fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }.start()
                    return pipe[0]
                }

                override fun getType(uri: Uri): String? = null

                override fun insert(uri: Uri, values: ContentValues?): Uri? = null

                override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

                override fun update(
                    uri: Uri,
                    values: ContentValues?,
                    selection: String?,
                    selectionArgs: Array<out String>?,
                ): Int = 0

                override fun query(
                    uri: Uri,
                    projection: Array<out String>?,
                    selection: String?,
                    selectionArgs: Array<out String>?,
                    sortOrder: String?,
                ): android.database.Cursor? = null
            },
        )
    }
}

private class FixedLengthInputStream(
    private var remainingBytes: Long,
) : InputStream() {
    override fun read(): Int {
        if (remainingBytes <= 0L) return -1
        remainingBytes -= 1L
        return 0
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remainingBytes <= 0L) return -1
        val count = minOf(length.toLong(), remainingBytes).toInt()
        remainingBytes -= count
        return count
    }
}

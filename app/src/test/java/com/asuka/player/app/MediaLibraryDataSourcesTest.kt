package com.asuka.player.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MediaLibraryDataSourcesTest {

    @Test
    fun scanLocalVideos_propagatesSecurityException() {
        registerMediaStoreProvider { _, _, _, _, _ ->
            throw SecurityException("permission denied")
        }
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = RuntimeEnvironment.getApplication(),
        )

        assertFailsWith<SecurityException> {
            kotlinx.coroutines.runBlocking {
                dataSource.scanLocalVideos()
            }
        }
    }

    @Test
    fun scanLocalVideos_throwsWhenQueryReturnsNullCursor() {
        registerMediaStoreProvider { _, _, _, _, _ -> null }
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = RuntimeEnvironment.getApplication(),
        )

        val error = assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking {
                dataSource.scanLocalVideos()
            }
        }

        assertEquals("MediaStore query returned a null cursor.", error.message)
    }

    @Test
    fun scanLocalVideos_returnsEmptyListWhenCursorHasNoRows() {
        registerMediaStoreProvider { _, _, _, _, _ -> emptyMediaStoreCursor() }
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = RuntimeEnvironment.getApplication(),
        )

        val result = kotlinx.coroutines.runBlocking {
            dataSource.scanLocalVideos()
        }

        assertEquals(emptyList<LocalVideoItem>(), result)
    }

    private fun registerMediaStoreProvider(
        queryHandler: (Uri, Array<out String>?, String?, Array<out String>?, String?) -> Cursor?,
    ) {
        ShadowContentResolver.registerProviderInternal(
            MediaStore.AUTHORITY,
            object : ContentProvider() {
                override fun onCreate(): Boolean = true

                override fun query(
                    uri: Uri,
                    projection: Array<out String>?,
                    selection: String?,
                    selectionArgs: Array<out String>?,
                    sortOrder: String?,
                ): Cursor? {
                    return queryHandler(uri, projection, selection, selectionArgs, sortOrder)
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
            },
        )
    }

    private fun emptyMediaStoreCursor(): Cursor {
        return MatrixCursor(
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.DATE_ADDED,
            ),
        )
    }
}

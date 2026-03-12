package com.asuka.player.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import com.asuka.player.data.AsukaMediaLibraryIndexDatabase
import com.asuka.player.data.IndexedVideoEntity
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
    fun loadFolderPage_readsCachedIndexWithoutInitialMediaStoreQuery() {
        val context = RuntimeEnvironment.getApplication()
        val database = AsukaMediaLibraryIndexDatabase.inMemory(context)
        database.indexedVideoDao().upsertAll(
            listOf(
                IndexedVideoEntity(
                    mediaStoreId = 1L,
                    uri = "content://media/external/video/media/1",
                    title = "cached.mp4",
                    durationMs = 1_000L,
                    sizeBytes = 2_000L,
                    folderName = "Movies",
                    folderPath = "/storage/emulated/0/Movies",
                    folderId = 7L,
                    dateAddedSec = 10L,
                    dateModifiedSec = 10L,
                ),
            ),
        )
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = context,
            database = database,
        )

        val result = kotlinx.coroutines.runBlocking {
            dataSource.loadFolderPage(MediaLibraryPageRequest(offset = 0, limit = 20))
        }

        assertEquals(listOf("Movies"), result.items.map(LocalVideoFolder::name))
        assertEquals(1, result.totalCount)
    }

    @Test
    fun syncIndex_propagatesSecurityException() {
        registerMediaStoreProvider { _, _, _, _, _ ->
            throw SecurityException("permission denied")
        }
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = RuntimeEnvironment.getApplication(),
        )

        assertFailsWith<SecurityException> {
            kotlinx.coroutines.runBlocking {
                dataSource.syncIndex(forceFullRescan = false)
            }
        }
    }

    @Test
    fun syncIndex_throwsWhenQueryReturnsNullCursor() {
        registerMediaStoreProvider { _, _, _, _, _ -> null }
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = RuntimeEnvironment.getApplication(),
        )

        val error = assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking {
                dataSource.syncIndex(forceFullRescan = false)
            }
        }

        assertEquals("MediaStore query returned a null cursor.", error.message)
    }

    @Test
    fun loadVideoPage_returnsEmptyListWhenIndexHasNoRows() {
        val dataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = RuntimeEnvironment.getApplication(),
        )

        val result = kotlinx.coroutines.runBlocking {
            dataSource.loadVideoPage(MediaLibraryPageRequest(offset = 0, limit = 20))
        }

        assertEquals(emptyList<LocalVideoItem>(), result.items)
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
                MediaStore.Video.Media.DATE_MODIFIED,
            ),
        )
    }
}

package com.asuka.player.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import com.asuka.player.data.AsukaMediaLibraryIndexDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MediaLibraryIndexingCoordinatorTest {

    @Test
    fun syncNow_afterInitialIndex_usesDateModifiedCutoffForIncrementalQuery() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val metadataSelections = mutableListOf<Pair<String?, List<String>?>>()
        registerMediaStoreProvider { _, projection, selection, selectionArgs, _ ->
            when {
                projection?.contentEquals(arrayOf(MediaStore.Video.Media._ID)) == true -> {
                    MatrixCursor(arrayOf(MediaStore.Video.Media._ID)).apply {
                        addRow(arrayOf(42L))
                    }
                }

                projection?.contains(MediaStore.Video.Media.DATE_MODIFIED) == true -> {
                    metadataSelections += selection to selectionArgs?.toList()
                    MatrixCursor(
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
                    ).apply {
                        addRow(
                            arrayOf<Any?>(
                                42L,
                                "video.mp4",
                                1_000L,
                                2_000L,
                                null,
                                "Movies",
                                7L,
                                10L,
                                100L,
                            ),
                        )
                    }
                }

                else -> emptyCursor()
            }
        }

        val database = AsukaMediaLibraryIndexDatabase.inMemory(context)
        val coordinator = MediaLibraryIndexingCoordinator(
            context = context,
            database = database,
        )

        try {
            coordinator.syncNow(forceFullRescan = false)
            coordinator.syncNow(forceFullRescan = false)

            assertEquals(2, metadataSelections.size)
            assertTrue(
                metadataSelections.first().first.isNullOrBlank() ||
                    metadataSelections.first().first?.contains(MediaStore.Video.Media.IS_PENDING) == true,
            )
            assertTrue(metadataSelections.last().first?.contains(MediaStore.Video.Media.DATE_MODIFIED) == true)
            assertEquals(listOf("100"), metadataSelections.last().second)
        } finally {
            coordinator.close()
        }
    }

    @Test
    fun syncNow_withObservedDeletedItem_removesRowWithoutFullIdRescan() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        var metadataQueryCount = 0
        var fullIdScanCount = 0
        var targetedIdQueryCount = 0
        registerMediaStoreProvider { _, projection, selection, selectionArgs, _ ->
            when {
                projection?.contentEquals(arrayOf(MediaStore.Video.Media._ID)) == true && selection?.contains("IN") != true -> {
                    fullIdScanCount += 1
                    MatrixCursor(arrayOf(MediaStore.Video.Media._ID)).apply {
                        if (fullIdScanCount == 1) {
                            addRow(arrayOf(42L))
                        }
                    }
                }

                projection?.contentEquals(arrayOf(MediaStore.Video.Media._ID)) == true && selection?.contains("IN") == true -> {
                    targetedIdQueryCount += 1
                    MatrixCursor(arrayOf(MediaStore.Video.Media._ID))
                }

                projection?.contains(MediaStore.Video.Media.DATE_MODIFIED) == true -> {
                    metadataQueryCount += 1
                    MatrixCursor(
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
                    ).apply {
                        if (metadataQueryCount == 1) {
                            addRow(
                                arrayOf<Any?>(
                                    42L,
                                    "video.mp4",
                                    1_000L,
                                    2_000L,
                                    null,
                                    "Movies",
                                    7L,
                                    10L,
                                    100L,
                                ),
                            )
                        }
                    }
                }

                else -> emptyCursor()
            }
        }

        val database = AsukaMediaLibraryIndexDatabase.inMemory(context)
        val coordinator = MediaLibraryIndexingCoordinator(
            context = context,
            database = database,
        )

        try {
            coordinator.syncNow(forceFullRescan = false)
            coordinator.recordObservedChangeForTest(
                android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 42L),
            )
            coordinator.syncNow(forceFullRescan = false)

            assertEquals(1, fullIdScanCount, "expected no extra full-id reconciliation after observed delete")
            assertEquals(1, targetedIdQueryCount, "expected one targeted id existence query for observed delete")
            assertEquals(0, database.indexedVideoDao().count(), "expected observed delete to remove stale row from index")
        } finally {
            coordinator.close()
        }
    }

    @Test
    @Config(sdk = [29])
    fun syncNow_withObservedAddedItem_upsertsMetadataWithoutDateModifiedAdvance() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        var metadataQueryCount = 0
        var targetedMetadataQueryCount = 0
        registerMediaStoreProvider { _, projection, selection, selectionArgs, _ ->
            when {
                projection?.contentEquals(arrayOf(MediaStore.Video.Media._ID)) == true && selection?.contains("IN") == true -> {
                    MatrixCursor(arrayOf(MediaStore.Video.Media._ID)).apply {
                        addRow(arrayOf(43L))
                    }
                }

                projection?.contains(MediaStore.Video.Media.DATE_MODIFIED) == true && selection?.contains("IN") == true -> {
                    targetedMetadataQueryCount += 1
                    MatrixCursor(
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
                    ).apply {
                        addRow(
                            arrayOf<Any?>(
                                43L,
                                "observed.mp4",
                                2_000L,
                                3_000L,
                                null,
                                "Movies",
                                8L,
                                11L,
                                50L,
                            ),
                        )
                    }
                }

                projection?.contains(MediaStore.Video.Media.DATE_MODIFIED) == true -> {
                    metadataQueryCount += 1
                    MatrixCursor(
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
                    ).apply {
                        if (metadataQueryCount == 1) {
                            addRow(
                                arrayOf<Any?>(
                                    42L,
                                    "video.mp4",
                                    1_000L,
                                    2_000L,
                                    null,
                                    "Movies",
                                    7L,
                                    10L,
                                    100L,
                                ),
                            )
                        }
                    }
                }

                else -> emptyCursor()
            }
        }

        val database = AsukaMediaLibraryIndexDatabase.inMemory(context)
        val coordinator = MediaLibraryIndexingCoordinator(
            context = context,
            database = database,
        )

        try {
            coordinator.syncNow(forceFullRescan = false)
            coordinator.recordObservedChangeForTest(
                android.content.ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, 43L),
            )
            coordinator.syncNow(forceFullRescan = false)

            val rows = database.indexedVideoDao().findByIds(listOf(42L, 43L))
            assertEquals(setOf(42L, 43L), rows.map { it.mediaStoreId }.toSet())
            assertEquals(1, targetedMetadataQueryCount, "expected one targeted metadata refresh for observed addition")
        } finally {
            coordinator.close()
        }
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

    private fun emptyCursor(): Cursor {
        return MatrixCursor(arrayOf(MediaStore.Video.Media._ID))
    }
}

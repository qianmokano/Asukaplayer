package com.asuka.player.app

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.asuka.player.R
import com.asuka.player.data.IndexedVideoEntity
import java.io.File

internal const val MEDIA_STORE_ID_CHUNK_SIZE = 900

internal class MediaStoreIndexReader(
    private val appContext: Context,
) {
    private val contentResolver: ContentResolver = appContext.contentResolver

    fun queryAllCurrentIds(volumeName: String): Set<Long> {
        val cursor = contentResolver.query(
            videoCollectionUri(volumeName),
            arrayOf(MediaStore.Video.Media._ID),
            baseSelection(),
            null,
            null,
        ) ?: throw IllegalStateException(NULL_CURSOR_MESSAGE)
        return cursor.use {
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            buildSet {
                while (cursor.moveToNext()) add(cursor.getLong(idIndex))
            }
        }
    }

    fun queryIndexedVideos(
        volumeName: String,
        generationBaseline: GenerationBaseline?,
        modifiedSinceInclusive: Long?,
    ): List<IndexedVideoEntity> {
        val cursor = contentResolver.query(
            videoCollectionUri(volumeName),
            mediaStoreProjection(),
            buildSelection(
                base = baseSelection(),
                generationBaseline = generationBaseline,
                modifiedSinceInclusive = modifiedSinceInclusive,
            ),
            buildSelectionArgs(
                generationBaseline = generationBaseline,
                modifiedSinceInclusive = modifiedSinceInclusive,
            ),
            buildSortOrder(generationBaseline),
        ) ?: throw IllegalStateException(NULL_CURSOR_MESSAGE)
        return cursor.use { readIndexedVideos(it, volumeName) }
    }

    fun queryExistingIds(volumeName: String, ids: Set<Long>): Set<Long> {
        if (ids.isEmpty()) return emptySet()
        return ids.chunked(MEDIA_STORE_ID_CHUNK_SIZE).flatMapTo(mutableSetOf()) { chunk ->
            val cursor = contentResolver.query(
                videoCollectionUri(volumeName),
                arrayOf(MediaStore.Video.Media._ID),
                selectionByIds(chunk),
                chunk.map(Long::toString).toTypedArray(),
                null,
            ) ?: throw IllegalStateException(NULL_CURSOR_MESSAGE)
            cursor.use {
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                buildList {
                    while (cursor.moveToNext()) add(cursor.getLong(idIndex))
                }
            }
        }
    }

    fun queryIndexedVideosByIds(volumeName: String, ids: Set<Long>): List<IndexedVideoEntity> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(MEDIA_STORE_ID_CHUNK_SIZE).flatMap { chunk ->
            val cursor = contentResolver.query(
                videoCollectionUri(volumeName),
                mediaStoreProjection(),
                selectionByIds(chunk),
                chunk.map(Long::toString).toTypedArray(),
                "${MediaStore.Video.Media._ID} ASC",
            ) ?: throw IllegalStateException(NULL_CURSOR_MESSAGE)
            cursor.use { readIndexedVideos(it, volumeName) }
        }
    }

    fun parseItem(uri: Uri): MediaStoreItemRef? {
        val mediaStoreId = uri.lastPathSegment?.toLongOrNull() ?: return null
        if (uri.authority != MediaStore.AUTHORITY) return null
        val volumeName = uri.pathSegments.firstOrNull() ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volumeName == MediaStore.VOLUME_EXTERNAL) {
            return null
        }
        return MediaStoreItemRef(volumeName = volumeName, mediaStoreId = mediaStoreId)
    }

    private fun selectionByIds(ids: List<Long>): String {
        val placeholders = ids.joinToString(",") { "?" }
        return listOfNotNull(baseSelection(), "${MediaStore.Video.Media._ID} IN ($placeholders)")
            .joinToString(" AND ")
    }

    private fun readIndexedVideos(cursor: Cursor, volumeName: String): List<IndexedVideoEntity> {
        val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dataPathIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
        val folderNameIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val folderIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
        val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val dateModifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val generationAddedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.GENERATION_ADDED)
        val generationModifiedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.GENERATION_MODIFIED)

        return buildList {
            while (cursor.moveToNext()) {
                add(cursor.toIndexedVideo(volumeName, idIndex, titleIndex, durationIndex, sizeIndex, dataPathIndex,
                    folderNameIndex, folderIdIndex, dateAddedIndex, dateModifiedIndex, generationAddedIndex,
                    generationModifiedIndex))
            }
        }
    }

    @Suppress("LongParameterList")
    private fun Cursor.toIndexedVideo(
        volumeName: String,
        idIndex: Int,
        titleIndex: Int,
        durationIndex: Int,
        sizeIndex: Int,
        dataPathIndex: Int,
        folderNameIndex: Int,
        folderIdIndex: Int,
        dateAddedIndex: Int,
        dateModifiedIndex: Int,
        generationAddedIndex: Int,
        generationModifiedIndex: Int,
    ): IndexedVideoEntity {
        val mediaStoreId = getLong(idIndex)
        val uri = ContentUris.withAppendedId(videoCollectionUri(volumeName), mediaStoreId).toString()
        val fallbackFolderName = getString(folderNameIndex)
            ?.takeIf(String::isNotBlank)
            ?: appContext.getString(R.string.unknown_folder)
        return IndexedVideoEntity(
            volumeName = volumeName,
            mediaStoreId = mediaStoreId,
            uri = uri,
            title = getString(titleIndex) ?: uri.substringAfterLast('/'),
            durationMs = getLong(durationIndex).coerceAtLeast(0L),
            sizeBytes = getLong(sizeIndex).coerceAtLeast(0L),
            folderName = fallbackFolderName,
            folderPath = resolveFolderPath(
                rawDataPath = if (dataPathIndex >= 0) getString(dataPathIndex) else null,
                fallbackFolderName = fallbackFolderName,
            ),
            folderId = getLong(folderIdIndex),
            dateAddedSec = getLong(dateAddedIndex).coerceAtLeast(0L),
            dateModifiedSec = getLong(dateModifiedIndex).coerceAtLeast(0L),
            generationAdded = if (generationAddedIndex >= 0) getLong(generationAddedIndex).coerceAtLeast(0L) else 0L,
            generationModified = if (generationModifiedIndex >= 0) {
                getLong(generationModifiedIndex).coerceAtLeast(0L)
            } else {
                0L
            },
        )
    }

    private fun mediaStoreProjection(): Array<String> {
        val columns = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            columns += MediaStore.MediaColumns.GENERATION_ADDED
            columns += MediaStore.MediaColumns.GENERATION_MODIFIED
        }
        return columns.toTypedArray()
    }

    private fun resolveFolderPath(rawDataPath: String?, fallbackFolderName: String): String {
        return rawDataPath
            ?.takeIf(String::isNotBlank)
            ?.let { File(it).parent }
            ?.takeIf(String::isNotBlank)
            ?.replace(
                Environment.getExternalStorageDirectory().absolutePath,
                appContext.getString(R.string.internal_storage),
            )
            ?: fallbackFolderName
    }

    private fun baseSelection(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Video.Media.IS_PENDING}=0"
        } else {
            null
        }
    }

    private fun videoCollectionUri(volumeName: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(volumeName)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    private companion object {
        const val NULL_CURSOR_MESSAGE = "MediaStore query returned a null cursor."
    }
}

internal data class MediaStoreItemRef(
    val volumeName: String,
    val mediaStoreId: Long,
)

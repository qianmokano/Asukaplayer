package com.asuka.player.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.asuka.player.R
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.QueueHistoryRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface VideoAccessDataSource {
    fun readVideoAccessState(): VideoAccessState
}

internal interface LocalVideoCatalogDataSource {
    suspend fun scanLocalVideos(): List<LocalVideoItem>

    suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int)
}

internal interface RecentPlaybackDataSource {
    suspend fun loadRecentMediaIds(limit: Int): List<String>
}

internal class AndroidVideoAccessDataSource(
    context: Context,
) : VideoAccessDataSource {
    private val appContext = context.applicationContext

    override fun readVideoAccessState(): VideoAccessState {
        val permissionGranted = hasFullVideoPermission(appContext)
        return VideoAccessState(
            permissionGranted = permissionGranted,
            userSelectedPermissionGranted = hasUserSelectedVideoPermission(appContext) && !permissionGranted,
        )
    }

    private fun hasFullVideoPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasUserSelectedVideoPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

internal class AndroidMediaStoreVideoCatalogDataSource(
    context: Context,
) : LocalVideoCatalogDataSource {
    private val appContext = context.applicationContext

    override suspend fun scanLocalVideos(): List<LocalVideoItem> {
        return withContext(Dispatchers.IO) {
            queryMediaStoreVideos()
        }
    }

    override suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int) {
        withContext(Dispatchers.IO) {
            warmupInitialThumbnails(
                context = appContext,
                videos = videos,
                limit = limit,
            )
        }
    }

    private fun queryMediaStoreVideos(): List<LocalVideoItem> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATE_ADDED,
        )
        val selection = if (Build.VERSION.SDK_INT >= 29) {
            "${MediaStore.Video.Media.IS_PENDING}=0"
        } else {
            null
        }
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
        return try {
            buildList {
                appContext.contentResolver.query(
                    collection,
                    projection,
                    selection,
                    null,
                    sortOrder,
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val dataPathIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val folderNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                    val folderIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                    val dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        val uri = ContentUris.withAppendedId(collection, id)
                        val fallbackFolderName = cursor.getString(folderNameIdx)
                            ?.takeIf { it.isNotBlank() }
                            ?: appContext.getString(R.string.unknown_folder)
                        val fullFolderPath = if (dataPathIdx >= 0) {
                            cursor.getString(dataPathIdx)
                                ?.takeIf { it.isNotBlank() }
                                ?.let { File(it).parent }
                                ?.takeIf { it.isNotBlank() }
                        } else {
                            null
                        }?.replace(
                            Environment.getExternalStorageDirectory().absolutePath,
                            appContext.getString(R.string.internal_storage),
                        ) ?: fallbackFolderName
                        add(
                            LocalVideoItem(
                                id = id,
                                uri = uri,
                                title = cursor.getString(titleIdx) ?: uri.lastPathSegment.orEmpty(),
                                durationMs = cursor.getLong(durationIdx).coerceAtLeast(0L),
                                sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L),
                                folderName = fallbackFolderName,
                                folderPath = fullFolderPath,
                                folderId = cursor.getLong(folderIdIdx),
                                dateAddedSec = cursor.getLong(dateAddedIdx).coerceAtLeast(0L),
                            ),
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}

internal class PlaybackRecentMediaDataSource(
    private val playbackStateRepository: PlaybackStateRepository,
    private val queueHistoryRepository: QueueHistoryRepository,
) : RecentPlaybackDataSource {
    override suspend fun loadRecentMediaIds(limit: Int): List<String> {
        return withContext(Dispatchers.IO) {
            resolveRecentMediaIds(
                historyUris = queueHistoryRepository.items(),
                fallbackMediaIds = playbackStateRepository.recentMediaIds(limit = limit),
            )
        }
    }
}

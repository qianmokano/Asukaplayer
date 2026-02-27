package com.asuka.player.app

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.asuka.player.R
import java.io.File

internal fun hasVideoPermission(context: Context): Boolean {
    val permissions = videoPermissionsForRuntime()
    return permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

internal fun videoPermissionsForRuntime(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

internal fun queryLocalVideos(context: Context): List<LocalVideoItem> {
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
            context.contentResolver.query(
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
                        ?: context.getString(R.string.unknown_folder)
                    val fullFolderPath = if (dataPathIdx >= 0) {
                        cursor.getString(dataPathIdx)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { File(it).parent }
                            ?.takeIf { it.isNotBlank() }
                    } else {
                        null
                    }?.replace("/storage/emulated/0", context.getString(R.string.internal_storage)) ?: fallbackFolderName
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

internal fun readAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Throwable) {
        "unknown"
    }
}

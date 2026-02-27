package com.asuka.player.core

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Shared helper for copying a content:// URI to the local cache so that seek works reliably.
 * Used by both PlaybackActivity (reactive, post-error fallback) and MainActivity (pre-launch check).
 */
class SeekFallbackCopier(
    private val contentResolver: ContentResolver,
    private val cacheDir: File,
) {
    companion object {
        /** Maximum per-file size that will be copied (500 MB). */
        const val MAX_FILE_BYTES = 500L * 1024L * 1024L

        /** Maximum total size of the seek_fallback cache folder (1 GB). */
        const val MAX_CACHE_BYTES = 1024L * 1024L * 1024L
    }

    /**
     * Copies [uri] to the seek_fallback cache folder.
     *
     * @param checkSize when true, skips copying if the file exceeds [MAX_FILE_BYTES].
     * @return a `file://` URI pointing to the local copy, or null on failure.
     */
    fun copy(uri: Uri, checkSize: Boolean = true): Uri? {
        if (checkSize) {
            val fileSize = queryFileSize(uri)
            if (fileSize >= 0L && fileSize > MAX_FILE_BYTES) {
                Log.w("AsukaSeekFallback", "skip fallback: file too large ($fileSize bytes)")
                return null
            }
        }
        val cacheFolder = File(cacheDir, "seek_fallback").apply { mkdirs() }
        cleanOldFiles(cacheFolder)
        val displayName = queryDisplayName(uri)
        val targetFile = File(cacheFolder, buildFallbackFileName(displayName))
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(targetFile)
        } catch (error: Throwable) {
            Log.w("AsukaSeekFallback", "copy failed for uri=$uri", error)
            null
        }
    }

    fun queryFileSize(uri: Uri): Long {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx) else -1L
                } ?: -1L
        } catch (_: Throwable) { -1L }
    }

    fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
                }
        } catch (_: Throwable) {
            null
        }
    }

    /** Removes files older than 24 h and evicts oldest until the folder is under [MAX_CACHE_BYTES]. */
    fun cleanOldFiles(folder: File) {
        val now = System.currentTimeMillis()
        val maxAgeMs = 24L * 60L * 60L * 1000L
        // Collect all files once, delete stale ones, then apply LRU eviction on the rest.
        val allFiles = folder.listFiles() ?: return
        val remaining = allFiles
            .onEach { file -> if (now - file.lastModified() > maxAgeMs) file.delete() }
            .filter { it.exists() }
            .sortedBy { it.lastModified() }
        var totalBytes = remaining.sumOf { it.length() }
        for (file in remaining) {
            if (totalBytes <= MAX_CACHE_BYTES) break
            totalBytes -= file.length()
            file.delete()
        }
    }

    private fun buildFallbackFileName(displayName: String?): String {
        val safeName = (displayName ?: "video")
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(80)
            .ifBlank { "video" }
        val timestamp = System.currentTimeMillis()
        return String.format(Locale.ROOT, "fallback_%d_%s", timestamp, safeName)
    }
}

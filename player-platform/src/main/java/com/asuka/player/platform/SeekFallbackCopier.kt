package com.asuka.player.platform

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class SeekFallbackCopier(
    private val contentResolver: ContentResolver,
    private val cacheDir: File,
) {
    companion object {
        const val MAX_FILE_BYTES = 500L * 1024L * 1024L
        const val MAX_CACHE_BYTES = 1024L * 1024L * 1024L
    }

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
        } catch (error: Exception) {
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
        } catch (_: Exception) {
            -1L
        }
    }

    fun queryDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    fun cleanOldFiles(folder: File) {
        val now = System.currentTimeMillis()
        val maxAgeMs = 24L * 60L * 60L * 1000L
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

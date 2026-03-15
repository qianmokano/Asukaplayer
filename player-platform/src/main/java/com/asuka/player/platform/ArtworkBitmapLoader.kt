package com.asuka.player.platform

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size

/**
 * Loads a thumbnail bitmap from a content or file URI.
 * Uses [ContentResolver.loadThumbnail] on Android Q+ for content URIs,
 * falling back to [MediaMetadataRetriever] for file URIs and older APIs.
 */
fun loadArtworkBitmap(
    contentResolver: ContentResolver,
    uri: Uri,
): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri.scheme == "content") {
        runCatching { return contentResolver.loadThumbnail(uri, Size(512, 512), null) }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: return null
                retriever.setDataSource(path)
            }
            else -> {
                val pfd = runCatching { contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
                pfd.use { retriever.setDataSource(it.fileDescriptor) }
            }
        }
        retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (e: Exception) {
        Log.w("ArtworkLoader", "Failed to load artwork bitmap for $uri", e)
        null
    } finally {
        runCatching { retriever.release() }
    }
}

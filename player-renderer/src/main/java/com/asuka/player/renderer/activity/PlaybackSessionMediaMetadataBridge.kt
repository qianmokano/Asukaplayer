package com.asuka.player.renderer.activity

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Size
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PlaybackSessionMediaMetadataBridge(
    private val contentResolver: ContentResolver,
    private val scope: CoroutineScope,
) {
    private class LruSet<K>(private val maxSize: Int) {
        private val map = object : LinkedHashMap<K, Unit>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, Unit>): Boolean {
                return size > maxSize
            }
        }

        fun add(key: K): Boolean {
            val existed = map.containsKey(key)
            map[key] = Unit
            return !existed
        }
    }

    private val artworkSetForMediaIds = LruSet<String>(MAX_ARTWORK_MEDIA_IDS)

    suspend fun resolveTitle(uri: Uri): String? {
        val fromContent = if (uri.scheme == "content") {
            runCatching {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                    }
            }.getOrNull()
        } else {
            null
        }
        val fromPath = uri.lastPathSegment
        return fromContent?.takeIf { it.isNotBlank() }
            ?: fromPath?.takeIf { it.isNotBlank() }
            ?: uri.toString().takeIf { it.isNotBlank() }
    }

    fun maybeLoadAndSetArtwork(
        controller: MediaController,
        mediaId: String,
        index: Int,
        uri: Uri,
    ) {
        val scheme = uri.scheme ?: return
        if (scheme != "content" && scheme != "file") return
        if (!artworkSetForMediaIds.add(mediaId)) return
        scope.launch(Dispatchers.IO) {
            val bitmap = loadArtworkBitmap(uri) ?: return@launch
            val bytes = ByteArrayOutputStream().use { baos ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
                baos.toByteArray()
            }
            withContext(Dispatchers.Main) {
                val current = controller.currentMediaItemIndex
                if (current != index) return@withContext
                val item = controller.currentMediaItem ?: return@withContext
                if (item.mediaId != mediaId) return@withContext
                val metadata = item.mediaMetadata.buildUpon()
                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
                controller.replaceMediaItem(index, item.buildUpon().setMediaMetadata(metadata).build())
            }
        }
    }

    private fun loadArtworkBitmap(uri: Uri): android.graphics.Bitmap? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && uri.scheme == "content") {
            runCatching {
                return contentResolver.loadThumbnail(uri, Size(512, 512), null)
            }
        }
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file") {
                val path = uri.path ?: return null
                retriever.setDataSource(path)
            } else {
                val pfd = runCatching { contentResolver.openFileDescriptor(uri, "r") }.getOrNull() ?: return null
                pfd.use { retriever.setDataSource(it.fileDescriptor) }
            }
            retriever.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private companion object {
        private const val MAX_ARTWORK_MEDIA_IDS = 200
    }
}

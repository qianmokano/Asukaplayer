package com.asuka.player.renderer.activity

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import com.asuka.player.platform.loadArtworkBitmap
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PlaybackSessionMediaMetadataBridge(
    private val contentResolver: ContentResolver,
    private val scope: CoroutineScope,
) {
    private val artworkSetForMediaIds = mutableSetOf<String>()

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
            val bitmap = loadArtworkBitmap(contentResolver, uri) ?: return@launch
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
}

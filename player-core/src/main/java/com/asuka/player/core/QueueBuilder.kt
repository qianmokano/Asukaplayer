package com.asuka.player.core

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * Builds a media queue from a list of URIs. Pure mapping only.
 */
object QueueBuilder {
    data class Queue(
        val items: List<MediaItem>,
        val startIndex: Int,
    )

    fun build(
        uris: List<Uri>,
        startUri: Uri?,
        titleResolver: ((Uri) -> String?)? = null,
    ): Queue {
        val startIndex = uris.indexOfFirst { it == startUri }.takeIf { it >= 0 } ?: 0
        val items = uris.map { uri ->
            val title = titleResolver?.invoke(uri)?.takeIf { it.isNotBlank() }
                ?: (uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: uri.toString())
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(uri.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setIsPlayable(true)
                        .build(),
                )
                .build()
        }
        return Queue(items = items, startIndex = startIndex)
    }
}

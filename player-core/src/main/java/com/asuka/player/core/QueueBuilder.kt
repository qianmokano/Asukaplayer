package com.asuka.player.core

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.net.Uri

/**
 * Builds a media queue from a list of URIs. Pure mapping only.
 */
object QueueBuilder {
    data class Queue(
        val items: List<MediaItem>,
        val startIndex: Int,
    )

    fun build(uris: List<Uri>, startUri: Uri?): Queue {
        val startIndex = uris.indexOfFirst { it == startUri }.takeIf { it >= 0 } ?: 0
        val items = uris.map { uri ->
            MediaItem.Builder()
                .setUri(uri)
                .setMediaId(uri.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(uri.lastPathSegment ?: "")
                        .build(),
                )
                .build()
        }
        return Queue(items = items, startIndex = startIndex)
    }
}

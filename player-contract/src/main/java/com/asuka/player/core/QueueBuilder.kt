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
        entries: List<PlaybackQueueEntry>,
        startMediaId: String?,
        titleResolver: ((Uri) -> String?)? = null,
    ): Queue {
        val startIndex = entries.indexOfFirst { it.mediaId == startMediaId }.takeIf { it >= 0 } ?: 0
        val items = entries.map { entry ->
            val title = titleResolver?.invoke(entry.uri)?.takeIf { it.isNotBlank() }
                ?: (entry.uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: entry.uri.toString())
            MediaItem.Builder()
                .setUri(entry.uri)
                .setMediaId(entry.mediaId)
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

    fun build(
        uris: List<Uri>,
        startUri: Uri?,
        titleResolver: ((Uri) -> String?)? = null,
    ): Queue {
        return build(
            entries = uris.map { uri -> PlaybackQueueEntry(mediaId = uri.toString(), uri = uri) },
            startMediaId = startUri?.toString(),
            titleResolver = titleResolver,
        )
    }
}

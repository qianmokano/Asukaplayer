package com.asuka.player.platform

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.asuka.player.contract.PlaybackQueue

fun PlaybackQueue.toMediaItems(
    targetMediaId: String? = null,
    targetPlaybackUri: String? = null,
): List<MediaItem> {
    return items.map { item ->
        val uri = if (item.mediaId == targetMediaId && !targetPlaybackUri.isNullOrBlank()) {
            targetPlaybackUri
        } else {
            item.uri
        }
        MediaItem.Builder()
            .setUri(uri)
            .setMediaId(item.mediaIdForPlayback())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }
}

fun String.isTransientPlaybackMediaId(): Boolean = startsWith(TRANSIENT_MEDIA_ID_PREFIX)

private fun com.asuka.player.contract.PlaybackQueueItem.mediaIdForPlayback(): String {
    return if (persistable) mediaId else "$TRANSIENT_MEDIA_ID_PREFIX$mediaId"
}

private const val TRANSIENT_MEDIA_ID_PREFIX = "transient:"

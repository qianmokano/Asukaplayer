package com.asuka.player.platform

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.asuka.player.contract.PlaybackQueue

fun PlaybackQueue.toMediaItems(): List<MediaItem> {
    return items.map { item ->
        MediaItem.Builder()
            .setUri(item.uri)
            .setMediaId(item.mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(item.title)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()
    }
}

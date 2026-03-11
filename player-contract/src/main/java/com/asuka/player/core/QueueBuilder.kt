package com.asuka.player.contract

/**
 * Builds a playback queue from pure queue entries without binding to a player implementation.
 */
data class PlaybackQueueItem(
    val mediaId: String,
    val uri: String,
    val title: String,
)

data class PlaybackQueue(
    val items: List<PlaybackQueueItem>,
    val startIndex: Int,
)

object QueueBuilder {
    fun build(
        entries: List<PlaybackQueueEntry>,
        startMediaId: String?,
        titleResolver: ((String) -> String?)? = null,
    ): PlaybackQueue {
        val startIndex = entries.indexOfFirst { it.mediaId == startMediaId }.takeIf { it >= 0 } ?: 0
        val items = entries.map { entry ->
            val title = titleResolver?.invoke(entry.uri)?.takeIf { it.isNotBlank() }
                ?: entry.uri.substringAfterLast('/').takeIf { it.isNotBlank() }
                ?: entry.uri
            PlaybackQueueItem(
                mediaId = entry.mediaId,
                uri = entry.uri,
                title = title,
            )
        }
        return PlaybackQueue(items = items, startIndex = startIndex)
    }

    fun buildFromUris(
        uris: List<String>,
        startUri: String?,
        titleResolver: ((String) -> String?)? = null,
    ): PlaybackQueue {
        return build(
            entries = uris.map { uri -> PlaybackQueueEntry(mediaId = uri.toString(), uri = uri) },
            startMediaId = startUri?.toString(),
            titleResolver = titleResolver,
        )
    }
}

package com.asuka.player.platform

import android.content.Intent
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionRequest

object IntentQueueReader {
    fun applyPlaybackIdentity(
        intent: Intent,
        mediaId: String,
        queueMediaIds: List<String>,
    ) {
        val queueUris = PlaybackSessionRequestCodec.readRuntimeQueueUris(intent)
        if (queueUris.isNotEmpty()) {
            val normalizedMediaIds = queueMediaIds
                .filter { it.isNotBlank() }
                .distinct()
                .toMutableList()
            if (mediaId.isNotBlank() && mediaId !in normalizedMediaIds) {
                normalizedMediaIds.add(0, mediaId)
            }
            val queueEntries = queueUris.mapIndexed { index, uri ->
                PlaybackQueueEntry(
                    mediaId = normalizedMediaIds.getOrElse(index) { uri },
                    uri = uri,
                )
            }
            val startIndex = queueEntries.indexOfFirst { it.mediaId == mediaId }
                .takeIf { it >= 0 }
                ?: 0
            PlaybackSessionRequestCodec.applyPlaybackRequest(
                intent = intent,
                request = PlaybackSessionRequest(
                    queueEntries = queueEntries,
                    startIndex = startIndex,
                    playbackUri = intent.data?.toString() ?: queueEntries[startIndex].uri,
                ),
            )
            return
        }

        PlaybackSessionRequestCodec.applyPlaybackRequest(
            intent = intent,
            request = PlaybackSessionRequestCodec.fromSelection(
                targetMediaId = mediaId,
                queueMediaIds = queueMediaIds,
            ),
        )
    }

    fun readTargetMediaId(intent: Intent): String? {
        return PlaybackSessionRequestCodec.readPlaybackRequest(intent)?.targetEntry?.mediaId
            ?: intent.data?.toString()
    }

    fun readEntries(intent: Intent): List<PlaybackQueueEntry> {
        return PlaybackSessionRequestCodec.readPlaybackRequest(intent)?.queueEntries.orEmpty()
    }

    fun read(intent: Intent): List<String> {
        return readEntries(intent).map(PlaybackQueueEntry::uri)
    }
}

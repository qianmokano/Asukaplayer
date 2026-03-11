package com.asuka.player.platform

import android.content.Intent
import com.asuka.player.contract.PlaybackQueueEntry

object IntentQueueReader {
    fun applyPlaybackIdentity(
        intent: Intent,
        mediaId: String,
        queueMediaIds: List<String>,
    ) {
        val queueUris = PlaybackIntentPayloadCodec.readRuntimeQueueUris(intent)
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
            PlaybackIntentPayloadCodec.applyPlaybackPayload(
                intent = intent,
                payload = PlaybackIntentPayload(
                    queueEntries = queueEntries,
                    startIndex = startIndex,
                ),
            )
            return
        }

        PlaybackIntentPayloadCodec.applyPlaybackPayload(
            intent = intent,
            payload = PlaybackIntentPayloadCodec.fromSelection(
                targetMediaId = mediaId,
                queueMediaIds = queueMediaIds,
            ),
        )
    }

    fun readTargetMediaId(intent: Intent): String? {
        return PlaybackIntentPayloadCodec.readPlaybackIntent(intent)?.targetEntry?.mediaId
            ?: intent.data?.toString()
    }

    fun readEntries(intent: Intent): List<PlaybackQueueEntry> {
        return PlaybackIntentPayloadCodec.readPlaybackIntent(intent)?.queueEntries.orEmpty()
    }

    fun read(intent: Intent): List<String> {
        return readEntries(intent).map(PlaybackQueueEntry::uri)
    }
}

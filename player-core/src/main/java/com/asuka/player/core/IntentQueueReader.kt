package com.asuka.player.core

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import java.util.ArrayList

object IntentQueueReader {
    private const val EXTRA_MEDIA_ID = "com.asuka.player.extra.MEDIA_ID"
    private const val EXTRA_QUEUE_MEDIA_IDS = "com.asuka.player.extra.QUEUE_MEDIA_IDS"

    fun applyPlaybackIdentity(
        intent: Intent,
        mediaId: String,
        queueMediaIds: List<String>,
    ) {
        intent.putExtra(EXTRA_MEDIA_ID, mediaId)
        intent.putStringArrayListExtra(
            EXTRA_QUEUE_MEDIA_IDS,
            ArrayList(normalizeQueueMediaIds(mediaId, queueMediaIds)),
        )
    }

    fun readTargetMediaId(intent: Intent): String? {
        return intent.getStringExtra(EXTRA_MEDIA_ID)
            ?.takeIf { it.isNotBlank() }
            ?: intent.data?.toString()
    }

    fun readEntries(intent: Intent): List<PlaybackQueueEntry> {
        val queueUris = read(intent)
        if (queueUris.isEmpty()) return emptyList()

        val currentMediaId = readTargetMediaId(intent)
        val storedQueueMediaIds = intent.getStringArrayListExtra(EXTRA_QUEUE_MEDIA_IDS).orEmpty()
        val queueMediaIds = if (storedQueueMediaIds.isNotEmpty()) {
            normalizeQueueMediaIds(currentMediaId, storedQueueMediaIds)
        } else {
            queueUris.map(Uri::toString)
        }

        return queueUris.mapIndexed { index, uri ->
            PlaybackQueueEntry(
                mediaId = queueMediaIds.getOrElse(index) { uri.toString() },
                uri = uri,
            )
        }
    }

    fun read(intent: Intent): List<Uri> {
        val clip: ClipData? = intent.clipData
        val clipUris = buildList {
            if (clip != null) {
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i).uri?.let { add(it) }
                }
            }
        }
        if (clipUris.isNotEmpty()) {
            val queue = clipUris.distinct().toMutableList()
            val dataUri = intent.data
            if (dataUri != null && dataUri !in queue) {
                queue.add(0, dataUri)
            }
            return queue
        }
        return listOfNotNull(intent.data)
    }

    private fun normalizeQueueMediaIds(
        currentMediaId: String?,
        queueMediaIds: List<String>,
    ): List<String> {
        val normalized = queueMediaIds
            .filter { it.isNotBlank() }
            .distinct()
            .toMutableList()
        val current = currentMediaId?.takeIf { it.isNotBlank() }
        if (normalized.isEmpty()) {
            return listOfNotNull(current)
        }
        if (current != null && current !in normalized) {
            normalized.add(0, current)
        }
        return normalized
    }
}

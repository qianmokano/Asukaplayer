package com.asuka.player.platform

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.asuka.player.contract.PlaybackQueueEntry
import java.util.ArrayList

data class PlaybackIntentPayload(
    val queueEntries: List<PlaybackQueueEntry>,
    val startIndex: Int,
) {
    init {
        require(queueEntries.isNotEmpty()) { "queueEntries must not be empty" }
        require(startIndex in queueEntries.indices) { "startIndex must point to a queue entry" }
    }

    val targetEntry: PlaybackQueueEntry
        get() = queueEntries[startIndex]
}

object PlaybackIntentPayloadCodec {
    private const val EXTRA_MEDIA_ID = "com.asuka.player.extra.MEDIA_ID"
    private const val EXTRA_QUEUE_MEDIA_IDS = "com.asuka.player.extra.QUEUE_MEDIA_IDS"
    private const val EXTRA_QUEUE_URIS = "com.asuka.player.extra.QUEUE_URIS"
    private const val EXTRA_START_INDEX = "com.asuka.player.extra.START_INDEX"

    fun fromExternalIntent(intent: Intent?): PlaybackIntentPayload? {
        val sourceIntent = intent ?: return null
        val dataUri = sourceIntent.data?.toString()
        val clipUris = readClipUris(sourceIntent)
        val extraStreamUris = readExtraStreamUris(sourceIntent)
        val targetUri = dataUri ?: clipUris.firstOrNull() ?: extraStreamUris.firstOrNull() ?: return null
        val queueUris = when {
            clipUris.isNotEmpty() -> normalizeQueueUris(targetUri, clipUris)
            extraStreamUris.isNotEmpty() -> normalizeQueueUris(targetUri, extraStreamUris)
            else -> listOf(targetUri)
        }
        return PlaybackIntentPayload(
            queueEntries = queueUris.map { uri ->
                PlaybackQueueEntry(mediaId = uri, uri = uri)
            },
            startIndex = queueUris.indexOf(targetUri).coerceAtLeast(0),
        )
    }

    fun fromSelection(
        targetMediaId: String,
        queueMediaIds: List<String>,
    ): PlaybackIntentPayload {
        val queue = normalizeQueueUris(targetMediaId, queueMediaIds)
        return PlaybackIntentPayload(
            queueEntries = queue.map { mediaId ->
                PlaybackQueueEntry(mediaId = mediaId, uri = mediaId)
            },
            startIndex = queue.indexOf(targetMediaId).coerceAtLeast(0),
        )
    }

    fun readPlaybackIntent(intent: Intent?): PlaybackIntentPayload? {
        val sourceIntent = intent ?: return null
        val storedQueueUris = sourceIntent.getStringArrayListExtra(EXTRA_QUEUE_URIS)
            .orEmpty()
            .filter { it.isNotBlank() }
        if (storedQueueUris.isNotEmpty()) {
            val storedQueueMediaIds = sourceIntent.getStringArrayListExtra(EXTRA_QUEUE_MEDIA_IDS).orEmpty()
            val queueEntries = storedQueueUris.mapIndexed { index, uri ->
                PlaybackQueueEntry(
                    mediaId = storedQueueMediaIds.getOrElse(index) { uri },
                    uri = uri,
                )
            }
            val requestedStartIndex = sourceIntent.getIntExtra(EXTRA_START_INDEX, 0)
            val startIndex = requestedStartIndex.coerceIn(0, queueEntries.lastIndex)
            return PlaybackIntentPayload(queueEntries, startIndex)
        }

        val queueUris = readRuntimeQueueUris(sourceIntent)
        if (queueUris.isEmpty()) return null
        val targetMediaId = sourceIntent.getStringExtra(EXTRA_MEDIA_ID)
            ?.takeIf { it.isNotBlank() }
            ?: sourceIntent.data?.toString()
            ?: queueUris.first()
        val storedQueueMediaIds = sourceIntent.getStringArrayListExtra(EXTRA_QUEUE_MEDIA_IDS)
            .orEmpty()
            .filter { it.isNotBlank() }
        val queueMediaIds = if (storedQueueMediaIds.isNotEmpty()) {
            normalizeQueueMediaIds(targetMediaId, storedQueueMediaIds)
        } else {
            queueUris
        }
        val queueEntries = queueUris.mapIndexed { index, uri ->
            PlaybackQueueEntry(
                mediaId = queueMediaIds.getOrElse(index) { uri },
                uri = uri,
            )
        }
        val startIndex = queueEntries.indexOfFirst { it.mediaId == targetMediaId }
            .takeIf { it >= 0 }
            ?: queueEntries.indexOfFirst { it.uri == sourceIntent.data?.toString() }
                .takeIf { it >= 0 }
            ?: 0
        return PlaybackIntentPayload(queueEntries, startIndex)
    }

    fun applyPlaybackPayload(
        intent: Intent,
        payload: PlaybackIntentPayload,
    ) {
        intent.putExtra(EXTRA_MEDIA_ID, payload.targetEntry.mediaId)
        intent.putExtra(EXTRA_START_INDEX, payload.startIndex)
        intent.putStringArrayListExtra(
            EXTRA_QUEUE_MEDIA_IDS,
            ArrayList(payload.queueEntries.map(PlaybackQueueEntry::mediaId)),
        )
        intent.putStringArrayListExtra(
            EXTRA_QUEUE_URIS,
            ArrayList(payload.queueEntries.map(PlaybackQueueEntry::uri)),
        )
    }

    fun buildClipData(payload: PlaybackIntentPayload): ClipData {
        val first = Uri.parse(payload.queueEntries.first().uri)
        return ClipData.newRawUri("queue", first).apply {
            payload.queueEntries.drop(1).forEach { entry ->
                addItem(ClipData.Item(Uri.parse(entry.uri)))
            }
        }
    }

    fun remapUri(
        payload: PlaybackIntentPayload,
        originalUri: Uri,
        replacementUri: Uri,
    ): PlaybackIntentPayload {
        val remappedEntries = payload.queueEntries.map { entry ->
            if (entry.uri == originalUri.toString()) {
                entry.copy(uri = replacementUri.toString())
            } else {
                entry
            }
        }
        return payload.copy(queueEntries = remappedEntries)
    }

    fun readRuntimeQueueUris(intent: Intent): List<String> {
        val clipUris = readClipUris(intent)
        if (clipUris.isNotEmpty()) {
            return normalizeQueueUris(intent.data?.toString(), clipUris)
        }
        return listOfNotNull(intent.data?.toString())
    }

    private fun readClipUris(intent: Intent): List<String> {
        val clipData = intent.clipData ?: return emptyList()
        return buildList {
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).uri?.toString()?.let(::add)
            }
        }.filter { it.isNotBlank() }.distinct()
    }

    private fun readExtraStreamUris(intent: Intent): List<String> {
        val multiple = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        }.orEmpty()
        if (multiple.isNotEmpty()) {
            return multiple.map(Uri::toString).filter { it.isNotBlank() }.distinct()
        }

        val single = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        return listOfNotNull(single?.toString()).filter { it.isNotBlank() }
    }

    private fun normalizeQueueUris(
        targetUri: String?,
        queueUris: List<String>,
    ): List<String> {
        val normalized = queueUris
            .filter { it.isNotBlank() }
            .distinct()
            .toMutableList()
        val target = targetUri?.takeIf { it.isNotBlank() }
        if (normalized.isEmpty()) {
            return listOfNotNull(target)
        }
        if (target != null && target !in normalized) {
            normalized.add(0, target)
        }
        return normalized
    }

    private fun normalizeQueueMediaIds(
        targetMediaId: String?,
        queueMediaIds: List<String>,
    ): List<String> {
        val normalized = queueMediaIds
            .filter { it.isNotBlank() }
            .distinct()
            .toMutableList()
        val target = targetMediaId?.takeIf { it.isNotBlank() }
        if (normalized.isEmpty()) {
            return listOfNotNull(target)
        }
        if (target != null && target !in normalized) {
            normalized.add(0, target)
        }
        return normalized
    }
}

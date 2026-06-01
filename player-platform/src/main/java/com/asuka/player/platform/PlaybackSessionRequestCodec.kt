package com.asuka.player.platform

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackSessionRequest
import java.util.ArrayList

object PlaybackSessionRequestCodec {
    private const val EXTRA_MEDIA_ID = "com.asuka.player.extra.MEDIA_ID"
    private const val EXTRA_QUEUE_MEDIA_IDS = "com.asuka.player.extra.QUEUE_MEDIA_IDS"
    private const val EXTRA_QUEUE_URIS = "com.asuka.player.extra.QUEUE_URIS"
    private const val EXTRA_QUEUE_PERSISTABLE = "com.asuka.player.extra.QUEUE_PERSISTABLE"
    private const val EXTRA_QUEUE_READABLE_IN_SESSION = "com.asuka.player.extra.QUEUE_READABLE_IN_SESSION"
    private const val EXTRA_START_INDEX = "com.asuka.player.extra.START_INDEX"
    private const val EXTRA_PLAYBACK_URI = "com.asuka.player.extra.PLAYBACK_URI"

    fun fromExternalIntent(intent: Intent?): PlaybackSessionRequest? {
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
        val persistable = hasPersistableReadGrant(sourceIntent)
        val hasReadGrant = hasReadGrant(sourceIntent)
        return PlaybackSessionRequest(
            queueEntries = queueUris.map { uri ->
                PlaybackQueueEntry(
                    mediaId = uri,
                    uri = uri,
                    persistable = persistable,
                    readableInSession = hasReadGrant || Uri.parse(uri).scheme != "content",
                )
            },
            startIndex = queueUris.indexOf(targetUri).coerceAtLeast(0),
            playbackUri = targetUri,
        )
    }

    fun fromSelection(
        targetMediaId: String,
        queueMediaIds: List<String>,
    ): PlaybackSessionRequest {
        val queue = normalizeQueueUris(targetMediaId, queueMediaIds)
        return PlaybackSessionRequest(
            queueEntries = queue.map { mediaId ->
                PlaybackQueueEntry(mediaId = mediaId, uri = mediaId)
            },
            startIndex = queue.indexOf(targetMediaId).coerceAtLeast(0),
            playbackUri = targetMediaId,
        )
    }

    fun fromQueueEntries(
        targetEntry: PlaybackQueueEntry,
        queueEntries: List<PlaybackQueueEntry>,
    ): PlaybackSessionRequest {
        val normalized = queueEntries
            .filter { it.mediaId.isNotBlank() && it.uri.isNotBlank() }
            .distinctBy(PlaybackQueueEntry::mediaId)
            .toMutableList()
        if (normalized.none { it.mediaId == targetEntry.mediaId }) {
            normalized.add(0, targetEntry)
        }
        return PlaybackSessionRequest(
            queueEntries = normalized,
            startIndex = normalized.indexOfFirst { it.mediaId == targetEntry.mediaId }
                .coerceAtLeast(0),
            playbackUri = targetEntry.uri,
        )
    }

    fun readPlaybackRequest(intent: Intent?): PlaybackSessionRequest? {
        val sourceIntent = intent ?: return null
        val storedQueueUris = sourceIntent.getStringArrayListExtra(EXTRA_QUEUE_URIS)
            .orEmpty()
            .filter { it.isNotBlank() }
        if (storedQueueUris.isNotEmpty()) {
            val storedQueueMediaIds = sourceIntent.getStringArrayListExtra(EXTRA_QUEUE_MEDIA_IDS).orEmpty()
            val storedPersistable = sourceIntent.getBooleanArrayExtra(EXTRA_QUEUE_PERSISTABLE)
            val storedReadableInSession = sourceIntent.getBooleanArrayExtra(EXTRA_QUEUE_READABLE_IN_SESSION)
            val readableInSession = hasReadGrant(sourceIntent)
            val queueEntries = storedQueueUris.mapIndexed { index, uri ->
                val persistable = storedPersistable?.getOrNull(index) ?: true
                PlaybackQueueEntry(
                    mediaId = storedQueueMediaIds.getOrElse(index) { uri },
                    uri = uri,
                    persistable = persistable,
                    readableInSession = storedReadableInSession?.getOrNull(index)
                        ?: (persistable || readableInSession || Uri.parse(uri).scheme != "content"),
                )
            }
            val requestedStartIndex = sourceIntent.getIntExtra(EXTRA_START_INDEX, 0)
            val startIndex = requestedStartIndex.coerceIn(0, queueEntries.lastIndex)
            val playbackUri = sourceIntent.getStringExtra(EXTRA_PLAYBACK_URI)
                ?.takeIf { it.isNotBlank() }
                ?: sourceIntent.data?.toString()
                ?: queueEntries[startIndex].uri
            return PlaybackSessionRequest(
                queueEntries = queueEntries,
                startIndex = startIndex,
                playbackUri = playbackUri,
            )
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
                persistable = true,
                readableInSession = true,
            )
        }
        val startIndex = queueEntries.indexOfFirst { it.mediaId == targetMediaId }
            .takeIf { it >= 0 }
            ?: queueEntries.indexOfFirst { it.uri == sourceIntent.data?.toString() }
                .takeIf { it >= 0 }
            ?: 0
        val playbackUri = sourceIntent.data?.toString()
            ?: queueEntries[startIndex].uri
        return PlaybackSessionRequest(
            queueEntries = queueEntries,
            startIndex = startIndex,
            playbackUri = playbackUri,
        )
    }

    fun applyPlaybackRequest(
        intent: Intent,
        request: PlaybackSessionRequest,
    ) {
        intent.putExtra(EXTRA_MEDIA_ID, request.targetEntry.mediaId)
        intent.putExtra(EXTRA_START_INDEX, request.startIndex)
        intent.putExtra(EXTRA_PLAYBACK_URI, request.playbackUri)
        intent.putStringArrayListExtra(
            EXTRA_QUEUE_MEDIA_IDS,
            ArrayList(request.queueEntries.map(PlaybackQueueEntry::mediaId)),
        )
        intent.putStringArrayListExtra(
            EXTRA_QUEUE_URIS,
            ArrayList(request.queueEntries.map(PlaybackQueueEntry::uri)),
        )
        intent.putExtra(
            EXTRA_QUEUE_PERSISTABLE,
            request.queueEntries.map(PlaybackQueueEntry::persistable).toBooleanArray(),
        )
        intent.putExtra(
            EXTRA_QUEUE_READABLE_IN_SESSION,
            request.queueEntries.map(PlaybackQueueEntry::readableInSession).toBooleanArray(),
        )
    }

    fun buildClipData(request: PlaybackSessionRequest): ClipData {
        val launchEntries = launchEntries(request)
        val first = Uri.parse(launchEntries.first().uri)
        return ClipData.newRawUri("queue", first).apply {
            launchEntries.drop(1).forEach { entry ->
                addItem(ClipData.Item(Uri.parse(entry.uri)))
            }
        }
    }

    fun remapPlaybackUri(
        request: PlaybackSessionRequest,
        replacementUri: Uri,
    ): PlaybackSessionRequest {
        return request.withPlaybackUri(replacementUri.toString())
    }

    fun readRuntimeQueueUris(intent: Intent): List<String> {
        val clipUris = readClipUris(intent)
        if (clipUris.isNotEmpty()) {
            return normalizeQueueUris(intent.data?.toString(), clipUris)
        }
        return listOfNotNull(intent.data?.toString())
    }

    private fun launchEntries(request: PlaybackSessionRequest): List<PlaybackQueueEntry> {
        return request.queueEntries.mapIndexed { index, entry ->
            if (index == request.startIndex) {
                entry.copy(uri = request.playbackUri)
            } else {
                entry
            }
        }
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

    private fun hasPersistableReadGrant(intent: Intent): Boolean {
        val flags = intent.flags
        return hasReadGrant(intent) && flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0
    }

    private fun hasReadGrant(intent: Intent): Boolean {
        return intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
    }
}

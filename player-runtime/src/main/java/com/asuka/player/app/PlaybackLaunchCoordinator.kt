package com.asuka.player.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.asuka.player.core.IntentQueueReader
import com.asuka.player.core.SeekFallbackCopier
import com.asuka.player.core.remapClipDataUri
import java.io.File

interface PlaybackUriResolver {
    fun resolveForPlayback(sourceUri: Uri): Uri
}

data class PlaybackLaunchRequest(
    val mediaUri: Uri,
    val clipData: ClipData?,
    val mediaId: String,
    val queueMediaIds: List<String>,
)

class PlaybackLaunchCoordinator(
    private val uriResolver: PlaybackUriResolver,
) {
    fun createLaunchRequest(
        mediaId: String,
        sourceIntent: Intent? = null,
        queueMediaIds: List<String> = emptyList(),
    ): PlaybackLaunchRequest {
        val sourceUri = Uri.parse(mediaId)
        val resolvedUri = uriResolver.resolveForPlayback(sourceUri)
        val queueClipData = sourceIntent?.clipData ?: buildQueueClipData(
            currentUri = sourceUri,
            queueMediaIds = queueMediaIds,
        )
        return PlaybackLaunchRequest(
            mediaUri = resolvedUri,
            clipData = remapClipDataUri(
                clipData = queueClipData,
                originalUri = sourceUri,
                replacementUri = resolvedUri,
            ),
            mediaId = mediaId,
            queueMediaIds = queueMediaIds.ifEmpty { readQueueMediaIds(queueClipData) },
        )
    }

    fun createPlaybackIntent(
        context: Context,
        activityClass: Class<*>,
        request: PlaybackLaunchRequest,
    ): Intent {
        return Intent(context, activityClass).apply {
            data = request.mediaUri
            clipData = request.clipData
            IntentQueueReader.applyPlaybackIdentity(
                intent = this,
                mediaId = request.mediaId,
                queueMediaIds = request.queueMediaIds,
            )
            if (request.mediaUri.scheme == "content" || request.clipData != null) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun buildQueueClipData(
        currentUri: Uri,
        queueMediaIds: List<String>,
    ): ClipData? {
        val queueUris = queueMediaIds
            .map(Uri::parse)
            .distinct()
            .toMutableList()
        if (currentUri !in queueUris) {
            queueUris.add(0, currentUri)
        }
        if (queueUris.isEmpty()) return null
        return ClipData.newRawUri("queue", queueUris.first()).apply {
            queueUris.drop(1).forEach { uri ->
                addItem(ClipData.Item(uri))
            }
        }
    }

    private fun readQueueMediaIds(clipData: ClipData?): List<String> {
        if (clipData == null) return emptyList()
        return buildList {
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).uri?.toString()?.let(::add)
            }
        }.distinct()
    }
}

class SeekAwarePlaybackUriResolver(
    private val contentResolver: android.content.ContentResolver,
    cacheDir: File,
) : PlaybackUriResolver {
    private val seekFallbackCopier = SeekFallbackCopier(contentResolver, cacheDir)

    override fun resolveForPlayback(sourceUri: Uri): Uri {
        if (sourceUri.scheme != "content") return sourceUri
        if (sourceUri.authority == MediaStore.AUTHORITY) return sourceUri
        if (isContentUriSeekable(sourceUri)) return sourceUri
        return seekFallbackCopier.copy(sourceUri) ?: sourceUri
    }

    private fun isContentUriSeekable(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                try {
                    Os.lseek(pfd.fileDescriptor, 0L, OsConstants.SEEK_CUR)
                    true
                } catch (_: ErrnoException) {
                    false
                }
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }
}

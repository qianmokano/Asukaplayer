package com.asuka.player.runtime

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.platform.PlaybackIntentPayload
import com.asuka.player.platform.PlaybackIntentPayloadCodec
import com.asuka.player.platform.SeekFallbackCopier
import java.io.File

interface PlaybackUriResolver {
    fun resolveForPlayback(sourceUri: Uri): Uri
}

data class PlaybackLaunchRequest(
    val payload: PlaybackIntentPayload,
) {
    val mediaUri: Uri
        get() = Uri.parse(payload.targetEntry.uri)
    val clipData: ClipData
        get() = PlaybackIntentPayloadCodec.buildClipData(payload)
    val mediaId: String
        get() = payload.targetEntry.mediaId
    val queueMediaIds: List<String>
        get() = payload.queueEntries.map(PlaybackQueueEntry::mediaId)
}

class PlaybackLaunchCoordinator(
    private val uriResolver: PlaybackUriResolver,
) {
    fun createLaunchRequest(
        payload: PlaybackIntentPayload,
    ): PlaybackLaunchRequest {
        val sourceUri = Uri.parse(payload.targetEntry.uri)
        val resolvedUri = uriResolver.resolveForPlayback(sourceUri)
        return PlaybackLaunchRequest(
            payload = PlaybackIntentPayloadCodec.remapUri(
                payload = payload,
                originalUri = sourceUri,
                replacementUri = resolvedUri,
            ),
        )
    }

    fun createPlaybackIntent(
        context: Context,
        activityClass: Class<*>,
        request: PlaybackLaunchRequest,
    ): Intent {
        return Intent(context, activityClass).apply {
            data = request.mediaUri
            clipData = PlaybackIntentPayloadCodec.buildClipData(request.payload)
            PlaybackIntentPayloadCodec.applyPlaybackPayload(this, request.payload)
            if (request.payload.queueEntries.any { Uri.parse(it.uri).scheme == "content" }) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
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

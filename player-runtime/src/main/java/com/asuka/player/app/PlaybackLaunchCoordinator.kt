package com.asuka.player.runtime

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.platform.PlaybackSessionRequestCodec
import com.asuka.player.platform.SeekFallbackCopier
import java.io.File

interface PlaybackUriResolver {
    fun resolveForPlayback(sourceUri: Uri): Uri
}

class PlaybackLaunchCoordinator(
    private val uriResolver: PlaybackUriResolver,
) {
    fun prepareRequest(
        request: PlaybackSessionRequest,
    ): PlaybackSessionRequest {
        val sourceUri = Uri.parse(request.originalUri)
        val resolvedUri = uriResolver.resolveForPlayback(sourceUri)
        return PlaybackSessionRequestCodec.remapPlaybackUri(
            request = request,
            replacementUri = resolvedUri,
        )
    }

    fun createPlaybackIntent(
        context: Context,
        activityClass: Class<*>,
        request: PlaybackSessionRequest,
    ): Intent {
        return Intent(context, activityClass).apply {
            data = Uri.parse(request.playbackUri)
            clipData = PlaybackSessionRequestCodec.buildClipData(request)
            PlaybackSessionRequestCodec.applyPlaybackRequest(this, request)
            if (request.queueEntries.any { Uri.parse(it.uri).scheme == "content" }) {
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

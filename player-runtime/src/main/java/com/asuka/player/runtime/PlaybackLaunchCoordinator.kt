package com.asuka.player.runtime

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.platform.PlaybackSessionRequestCodec

interface PlaybackUriResolver {
    fun resolveForPlayback(sourceUri: Uri): Uri
}

object PassthroughPlaybackUriResolver : PlaybackUriResolver {
    override fun resolveForPlayback(sourceUri: Uri): Uri = sourceUri
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
            if (request.hasContentUris()) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    private fun PlaybackSessionRequest.hasContentUris(): Boolean {
        return Uri.parse(playbackUri).scheme == "content" ||
            queueEntries.any { Uri.parse(it.uri).scheme == "content" }
    }
}

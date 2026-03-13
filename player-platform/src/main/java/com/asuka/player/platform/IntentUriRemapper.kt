package com.asuka.player.platform

import android.content.ClipData
import android.content.Intent
import android.net.Uri

fun remapClipDataUri(
    clipData: ClipData?,
    originalUri: Uri,
    replacementUri: Uri,
): ClipData? {
    if (clipData == null || clipData.itemCount == 0) return null

    var remapped: ClipData? = null
    for (index in 0 until clipData.itemCount) {
        val rawUri = clipData.getItemAt(index).uri ?: continue
        val mappedUri = if (rawUri == originalUri) replacementUri else rawUri
        remapped = if (remapped == null) {
            ClipData.newRawUri(clipData.description.label, mappedUri)
        } else {
            remapped.apply { addItem(ClipData.Item(mappedUri)) }
        }
    }
    return remapped
}

fun copyIntentWithRemappedUri(
    intent: Intent?,
    originalUri: Uri,
    replacementUri: Uri,
): Intent {
    return (intent?.let(::Intent) ?: Intent()).apply {
        val request = PlaybackSessionRequestCodec.readPlaybackRequest(intent)
        if (request != null) {
            val remappedRequest = if (request.playbackUri == originalUri.toString()) {
                PlaybackSessionRequestCodec.remapPlaybackUri(
                    request = request,
                    replacementUri = replacementUri,
                )
            } else {
                request
            }
            PlaybackSessionRequestCodec.applyPlaybackRequest(this, remappedRequest)
            data = Uri.parse(remappedRequest.playbackUri)
            clipData = PlaybackSessionRequestCodec.buildClipData(remappedRequest)
        } else {
            data = replacementUri
            clipData = remapClipDataUri(intent?.clipData, originalUri, replacementUri)
        }
    }
}

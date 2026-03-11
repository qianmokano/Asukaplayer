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
        val payload = PlaybackIntentPayloadCodec.readPlaybackIntent(intent)
        if (payload != null) {
            val remappedPayload = PlaybackIntentPayloadCodec.remapUri(
                payload = payload,
                originalUri = originalUri,
                replacementUri = replacementUri,
            )
            PlaybackIntentPayloadCodec.applyPlaybackPayload(this, remappedPayload)
            data = Uri.parse(remappedPayload.targetEntry.uri)
            clipData = PlaybackIntentPayloadCodec.buildClipData(remappedPayload)
        } else {
            data = replacementUri
            clipData = remapClipDataUri(intent?.clipData, originalUri, replacementUri)
        }
    }
}

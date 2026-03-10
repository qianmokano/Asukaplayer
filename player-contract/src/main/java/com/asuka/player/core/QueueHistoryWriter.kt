package com.asuka.player.core

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

class QueueHistoryWriter(
    private val store: QueueHistoryStore,
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val uri = resolveHistoryUri(mediaItem)
        if (mediaItem != null && uri == null) {
            // Remote/stream media without a local URI is expected; silently skip history.
            Log.v(TAG, "Media item '${mediaItem.mediaId}' has no local URI, skipping history")
        }
        uri?.let { store.push(it) }
    }

    private fun resolveHistoryUri(mediaItem: MediaItem?): Uri? {
        val stableMediaUri = mediaItem?.mediaId
            ?.takeIf { it.isNotBlank() }
            ?.let(Uri::parse)
            ?.takeIf { it.scheme != null }
        return stableMediaUri ?: mediaItem?.localConfiguration?.uri
    }

    companion object {
        private const val TAG = "QueueHistoryWriter"
    }
}

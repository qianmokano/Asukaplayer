package com.asuka.player.platform

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.asuka.player.contract.QueueHistoryStore

class QueueHistoryWriter(
    private val store: QueueHistoryStore,
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val mediaId = resolveHistoryMediaId(mediaItem)
        if (mediaItem != null && mediaId == null) {
            Log.v(TAG, "Media item '${mediaItem.mediaId}' has no local URI, skipping history")
        }
        mediaId?.let(store::push)
    }

    private fun resolveHistoryMediaId(mediaItem: MediaItem?): String? {
        val stableMediaId = mediaItem?.mediaId
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { runCatching { Uri.parse(it) }.getOrNull()?.scheme != null }
        return stableMediaId ?: mediaItem?.localConfiguration?.uri?.toString()
    }

    companion object {
        private const val TAG = "QueueHistoryWriter"
    }
}

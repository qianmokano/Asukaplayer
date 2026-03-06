package com.asuka.player.core

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.asuka.player.data.QueueHistoryStore

class QueueHistoryWriter(
    private val store: QueueHistoryStore,
) : Player.Listener {

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val uri = mediaItem?.localConfiguration?.uri
        if (mediaItem != null && uri == null) {
            Log.w(TAG, "Media item '${mediaItem.mediaId}' has no local URI, skipping history")
        }
        uri?.let { store.push(it) }
    }

    companion object {
        private const val TAG = "QueueHistoryWriter"
    }
}

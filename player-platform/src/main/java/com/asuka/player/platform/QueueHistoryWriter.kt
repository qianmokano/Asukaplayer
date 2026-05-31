package com.asuka.player.platform

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.asuka.player.contract.QueueHistoryStore
import kotlinx.coroutines.CoroutineDispatcher

class QueueHistoryWriter(
    private val store: QueueHistoryStore,
    writeDispatcher: CoroutineDispatcher? = null,
) : Player.Listener {
    private val writeQueue = SerialTaskQueue(
        dispatcher = writeDispatcher ?: kotlinx.coroutines.Dispatchers.IO.limitedParallelism(1),
        tag = TAG,
    )

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val mediaId = resolveHistoryMediaId(mediaItem)
        if (mediaItem != null && mediaId == null) {
            Log.v(TAG, "Media item '${mediaItem.mediaId}' has no local URI, skipping history")
        }
        if (mediaId?.isTransientPlaybackMediaId() == true) {
            Log.v(TAG, "Media item is transient, skipping history")
            return
        }
        mediaId?.let { resolvedId ->
            dispatchWrite {
                store.push(resolvedId)
            }
        }
    }

    fun close() {
        writeQueue.close()
    }

    suspend fun awaitIdle() {
        writeQueue.awaitIdle()
    }

    private fun resolveHistoryMediaId(mediaItem: MediaItem?): String? {
        val stableMediaId = mediaItem?.mediaId
            ?.takeIf { it.isNotBlank() }
        return stableMediaId ?: mediaItem?.localConfiguration?.uri?.toString()
    }

    companion object {
        private const val TAG = "QueueHistoryWriter"
    }

    private fun dispatchWrite(block: suspend () -> Unit) {
        writeQueue.dispatch(block)
    }
}

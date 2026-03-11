package com.asuka.player.platform

import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.asuka.player.contract.QueueHistoryStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QueueHistoryWriter(
    private val store: QueueHistoryStore,
    writeDispatcher: CoroutineDispatcher? = null,
) : Player.Listener {
    private val writeMutex = Mutex()
    private val writeScope = writeDispatcher?.let {
        CoroutineScope(SupervisorJob() + it)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val mediaId = resolveHistoryMediaId(mediaItem)
        if (mediaItem != null && mediaId == null) {
            Log.v(TAG, "Media item '${mediaItem.mediaId}' has no local URI, skipping history")
        }
        mediaId?.let { resolvedId ->
            dispatchWrite {
                store.push(resolvedId)
            }
        }
    }

    fun close() {
        writeScope?.cancel()
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

    private fun dispatchWrite(block: suspend () -> Unit) {
        val scope = writeScope
        if (scope == null) {
            runBlocking {
                block()
            }
            return
        }
        scope.launch {
            writeMutex.withLock {
                block()
            }
        }
    }
}

package com.asuka.player.engine.service

import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.QueueHistoryWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal interface PlaybackStateShutdownHandle {
    fun enqueueFinalPosition(): Boolean

    suspend fun awaitIdle()

    fun close()
}

internal interface QueueHistoryShutdownHandle {
    suspend fun awaitIdle()

    fun close()
}

internal fun PlaybackStateWriter.asShutdownHandle(): PlaybackStateShutdownHandle {
    return object : PlaybackStateShutdownHandle {
        override fun enqueueFinalPosition(): Boolean {
            return this@asShutdownHandle.flushCurrentPosition()
        }

        override suspend fun awaitIdle() {
            this@asShutdownHandle.awaitIdle()
        }

        override fun close() {
            this@asShutdownHandle.close()
        }
    }
}

internal fun QueueHistoryWriter.asShutdownHandle(): QueueHistoryShutdownHandle {
    return object : QueueHistoryShutdownHandle {
        override suspend fun awaitIdle() {
            this@asShutdownHandle.awaitIdle()
        }

        override fun close() {
            this@asShutdownHandle.close()
        }
    }
}

/**
 * Flushes the final position and gives both persistence queues a short chance
 * to drain before closing them.
 */
internal class PlaybackPersistenceShutdownCoordinator(
    private val drainTimeoutMs: Long = DEFAULT_DRAIN_TIMEOUT_MS,
) {
    fun drainAndCloseAsync(
        scope: CoroutineScope,
        playbackState: PlaybackStateShutdownHandle?,
        history: QueueHistoryShutdownHandle?,
    ): Job {
        playbackState?.enqueueFinalPosition()
        return scope.launch(Dispatchers.IO) {
            drainAndCloseEnqueued(playbackState = playbackState, history = history)
        }
    }

    suspend fun drainAndClose(
        playbackState: PlaybackStateShutdownHandle?,
        history: QueueHistoryShutdownHandle?,
    ): Boolean {
        playbackState?.enqueueFinalPosition()
        return drainAndCloseEnqueued(playbackState = playbackState, history = history)
    }

    private suspend fun drainAndCloseEnqueued(
        playbackState: PlaybackStateShutdownHandle?,
        history: QueueHistoryShutdownHandle?,
    ): Boolean {
        if (playbackState == null && history == null) return true
        val drained = withContext(NonCancellable) {
            withTimeoutOrNull(drainTimeoutMs) {
                coroutineScope {
                    val playbackDrain = async {
                        playbackState?.awaitIdle()
                    }
                    val historyDrain = async {
                        history?.awaitIdle()
                    }
                    playbackDrain.await()
                    historyDrain.await()
                }
                true
            } ?: false
        }
        playbackState?.close()
        history?.close()
        return drained
    }

    private companion object {
        const val DEFAULT_DRAIN_TIMEOUT_MS = 250L
    }
}

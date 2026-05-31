package com.asuka.player.engine.service

import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.QueueHistoryWriter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

internal interface PlaybackStateShutdownHandle {
    suspend fun flushCurrentPosition()

    suspend fun awaitIdle()

    fun close()
}

internal interface QueueHistoryShutdownHandle {
    suspend fun awaitIdle()

    fun close()
}

internal fun PlaybackStateWriter.asShutdownHandle(): PlaybackStateShutdownHandle {
    return object : PlaybackStateShutdownHandle {
        override suspend fun flushCurrentPosition() {
            this@asShutdownHandle.flushCurrentPositionAndAwait()
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
    suspend fun drainAndClose(
        playbackState: PlaybackStateShutdownHandle?,
        history: QueueHistoryShutdownHandle?,
    ): Boolean {
        if (playbackState == null && history == null) return true
        val drained = withTimeoutOrNull(drainTimeoutMs) {
            coroutineScope {
                val playbackDrain = async {
                    playbackState?.flushCurrentPosition()
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
        playbackState?.close()
        history?.close()
        return drained
    }

    private companion object {
        const val DEFAULT_DRAIN_TIMEOUT_MS = 250L
    }
}

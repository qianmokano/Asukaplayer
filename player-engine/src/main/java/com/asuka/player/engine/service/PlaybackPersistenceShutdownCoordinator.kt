package com.asuka.player.engine.service

import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.QueueHistoryWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

internal interface PlaybackStateShutdownHandle {
    fun flushCurrentPosition()

    suspend fun awaitIdle()

    fun close()
}

internal interface QueueHistoryShutdownHandle {
    suspend fun awaitIdle()

    fun close()
}

internal fun PlaybackStateWriter.asShutdownHandle(): PlaybackStateShutdownHandle {
    return object : PlaybackStateShutdownHandle {
        override fun flushCurrentPosition() {
            this@asShutdownHandle.flushCurrentPosition()
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

internal class PlaybackPersistenceShutdownCoordinator(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val onTimeout: () -> Unit = {},
) {
    fun drainAndClose(
        playbackState: PlaybackStateShutdownHandle?,
        history: QueueHistoryShutdownHandle?,
    ) {
        if (playbackState == null && history == null) return
        playbackState?.flushCurrentPosition()
        runBlocking(dispatcher) {
            val drained = withTimeoutOrNull(timeoutMs) {
                playbackState?.awaitIdle()
                history?.awaitIdle()
                true
            } ?: false
            if (!drained) onTimeout()
            playbackState?.close()
            history?.close()
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 1_000L
    }
}

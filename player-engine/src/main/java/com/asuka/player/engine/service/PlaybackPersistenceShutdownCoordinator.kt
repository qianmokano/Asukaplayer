package com.asuka.player.engine.service

import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.QueueHistoryWriter

internal interface PlaybackStateShutdownHandle {
    fun flushCurrentPosition()

    fun close()
}

internal interface QueueHistoryShutdownHandle {
    fun close()
}

internal fun PlaybackStateWriter.asShutdownHandle(): PlaybackStateShutdownHandle {
    return object : PlaybackStateShutdownHandle {
        override fun flushCurrentPosition() {
            this@asShutdownHandle.flushCurrentPosition()
        }

        override fun close() {
            this@asShutdownHandle.close()
        }
    }
}

internal fun QueueHistoryWriter.asShutdownHandle(): QueueHistoryShutdownHandle {
    return object : QueueHistoryShutdownHandle {
        override fun close() {
            this@asShutdownHandle.close()
        }
    }
}

/**
 * Non-blocking persistence shutdown: flushes the final position, then closes
 * both write queues. Remaining enqueued writes are processed asynchronously
 * by each queue's own coroutine scope — the main thread is never blocked.
 */
internal class PlaybackPersistenceShutdownCoordinator {
    fun drainAndClose(
        playbackState: PlaybackStateShutdownHandle?,
        history: QueueHistoryShutdownHandle?,
    ) {
        if (playbackState == null && history == null) return
        playbackState?.flushCurrentPosition()
        playbackState?.close()
        history?.close()
    }
}

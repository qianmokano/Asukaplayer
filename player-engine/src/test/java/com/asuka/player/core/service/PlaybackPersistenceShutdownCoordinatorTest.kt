package com.asuka.player.engine.service

import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackPersistenceShutdownCoordinatorTest {

    @Test
    fun drainAndClose_flushesAndClosesAfterQueuesDrain() = runBlocking {
        val playback = FakePlaybackStateShutdownHandle()
        val history = FakeQueueHistoryShutdownHandle()
        val coordinator = PlaybackPersistenceShutdownCoordinator(
            dispatcher = Dispatchers.Unconfined,
            timeoutMs = 100,
        )

        coordinator.drainAndClose(playbackState = playback, history = history)

        playback.closed.await()
        history.closed.await()

        assertEquals(1, playback.flushCount)
        assertEquals(1, playback.awaitIdleCount)
        assertEquals(1, playback.closeCount)
        assertEquals(1, history.awaitIdleCount)
        assertEquals(1, history.closeCount)
    }

    @Test
    fun drainAndClose_timesOutInBackgroundWithoutBlockingCaller() = runBlocking {
        val playback = FakePlaybackStateShutdownHandle(awaitDelayMs = 200)
        val history = FakeQueueHistoryShutdownHandle(awaitDelayMs = 200)
        val timeoutSignal = CompletableDeferred<Unit>()
        val coordinator = PlaybackPersistenceShutdownCoordinator(
            dispatcher = Dispatchers.Default,
            timeoutMs = 50,
            onTimeout = { timeoutSignal.complete(Unit) },
        )

        val elapsedMs = measureTimeMillis {
            coordinator.drainAndClose(playbackState = playback, history = history)
        }

        playback.closed.await()
        history.closed.await()
        timeoutSignal.await()

        assertTrue(elapsedMs < 100, "Expected non-blocking shutdown scheduling, but took ${elapsedMs}ms")
        assertEquals(1, playback.flushCount)
        assertEquals(1, playback.closeCount)
        assertEquals(1, history.closeCount)
    }
}

private class FakePlaybackStateShutdownHandle(
    private val awaitDelayMs: Long = 0L,
) : PlaybackStateShutdownHandle {
    var flushCount: Int = 0
    var awaitIdleCount: Int = 0
    var closeCount: Int = 0
    val closed = CompletableDeferred<Unit>()

    override fun flushCurrentPosition() {
        flushCount += 1
    }

    override suspend fun awaitIdle() {
        awaitIdleCount += 1
        if (awaitDelayMs > 0L) {
            delay(awaitDelayMs)
        }
    }

    override fun close() {
        closeCount += 1
        closed.complete(Unit)
    }
}

private class FakeQueueHistoryShutdownHandle(
    private val awaitDelayMs: Long = 0L,
) : QueueHistoryShutdownHandle {
    var awaitIdleCount: Int = 0
    var closeCount: Int = 0
    val closed = CompletableDeferred<Unit>()

    override suspend fun awaitIdle() {
        awaitIdleCount += 1
        if (awaitDelayMs > 0L) {
            delay(awaitDelayMs)
        }
    }

    override fun close() {
        closeCount += 1
        closed.complete(Unit)
    }
}

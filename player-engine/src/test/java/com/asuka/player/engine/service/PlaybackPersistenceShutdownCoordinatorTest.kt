package com.asuka.player.engine.service

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackPersistenceShutdownCoordinatorTest {

    @Test
    fun drainAndClose_flushesAwaitsAndClosesBothHandles() = runBlocking {
        val playback = FakePlaybackStateShutdownHandle()
        val history = FakeQueueHistoryShutdownHandle()
        val coordinator = PlaybackPersistenceShutdownCoordinator()

        val drained = coordinator.drainAndClose(playbackState = playback, history = history)

        assertTrue(drained)
        assertEquals(1, playback.enqueueCount)
        assertEquals(1, playback.awaitCount)
        assertEquals(1, playback.closeCount)
        assertEquals(1, history.awaitCount)
        assertEquals(1, history.closeCount)
    }

    @Test
    fun drainAndClose_skipsWhenBothHandlesNull() = runBlocking {
        val coordinator = PlaybackPersistenceShutdownCoordinator()

        assertTrue(coordinator.drainAndClose(playbackState = null, history = null))
    }

    @Test
    fun drainAndClose_handlesPartialNulls() = runBlocking {
        val playback = FakePlaybackStateShutdownHandle()
        val coordinator = PlaybackPersistenceShutdownCoordinator()

        val drained = coordinator.drainAndClose(playbackState = playback, history = null)

        assertTrue(drained)
        assertEquals(1, playback.enqueueCount)
        assertEquals(1, playback.awaitCount)
        assertEquals(1, playback.closeCount)
    }

    @Test
    fun drainAndClose_closesHandlesAfterTimeout() = runBlocking {
        val playback = FakePlaybackStateShutdownHandle()
        playback.awaitGate = CompletableDeferred()
        playback.awaitStarted = CompletableDeferred()
        val history = FakeQueueHistoryShutdownHandle()
        val coordinator = PlaybackPersistenceShutdownCoordinator(drainTimeoutMs = 250L)

        val drain = async {
            coordinator.drainAndClose(playbackState = playback, history = history)
        }
        val started = withTimeoutOrNull(5_000L) {
            playback.awaitStarted?.await()
            true
        } ?: false
        assertTrue(started, "Timed out waiting for playback drain to start")
        val drained = drain.await()

        assertFalse(drained)
        assertEquals(1, playback.enqueueCount)
        assertEquals(1, playback.awaitCount)
        assertEquals(1, playback.closeCount)
        assertEquals(1, history.closeCount)
    }
}

private class FakePlaybackStateShutdownHandle : PlaybackStateShutdownHandle {
    var enqueueCount: Int = 0
    var awaitCount: Int = 0
    var closeCount: Int = 0
    var awaitGate: CompletableDeferred<Unit>? = null
    var awaitStarted: CompletableDeferred<Unit>? = null

    override fun enqueueFinalPosition(): Boolean {
        enqueueCount += 1
        return true
    }

    override suspend fun awaitIdle() {
        awaitCount += 1
        awaitStarted?.complete(Unit)
        awaitGate?.await()
    }

    override fun close() {
        closeCount += 1
    }
}

private class FakeQueueHistoryShutdownHandle : QueueHistoryShutdownHandle {
    var awaitCount: Int = 0
    var closeCount: Int = 0

    override suspend fun awaitIdle() {
        awaitCount += 1
    }

    override fun close() {
        closeCount += 1
    }
}

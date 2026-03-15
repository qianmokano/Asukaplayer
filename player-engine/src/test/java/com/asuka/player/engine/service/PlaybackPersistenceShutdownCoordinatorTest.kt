package com.asuka.player.engine.service

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaybackPersistenceShutdownCoordinatorTest {

    @Test
    fun drainAndClose_flushesAndClosesBothHandles() {
        val playback = FakePlaybackStateShutdownHandle()
        val history = FakeQueueHistoryShutdownHandle()
        val coordinator = PlaybackPersistenceShutdownCoordinator()

        coordinator.drainAndClose(playbackState = playback, history = history)

        assertEquals(1, playback.flushCount)
        assertEquals(1, playback.closeCount)
        assertEquals(1, history.closeCount)
    }

    @Test
    fun drainAndClose_skipsWhenBothHandlesNull() {
        val coordinator = PlaybackPersistenceShutdownCoordinator()

        coordinator.drainAndClose(playbackState = null, history = null)
    }

    @Test
    fun drainAndClose_handlesPartialNulls() {
        val playback = FakePlaybackStateShutdownHandle()
        val coordinator = PlaybackPersistenceShutdownCoordinator()

        coordinator.drainAndClose(playbackState = playback, history = null)

        assertEquals(1, playback.flushCount)
        assertEquals(1, playback.closeCount)
    }
}

private class FakePlaybackStateShutdownHandle : PlaybackStateShutdownHandle {
    var flushCount: Int = 0
    var closeCount: Int = 0

    override fun flushCurrentPosition() {
        flushCount += 1
    }

    override fun close() {
        closeCount += 1
    }
}

private class FakeQueueHistoryShutdownHandle : QueueHistoryShutdownHandle {
    var closeCount: Int = 0

    override fun close() {
        closeCount += 1
    }
}

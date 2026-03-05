package com.asuka.player.core

import androidx.media3.common.Player
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackStateWriterTest {

    @Test
    fun shouldSavePositionOnPause_skipsBufferingIdleEnded() {
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_BUFFERING))
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_IDLE))
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_ENDED))
    }

    @Test
    fun shouldSavePositionOnPause_savesWhenReadyAndNotPlaying() {
        assertTrue(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = false, playbackState = Player.STATE_READY))
    }

    @Test
    fun shouldSavePositionOnPause_neverSavesWhenPlaying() {
        assertFalse(PlaybackStateWriter.shouldSavePositionOnPause(isPlaying = true, playbackState = Player.STATE_READY))
    }
}


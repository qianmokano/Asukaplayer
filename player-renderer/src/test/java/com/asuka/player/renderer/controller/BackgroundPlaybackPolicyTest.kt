package com.asuka.player.renderer.controller

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundPlaybackPolicyTest {

    @Test
    fun shouldRetainSession_fromConnectionRetentionAlone_withoutKeepingPlaybackActive() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = true,
            autoBackgroundPlaybackEnabled = false,
        )

        assertTrue(policy.shouldRetainSession())
        assertFalse(policy.shouldKeepPlaybackActive())
    }

    @Test
    fun shouldRetainSession_forManualBackgroundPlaybackRequest() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = false,
            autoBackgroundPlaybackEnabled = false,
        )

        assertFalse(policy.shouldRetainSession())
        policy.requestBackgroundPlayback()
        assertTrue(policy.shouldRetainSession())
        assertTrue(policy.shouldKeepPlaybackActive())
        policy.clearManualBackgroundPlaybackRequest()
        assertFalse(policy.shouldRetainSession())
        assertFalse(policy.shouldKeepPlaybackActive())
    }

    @Test
    fun shouldRetainSession_whileInPictureInPicture() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = false,
            autoBackgroundPlaybackEnabled = false,
        )

        policy.setPictureInPicture(true)
        assertTrue(policy.shouldRetainSession())
        assertTrue(policy.shouldKeepPlaybackActive())
        policy.setPictureInPicture(false)
        assertFalse(policy.shouldRetainSession())
        assertFalse(policy.shouldKeepPlaybackActive())
    }

    @Test
    fun shouldRetainSession_whenAutoBackgroundPlaybackEnabled() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = true,
            autoBackgroundPlaybackEnabled = true,
        )

        assertTrue(policy.shouldRetainSession())
        assertTrue(policy.shouldKeepPlaybackActive())
    }

    @Test
    fun shouldNotKeepPlaybackActive_whenAutoBackgroundPlaybackEnabledWithoutConnectionRetention() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = false,
            autoBackgroundPlaybackEnabled = true,
        )

        assertFalse(policy.shouldRetainSession())
        assertFalse(policy.shouldKeepPlaybackActive())
    }
}

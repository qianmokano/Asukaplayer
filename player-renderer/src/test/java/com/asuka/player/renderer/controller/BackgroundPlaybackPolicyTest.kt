package com.asuka.player.renderer.controller

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundPlaybackPolicyTest {

    @Test
    fun shouldNotRetainSession_fromConnectionRetentionAlone() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = true,
            autoBackgroundPlaybackEnabled = false,
        )

        assertFalse(policy.shouldRetainSession())
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
        policy.clearManualBackgroundPlaybackRequest()
        assertFalse(policy.shouldRetainSession())
    }

    @Test
    fun shouldRetainSession_whileInPictureInPicture() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = false,
            autoBackgroundPlaybackEnabled = false,
        )

        policy.setPictureInPicture(true)
        assertTrue(policy.shouldRetainSession())
        policy.setPictureInPicture(false)
        assertFalse(policy.shouldRetainSession())
    }

    @Test
    fun shouldRetainSession_whenAutoBackgroundPlaybackEnabled() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = true,
            autoBackgroundPlaybackEnabled = true,
        )

        assertTrue(policy.shouldRetainSession())
    }

    @Test
    fun shouldNotRetainSession_whenAutoBackgroundPlaybackEnabledWithoutConnectionRetention() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = false,
            autoBackgroundPlaybackEnabled = true,
        )

        assertFalse(policy.shouldRetainSession())
    }
}

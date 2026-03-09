package com.asuka.player.ui.controller

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundPlaybackPolicyTest {

    @Test
    fun shouldRetainSession_whenConnectionRetentionEnabled() {
        val policy = BackgroundPlaybackPolicy(
            retainControllerConnection = true,
            autoBackgroundPlaybackEnabled = false,
        )

        assertTrue(policy.shouldRetainSession())
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
            retainControllerConnection = false,
            autoBackgroundPlaybackEnabled = true,
        )

        assertTrue(policy.shouldRetainSession())
    }
}

package com.asuka.player.ui.activity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaybackPictureInPictureControllerTest {

    @Test
    fun resolveAspectRatio_returnsFallbackForTallVideo() {
        val ratio = PlaybackPictureInPictureController.resolveAspectRatio(100, 400)

        assertEquals(PictureInPictureAspectRatio(1, 2), ratio)
    }

    @Test
    fun resolveAspectRatio_returnsFallbackForWideVideo() {
        val ratio = PlaybackPictureInPictureController.resolveAspectRatio(400, 100)

        assertEquals(PictureInPictureAspectRatio(239, 100), ratio)
    }

    @Test
    fun resolveAspectRatio_returnsNullForInvalidSize() {
        val ratio = PlaybackPictureInPictureController.resolveAspectRatio(0, 200)

        assertNull(ratio)
    }
}

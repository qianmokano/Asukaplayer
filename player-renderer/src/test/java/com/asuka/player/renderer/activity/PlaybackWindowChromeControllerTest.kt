package com.asuka.player.renderer.activity

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaybackWindowChromeControllerTest {

    @Test
    fun resolveNextOrientation_togglesLandscapeToPortrait() {
        val result = PlaybackWindowChromeController.resolveNextOrientation(
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            currentOrientation = Configuration.ORIENTATION_LANDSCAPE,
        )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, result)
    }

    @Test
    fun resolveNextOrientation_usesCurrentOrientationWhenRequestIsUnspecified() {
        val result = PlaybackWindowChromeController.resolveNextOrientation(
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            currentOrientation = Configuration.ORIENTATION_PORTRAIT,
        )

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, result)
    }
}

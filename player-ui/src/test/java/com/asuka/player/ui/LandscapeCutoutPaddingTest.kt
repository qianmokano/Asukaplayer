package com.asuka.player.ui

import android.content.res.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals

class LandscapeCutoutPaddingTest {

    @Test
    fun resolveLandscapeCutoutPadding_returnsZeroOutsideLandscape() {
        val result = resolveLandscapeCutoutPadding(
            orientation = Configuration.ORIENTATION_PORTRAIT,
            leftCutoutPx = 96,
            rightCutoutPx = 0,
            statusBarHeightPx = 48,
        )

        assertEquals(LandscapeCutoutPaddingPx(), result)
    }

    @Test
    fun resolveLandscapeCutoutPadding_usesStatusBarHeightForLeftCutoutClearance() {
        val result = resolveLandscapeCutoutPadding(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            leftCutoutPx = 24,
            rightCutoutPx = 0,
            statusBarHeightPx = 32,
        )

        assertEquals(LandscapeCutoutPaddingPx(left = 32, right = 0), result)
    }

    @Test
    fun resolveLandscapeCutoutPadding_tracksReverseLandscapeByPaddingRightSide() {
        val result = resolveLandscapeCutoutPadding(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            leftCutoutPx = 0,
            rightCutoutPx = 26,
            statusBarHeightPx = 32,
        )

        assertEquals(LandscapeCutoutPaddingPx(left = 0, right = 32), result)
    }

    @Test
    fun resolveLandscapeCutoutPadding_keepsLargerPlatformInsetWhenCutoutExceedsStatusBar() {
        val result = resolveLandscapeCutoutPadding(
            orientation = Configuration.ORIENTATION_LANDSCAPE,
            leftCutoutPx = 40,
            rightCutoutPx = 0,
            statusBarHeightPx = 32,
        )

        assertEquals(LandscapeCutoutPaddingPx(left = 40, right = 0), result)
    }
}

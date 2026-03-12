package com.asuka.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
internal data class LandscapeCutoutPadding(
    val left: Dp = 0.dp,
    val right: Dp = 0.dp,
) {
    fun start(layoutDirection: LayoutDirection): Dp {
        return if (layoutDirection == LayoutDirection.Ltr) left else right
    }

    fun end(layoutDirection: LayoutDirection): Dp {
        return if (layoutDirection == LayoutDirection.Ltr) right else left
    }

    companion object {
        val None = LandscapeCutoutPadding()
    }
}

internal data class LandscapeCutoutPaddingPx(
    val left: Int = 0,
    val right: Int = 0,
)

internal fun resolveLandscapeCutoutPadding(
    orientation: Int,
    leftCutoutPx: Int,
    rightCutoutPx: Int,
    statusBarHeightPx: Int,
): LandscapeCutoutPaddingPx {
    if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
        return LandscapeCutoutPaddingPx()
    }
    val safeClearancePx = maxOf(statusBarHeightPx, leftCutoutPx, rightCutoutPx)
    return LandscapeCutoutPaddingPx(
        left = if (leftCutoutPx > 0) safeClearancePx else 0,
        right = if (rightCutoutPx > 0) safeClearancePx else 0,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberLandscapeCutoutPadding(): LandscapeCutoutPadding {
    val orientation = LocalConfiguration.current.orientation
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val displayCutoutInsets = WindowInsets.Companion.displayCutout
    val statusBarInsets = WindowInsets.Companion.statusBarsIgnoringVisibility
    val resolvedPadding = resolveLandscapeCutoutPadding(
        orientation = orientation,
        leftCutoutPx = displayCutoutInsets.getLeft(density, layoutDirection),
        rightCutoutPx = displayCutoutInsets.getRight(density, layoutDirection),
        statusBarHeightPx = statusBarInsets.getTop(density),
    )
    return with(density) {
        LandscapeCutoutPadding(
            left = resolvedPadding.left.toDp(),
            right = resolvedPadding.right.toDp(),
        )
    }
}

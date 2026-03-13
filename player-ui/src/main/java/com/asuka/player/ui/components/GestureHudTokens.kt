package com.asuka.player.ui.components

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.theme.PlayerUiTokens

internal val gestureHudHeight = 52.dp
private val portraitGestureHudGapFromPlaybackButton = 96.dp
private val landscapeGestureHudGapFromPlaybackButton = 22.dp
private const val gestureHudSurfaceAlpha = 0.55f

@Composable
internal fun rememberGestureHudVerticalOffset(): Dp {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val gap = if (isLandscape) {
        landscapeGestureHudGapFromPlaybackButton
    } else {
        portraitGestureHudGapFromPlaybackButton
    }
    return -((PlayerUiTokens.ButtonSize.playbackPrimary / 2) + gap + (gestureHudHeight / 2))
}

@Composable
internal fun gestureHudSurfaceColor(): Color =
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = gestureHudSurfaceAlpha)

@Composable
internal fun gestureHudContentColor(): Color =
    MaterialTheme.colorScheme.onPrimaryContainer

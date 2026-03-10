package com.asuka.player.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

internal const val ROUTE_HOME = "home"
internal const val ROUTE_ALL_VIDEOS = "all_videos"
internal const val ROUTE_RECENT = "recent"
internal const val ROUTE_SETTINGS = "settings"
internal const val ROUTE_SETTINGS_PLAYER = "settings/player"
internal const val ROUTE_SETTINGS_THEME = "settings/theme"
internal const val ROUTE_SETTINGS_MOTION = "settings/motion"
internal const val ARG_FOLDER_ID = "folderId"
internal const val ROUTE_FOLDER = "folder/{$ARG_FOLDER_ID}"

internal fun folderRoute(folderId: Long): String = "folder/$folderId"

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnterTransition(
    durationMs: Int,
): EnterTransition {
    val safeDuration = durationMs.coerceAtLeast(0)
    return fadeIn(animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing)) +
        slideInHorizontally(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            initialOffsetX = { (it * 0.08f).toInt() },
        ) +
        scaleIn(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            initialScale = 0.98f,
        )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExitTransition(
    durationMs: Int,
): ExitTransition {
    val safeDuration = durationMs.coerceAtLeast(0)
    return fadeOut(animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            targetOffsetX = { (-it * 0.06f).toInt() },
        ) +
        scaleOut(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            targetScale = 0.985f,
        )
}

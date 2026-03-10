package com.asuka.player.app

import android.content.ComponentName

data class PlaybackPlatformBindings(
    val playbackServiceComponent: ComponentName,
    val notificationSmallIconResId: Int,
)

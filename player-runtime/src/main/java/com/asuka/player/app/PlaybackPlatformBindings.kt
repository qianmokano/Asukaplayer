package com.asuka.player.runtime

import android.content.ComponentName

data class PlaybackPlatformBindings(
    val playbackServiceComponent: ComponentName,
    val notificationSmallIconResId: Int,
)

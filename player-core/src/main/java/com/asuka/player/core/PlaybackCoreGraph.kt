package com.asuka.player.core

import android.content.Context
import androidx.annotation.DrawableRes
import com.asuka.player.data.PlaybackStore
import com.asuka.player.data.QueueHistoryStore

interface PlaybackCoreGraph {
    val playbackStore: PlaybackStore
    val queueHistoryStore: QueueHistoryStore
    val playbackStateRepository: PlaybackStateRepository
    val playbackSessionPlanner: PlaybackSessionPlanner
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

fun Context.requirePlaybackCoreGraph(): PlaybackCoreGraph {
    return PlaybackCoreRegistry.require(applicationContext)
}

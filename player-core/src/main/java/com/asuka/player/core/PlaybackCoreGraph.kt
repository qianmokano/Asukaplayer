package com.asuka.player.core

import android.content.Context
import androidx.annotation.DrawableRes
import com.asuka.player.data.PlaybackStore

interface PlaybackCoreGraph {
    val playbackStore: PlaybackStore
    val queueHistoryStore: QueueHistoryStore
    val playbackStateRepository: PlaybackStateRepository
    val playbackSessionPlanner: PlaybackSessionPlanner
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

interface PlaybackCoreGraphOwner {
    val playbackCoreGraph: PlaybackCoreGraph
}

fun Context.requirePlaybackCoreGraph(): PlaybackCoreGraph {
    val app = applicationContext
    return (app as? PlaybackCoreGraphOwner)?.playbackCoreGraph
        ?: error(
            "Application must implement PlaybackCoreGraphOwner and expose a PlaybackCoreGraph.",
        )
}

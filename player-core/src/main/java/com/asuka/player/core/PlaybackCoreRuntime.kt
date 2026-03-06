package com.asuka.player.core

import androidx.annotation.DrawableRes
import com.asuka.player.data.PlaybackStore

interface PlaybackCoreDependencies {
    val playbackStore: PlaybackStore
    val queueHistoryStore: QueueHistoryStore
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

object PlaybackCoreRuntime {
    @Volatile
    private var dependencies: PlaybackCoreDependencies? = null

    fun install(dependencies: PlaybackCoreDependencies) {
        synchronized(this) {
            this.dependencies = dependencies
        }
    }

    val playbackStore: PlaybackStore
        get() = requireDependencies().playbackStore

    val queueHistoryStore: QueueHistoryStore
        get() = requireDependencies().queueHistoryStore

    val sessionActivityClass: Class<*>?
        get() = requireDependencies().sessionActivityClass

    val notificationSmallIconResId: Int
        get() = requireDependencies().notificationSmallIconResId

    private fun requireDependencies(): PlaybackCoreDependencies {
        return checkNotNull(dependencies) {
            "PlaybackCoreRuntime not installed. Call PlaybackCoreRuntime.install(...) in Application.onCreate()."
        }
    }
}


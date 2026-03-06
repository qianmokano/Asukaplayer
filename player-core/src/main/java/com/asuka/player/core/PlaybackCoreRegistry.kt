package com.asuka.player.core

import android.content.Context

object PlaybackCoreRegistry {
    private val lock = Any()
    private var provider: ((Context) -> PlaybackCoreGraph)? = null

    fun install(provider: (Context) -> PlaybackCoreGraph) {
        synchronized(lock) {
            this.provider = provider
        }
    }

    fun clear() {
        synchronized(lock) {
            provider = null
        }
    }

    fun require(context: Context): PlaybackCoreGraph {
        val currentProvider = synchronized(lock) { provider }
        return currentProvider?.invoke(context.applicationContext)
            ?: error("PlaybackCoreRegistry has not been installed by the application.")
    }
}

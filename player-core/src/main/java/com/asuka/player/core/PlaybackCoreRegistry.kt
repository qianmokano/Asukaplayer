package com.asuka.player.core

import android.content.Context

object PlaybackCoreRegistry {
    private val lock = Any()
    private var provider: ((Context) -> PlaybackCoreGraph)? = null

    fun install(provider: (Context) -> PlaybackCoreGraph) {
        synchronized(lock) {
            require(this.provider == null) {
                "PlaybackCoreRegistry.install() called more than once. " +
                    "Call clear() first if you need to re-install (e.g. in tests)."
            }
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

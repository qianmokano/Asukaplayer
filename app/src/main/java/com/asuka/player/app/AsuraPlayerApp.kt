package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackCoreRuntime

class AsuraPlayerApp : Application() {
    internal lateinit var graph: AsukaAppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AsukaAppGraph(this)
        PlaybackCoreRuntime.install(graph)
    }
}

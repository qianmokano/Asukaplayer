package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackCoreGraph
import com.asuka.player.core.PlaybackCoreGraphOwner

class AsuraPlayerApp : Application(), PlaybackCoreGraphOwner {
    internal lateinit var graph: AsukaAppGraph
        private set

    override val playbackCoreGraph: PlaybackCoreGraph
        get() = graph

    override fun onCreate() {
        super.onCreate()
        graph = AsukaAppGraph(this)
    }
}

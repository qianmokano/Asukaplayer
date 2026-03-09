package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackCoreGraph
import com.asuka.player.core.PlaybackCoreGraphProvider

class AsuraPlayerApp : Application(), PlaybackCoreGraphProvider {
    internal lateinit var graph: AsukaAppGraph
        private set

    override val playbackCoreGraph: PlaybackCoreGraph
        get() = graph

    /**
     * Override in tests to inject a fake/stub graph without subclassing the Application.
     * Must be set before [onCreate] is called (i.e. before Robolectric starts the app).
     */
    internal var graphFactory: (Application) -> AsukaAppGraph = ::AsukaAppGraph

    override fun onCreate() {
        super.onCreate()
        graph = graphFactory(this)
    }
}

package com.asuka.player.app

import android.app.Application
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.renderer.activity.PlaybackActivity
import com.asuka.player.runtime.AsukaAppGraph

internal class AppComposition(
    private val application: Application,
    private val graph: AsukaAppGraph,
) {
    val mainActivityDependencies: MainActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        AppMainActivityDependencies(
            application = application,
            graph = graph,
            playbackActivityClass = PlaybackActivity::class.java,
        )
    }
    val playbackActivityDependencies: PlaybackActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        AppPlaybackActivityDependencies(graph)
    }
    val playbackServiceDependencies: PlaybackServiceDependencies by lazy(LazyThreadSafetyMode.NONE) {
        AppPlaybackServiceDependencies(graph)
    }
}

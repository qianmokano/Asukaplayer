package com.asuka.player.app

import android.app.Application
import android.content.ComponentName
import androidx.annotation.DrawableRes
import com.asuka.player.core.PlaybackActivityDependencies
import com.asuka.player.core.PlaybackActivityDependenciesProvider
import com.asuka.player.core.PlaybackDeviceControllerFactory
import com.asuka.player.core.PlaybackRuntimeSettingsSource
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.PlaybackServiceDependencies
import com.asuka.player.core.PlaybackServiceDependenciesProvider
import com.asuka.player.core.PlaybackStateWriter
import com.asuka.player.core.PlaybackUiPersistence
import com.asuka.player.core.QueueHistoryWriter
import com.asuka.player.core.R as CoreR
import com.asuka.player.core.service.PlaybackService
import com.asuka.player.ui.activity.PlaybackActivity

class AsuraPlayerApp : Application(), AsukaAppGraphProvider, PlaybackActivityDependenciesProvider, PlaybackServiceDependenciesProvider {
    internal lateinit var graph: AsukaAppGraph
        private set
    private lateinit var activityDependencies: PlaybackActivityDependencies
    private lateinit var serviceDependencies: PlaybackServiceDependencies

    override val appGraph: AsukaAppGraph
        get() = graph

    override val playbackActivityDependencies: PlaybackActivityDependencies
        get() = activityDependencies

    override val playbackServiceDependencies: PlaybackServiceDependencies
        get() = serviceDependencies

    /**
     * Override in tests to inject a fake/stub graph without subclassing the Application.
     * Must be set before [onCreate] is called (i.e. before Robolectric starts the app).
     */
    internal var graphFactory: (Application) -> AsukaAppGraph = ::AsukaAppGraph

    override fun onCreate() {
        super.onCreate()
        graph = graphFactory(this)
        activityDependencies = AppPlaybackActivityDependencies(
            graph = graph,
            playbackServiceComponent = ComponentName(this, PlaybackService::class.java),
        )
        serviceDependencies = AppPlaybackServiceDependencies(
            graph = graph,
            sessionActivityClass = PlaybackActivity::class.java,
            notificationSmallIconResId = CoreR.drawable.ic_stat_playback,
        )
    }
}

private class AppPlaybackActivityDependencies(
    private val graph: AsukaAppGraph,
    override val playbackServiceComponent: ComponentName,
) : PlaybackActivityDependencies {
    override val playbackSessionPlanner: PlaybackSessionPlanner
        get() = graph.playbackSessionPlanner
    override val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
        get() = graph.playbackRuntimeSettingsSource
    override val playbackUiPersistence: PlaybackUiPersistence
        get() = graph.playbackUiPersistence
    override val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
        get() = graph.playbackDeviceControllerFactory
}

private class AppPlaybackServiceDependencies(
    private val graph: AsukaAppGraph,
    override val sessionActivityClass: Class<*>?,
    @get:DrawableRes override val notificationSmallIconResId: Int,
) : PlaybackServiceDependencies {
    override fun createPlaybackStateWriter(): PlaybackStateWriter {
        return graph.createPlaybackStateWriter()
    }

    override fun createQueueHistoryWriter(): QueueHistoryWriter {
        return graph.createQueueHistoryWriter()
    }
}

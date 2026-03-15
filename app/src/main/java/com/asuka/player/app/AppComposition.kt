package com.asuka.player.app

import android.app.Application
import android.content.Context
import androidx.annotation.DrawableRes
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
        object : PlaybackActivityDependencies {
            override val playbackSessionPlanner get() = graph.playback.playbackSessionPlanner
            override val playbackRuntimeSettingsSource get() = graph.settings.playbackRuntimeSettingsSource
            override val playbackUiPersistence get() = graph.playback.playbackUiPersistence
            override val playbackPreviewFrameProvider get() = graph.playback.playbackPreviewFrameProvider
            override val playbackDeviceControllerFactory get() = graph.playback.playbackDeviceControllerFactory
            override val persistenceDegraded get() = graph.playback.persistenceDegraded
            override fun createPlaybackControllerConnector(context: Context) =
                graph.playback.playbackControllerConnectorFactory.create(
                    context = context,
                    playbackServiceComponent = graph.playback.playbackPlatformBindings.playbackServiceComponent,
                )
        }
    }
    val playbackServiceDependencies: PlaybackServiceDependencies by lazy(LazyThreadSafetyMode.NONE) {
        object : PlaybackServiceDependencies {
            override val playbackStore get() = graph.playback.playbackStore
            override val queueHistoryStore get() = graph.playback.queueHistoryStore
            override val sessionActivityClass: Class<*> = PlaybackActivity::class.java
            @get:DrawableRes
            override val notificationSmallIconResId get() = graph.playback.playbackPlatformBindings.notificationSmallIconResId
        }
    }
}

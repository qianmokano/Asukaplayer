package com.asuka.player.app

import androidx.annotation.DrawableRes
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.runtime.AsukaAppGraph

internal class AppPlaybackActivityDependencies(
    private val graph: AsukaAppGraph,
) : PlaybackActivityDependencies {
    override val playbackSessionPlanner: PlaybackSessionPlanner
        get() = graph.playbackSessionPlanner
    override val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
        get() = graph.playbackRuntimeSettingsSource
    override val playbackUiPersistence: PlaybackUiPersistence
        get() = graph.playbackUiPersistence
    override val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
        get() = graph.playbackDeviceControllerFactory

    override fun createPlaybackControllerConnector(context: android.content.Context) =
        graph.playbackControllerConnectorFactory.create(
            context = context,
            playbackServiceComponent = graph.playbackPlatformBindings.playbackServiceComponent,
        )
}

internal class AppPlaybackServiceDependencies(
    private val graph: AsukaAppGraph,
    override val sessionActivityClass: Class<*>?,
    @get:DrawableRes override val notificationSmallIconResId: Int,
) : PlaybackServiceDependencies {
    override val playbackStore
        get() = graph.playbackStore

    override val queueHistoryStore
        get() = graph.queueHistoryStore
}

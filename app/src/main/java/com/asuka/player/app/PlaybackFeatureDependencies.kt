package com.asuka.player.app

import androidx.annotation.DrawableRes
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.runtime.AsukaAppGraph
import kotlinx.coroutines.flow.StateFlow

internal class AppPlaybackActivityDependencies(
    private val graph: AsukaAppGraph,
) : PlaybackActivityDependencies {
    override val playbackSessionPlanner: PlaybackSessionPlanner
        get() = graph.playback.playbackSessionPlanner
    override val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
        get() = graph.settings.playbackRuntimeSettingsSource
    override val playbackUiPersistence: PlaybackUiPersistence
        get() = graph.playback.playbackUiPersistence
    override val playbackPreviewFrameProvider: PlaybackPreviewFrameProvider
        get() = graph.playback.playbackPreviewFrameProvider
    override val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
        get() = graph.playback.playbackDeviceControllerFactory
    override val persistenceDegraded: StateFlow<Boolean>
        get() = graph.playback.persistenceDegraded

    override fun createPlaybackControllerConnector(context: android.content.Context) =
        graph.playback.playbackControllerConnectorFactory.create(
            context = context,
            playbackServiceComponent = graph.playback.playbackPlatformBindings.playbackServiceComponent,
        )
}

internal class AppPlaybackServiceDependencies(
    private val graph: AsukaAppGraph,
) : PlaybackServiceDependencies {
    override val sessionActivityClass: Class<*>?
        get() = com.asuka.player.renderer.activity.PlaybackActivity::class.java

    @get:DrawableRes
    override val notificationSmallIconResId: Int
        get() = graph.playback.playbackPlatformBindings.notificationSmallIconResId

    override val playbackStore
        get() = graph.playback.playbackStore

    override val queueHistoryStore
        get() = graph.playback.queueHistoryStore
}

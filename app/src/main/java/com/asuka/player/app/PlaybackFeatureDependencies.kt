package com.asuka.player.app

import androidx.annotation.DrawableRes
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.platform.PlaybackServiceDependencies

internal class AppPlaybackActivityDependencies(
    private val bindings: PlaybackActivityEntryBindings,
) : PlaybackActivityDependencies {
    override val playbackSessionPlanner: PlaybackSessionPlanner
        get() = bindings.playbackSessionPlanner()
    override val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
        get() = bindings.playbackRuntimeSettingsSource()
    override val playbackUiPersistence: PlaybackUiPersistence
        get() = bindings.playbackUiPersistence()
    override val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
        get() = bindings.playbackDeviceControllerFactory()

    override fun createPlaybackControllerConnector(context: android.content.Context) =
        bindings.createPlaybackControllerConnector(context)
}

internal class AppPlaybackServiceDependencies(
    private val bindings: PlaybackServiceEntryBindings,
) : PlaybackServiceDependencies {
    override val sessionActivityClass: Class<*>?
        get() = bindings.sessionActivityClass

    @get:DrawableRes
    override val notificationSmallIconResId: Int
        get() = bindings.notificationSmallIconResId

    override val playbackStore
        get() = bindings.playbackStore()

    override val queueHistoryStore
        get() = bindings.queueHistoryStore()
}

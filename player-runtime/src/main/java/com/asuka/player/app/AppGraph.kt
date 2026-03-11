package com.asuka.player.runtime

import android.app.Application
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryRepository
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AsukaAppGraph(
    application: Application,
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings: SettingsRuntimeFeature by lazy(LazyThreadSafetyMode.NONE) {
        SettingsRuntimeInstaller.install(
            application = application,
            scope = appScope,
        )
    }
    val playback: PlaybackRuntimeFeature by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackRuntimeInstaller.install(
            application = application,
            playbackBehaviorRepository = settings.playbackBehaviorRepository,
            scope = appScope,
        )
    }

    val uiSettingsRepository: UiSettingsRepository
        get() = settings.uiSettingsRepository
    val playerSettingsRepository: PlayerSettingsRepository
        get() = settings.playerSettingsRepository
    val playbackBehaviorRepository: PlaybackBehaviorRepository
        get() = settings.playbackBehaviorRepository
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
        get() = settings.playbackRuntimeSettingsSource

    val playbackStore: PlaybackStore
        get() = playback.playbackStore
    val queueHistoryStore: QueueHistoryStore
        get() = playback.queueHistoryStore
    val queueHistoryRepository: QueueHistoryRepository
        get() = playback.queueHistoryRepository
    val playbackStateRepository: PlaybackStateRepository
        get() = playback.playbackStateRepository
    val playbackSessionPlanner: PlaybackSessionPlanner
        get() = playback.playbackSessionPlanner
    val playbackUiPersistence: PlaybackUiPersistence
        get() = playback.playbackUiPersistence
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
        get() = playback.playbackDeviceControllerFactory
    val playbackControllerConnectorFactory: PlaybackControllerConnectorFactory
        get() = playback.playbackControllerConnectorFactory
    val playbackLaunchCoordinator: PlaybackLaunchCoordinator
        get() = playback.playbackLaunchCoordinator
    val playbackPlatformBindings: PlaybackPlatformBindings
        get() = playback.playbackPlatformBindings
}

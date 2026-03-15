package com.asuka.player.runtime

import android.app.Application
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AsukaAppGraph(
    application: Application,
    controllerConnectorFactory: PlaybackControllerConnectorFactory,
    platformBindings: PlaybackPlatformBindings,
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings: SettingsRuntimeFeature by lazy(LazyThreadSafetyMode.NONE) {
        SettingsRuntimeFeature.create(
            application = application,
            scope = appScope,
        )
    }
    val playback: PlaybackRuntimeFeature by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackRuntimeFeature(
            application = application,
            playbackBehaviorRepository = settings.playbackBehaviorRepository,
            scope = appScope,
            controllerConnectorFactory = controllerConnectorFactory,
            playbackPlatformBindings = platformBindings,
        )
    }
}

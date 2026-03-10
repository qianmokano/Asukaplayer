package com.asuka.player.core

import android.content.ComponentName
import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.StateFlow

interface PlaybackRuntimeSettingsSource {
    val settings: StateFlow<PlaybackRuntimeSettings>

    fun current(): PlaybackRuntimeSettings = settings.value
}

interface PlaybackActivityDependencies {
    val playbackSessionPlanner: PlaybackSessionPlanner
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
    val playbackUiPersistence: PlaybackUiPersistence
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
    val playbackServiceComponent: ComponentName
}

interface PlaybackServiceDependencies {
    fun createPlaybackStateWriter(): PlaybackStateWriter
    fun createQueueHistoryWriter(): QueueHistoryWriter
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

object PlaybackDependencyRegistry {
    @Volatile
    private var activityDependencies: PlaybackActivityDependencies? = null

    @Volatile
    private var serviceDependencies: PlaybackServiceDependencies? = null

    fun register(
        activityDependencies: PlaybackActivityDependencies,
        serviceDependencies: PlaybackServiceDependencies,
    ) {
        this.activityDependencies = activityDependencies
        this.serviceDependencies = serviceDependencies
    }

    fun requireActivityDependencies(): PlaybackActivityDependencies {
        return activityDependencies
            ?: error("PlaybackActivityDependencies have not been registered.")
    }

    fun requireServiceDependencies(): PlaybackServiceDependencies {
        return serviceDependencies
            ?: error("PlaybackServiceDependencies have not been registered.")
    }
}

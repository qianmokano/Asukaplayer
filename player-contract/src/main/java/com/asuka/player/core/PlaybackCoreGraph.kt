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

interface PlaybackDependenciesProvider {
    val playbackActivityDependencies: PlaybackActivityDependencies
    val playbackServiceDependencies: PlaybackServiceDependencies
}

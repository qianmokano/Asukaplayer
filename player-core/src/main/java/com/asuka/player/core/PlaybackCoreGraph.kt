package com.asuka.player.core

import android.content.ComponentName
import android.content.Context
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

interface PlaybackActivityDependenciesProvider {
    val playbackActivityDependencies: PlaybackActivityDependencies
}

interface PlaybackServiceDependencies {
    fun createPlaybackStateWriter(): PlaybackStateWriter
    fun createQueueHistoryWriter(): QueueHistoryWriter
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

interface PlaybackServiceDependenciesProvider {
    val playbackServiceDependencies: PlaybackServiceDependencies
}

fun Context.requirePlaybackActivityDependencies(): PlaybackActivityDependencies {
    val appContext = applicationContext
    return (appContext as? PlaybackActivityDependenciesProvider)?.playbackActivityDependencies
        ?: error(
            "Application must implement PlaybackActivityDependenciesProvider to expose playback activity dependencies.",
        )
}

fun Context.requirePlaybackServiceDependencies(): PlaybackServiceDependencies {
    val appContext = applicationContext
    return (appContext as? PlaybackServiceDependenciesProvider)?.playbackServiceDependencies
        ?: error(
            "Application must implement PlaybackServiceDependenciesProvider to expose playback service dependencies.",
        )
}

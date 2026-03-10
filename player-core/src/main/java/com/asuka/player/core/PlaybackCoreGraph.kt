package com.asuka.player.core

import android.content.ComponentName
import android.content.Context
import androidx.annotation.DrawableRes
import com.asuka.player.data.PlaybackStore
import com.asuka.player.data.QueueHistoryStore
import kotlinx.coroutines.flow.StateFlow

interface PlaybackRuntimeSettingsSource {
    val settings: StateFlow<PlaybackRuntimeSettings>

    fun current(): PlaybackRuntimeSettings = settings.value
}

interface PlaybackCoreGraphProvider {
    val playbackCoreGraph: PlaybackCoreGraph
}

interface PlaybackCoreGraph {
    val playbackStore: PlaybackStore
    val queueHistoryStore: QueueHistoryStore
    val playbackStateRepository: PlaybackStateRepository
    val playbackSessionPlanner: PlaybackSessionPlanner
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
    val playbackUiPersistence: PlaybackUiPersistence
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
    val playbackServiceComponent: ComponentName
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

fun Context.requirePlaybackCoreGraph(): PlaybackCoreGraph {
    val appContext = applicationContext
    return (appContext as? PlaybackCoreGraphProvider)?.playbackCoreGraph
        ?: error(
            "Application must implement PlaybackCoreGraphProvider to expose the playback graph.",
        )
}

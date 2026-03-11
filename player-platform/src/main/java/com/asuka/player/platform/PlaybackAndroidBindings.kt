package com.asuka.player.platform

import android.content.Context
import androidx.annotation.DrawableRes
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryStore

interface PlaybackActivityDependencies {
    val playbackSessionPlanner: PlaybackSessionPlanner
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
    val playbackUiPersistence: PlaybackUiPersistence
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory

    fun createPlaybackControllerConnector(context: Context): PlaybackControllerConnector
}

interface PlaybackServiceDependencies {
    val playbackStore: PlaybackStore
    val queueHistoryStore: QueueHistoryStore
    val sessionActivityClass: Class<*>?

    @get:DrawableRes
    val notificationSmallIconResId: Int
}

interface PlaybackDependenciesProvider {
    val playbackActivityDependencies: PlaybackActivityDependencies
    val playbackServiceDependencies: PlaybackServiceDependencies
}

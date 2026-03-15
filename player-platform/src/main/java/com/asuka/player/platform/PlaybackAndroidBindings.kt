package com.asuka.player.platform

import android.content.Context
import androidx.annotation.DrawableRes
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PlaybackActivityDependencies {
    val playbackSessionPlanner: PlaybackSessionPlanner
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
    val playbackUiPersistence: PlaybackUiPersistence
    val playbackPreviewFrameProvider: PlaybackPreviewFrameProvider
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
    val persistenceDegraded: StateFlow<Boolean>
        get() = MutableStateFlow(false)

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

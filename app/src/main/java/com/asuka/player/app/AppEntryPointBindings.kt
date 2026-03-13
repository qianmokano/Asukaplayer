package com.asuka.player.app

import android.content.Context
import androidx.annotation.DrawableRes
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryRepository
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.platform.PlaybackControllerConnector
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.runtime.PlaybackLaunchCoordinator
import com.asuka.player.runtime.PlayerSettingsRepository
import com.asuka.player.runtime.UiSettingsRepository

internal data class MainLibraryFeatureBindings(
    val uiSettingsRepository: () -> UiSettingsRepository,
    val playerSettingsRepository: () -> PlayerSettingsRepository,
    val playbackStateRepository: () -> PlaybackStateRepository,
    val queueHistoryRepository: () -> QueueHistoryRepository,
    val playbackLaunchCoordinator: () -> PlaybackLaunchCoordinator,
)

internal data class PlaybackActivityEntryBindings(
    val playbackSessionPlanner: () -> PlaybackSessionPlanner,
    val playbackRuntimeSettingsSource: () -> PlaybackRuntimeSettingsSource,
    val playbackUiPersistence: () -> PlaybackUiPersistence,
    val playbackPreviewFrameProvider: () -> PlaybackPreviewFrameProvider,
    val playbackDeviceControllerFactory: () -> PlaybackDeviceControllerFactory,
    val createPlaybackControllerConnector: (Context) -> PlaybackControllerConnector,
)

internal data class PlaybackServiceEntryBindings(
    val playbackStore: () -> PlaybackStore,
    val queueHistoryStore: () -> QueueHistoryStore,
    val sessionActivityClass: Class<*>?,
    @get:DrawableRes val notificationSmallIconResId: Int,
)

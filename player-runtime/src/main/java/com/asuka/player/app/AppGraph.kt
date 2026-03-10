package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackDeviceControllerFactory
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.PlaybackStateWriter
import com.asuka.player.core.PlaybackRuntimeSettingsSource
import com.asuka.player.core.PlaybackUiPersistence
import com.asuka.player.core.QueueHistoryRepository
import com.asuka.player.core.QueueHistoryWriter
import com.asuka.player.data.SharedPreferencesAppSettingsStore
import com.asuka.player.data.SharedPreferencesPlaybackStore
import com.asuka.player.data.SharedPreferencesQueueHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AsukaAppGraph(
    application: Application,
) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settingsStore = SharedPreferencesAppSettingsStore(application)
    private val playbackStore = SharedPreferencesPlaybackStore(application)
    private val queueHistoryStore = SharedPreferencesQueueHistoryStore(application)
    private val uriResolver = SeekAwarePlaybackUriResolver(
        contentResolver = application.contentResolver,
        cacheDir = application.cacheDir,
    )

    val uiSettingsRepository = UiSettingsRepository(settingsStore)
    val playerSettingsRepository = PlayerSettingsRepository(settingsStore)
    val playbackBehaviorRepository = PlaybackBehaviorRepository(settingsStore)
    val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource = AppPlaybackRuntimeSettingsSource(
        playerSettingsRepository = playerSettingsRepository,
        playbackBehaviorRepository = playbackBehaviorRepository,
        scope = appScope,
    )

    val queueHistoryRepository = QueueHistoryRepository(queueHistoryStore)

    val playbackStateRepository = PlaybackStateRepository(playbackStore)
    val playbackSessionPlanner = PlaybackSessionPlanner(
        playbackStateRepository = playbackStateRepository,
    )
    val playbackUiPersistence: PlaybackUiPersistence = PlaybackStateUiPersistence(
        playbackStateRepository = playbackStateRepository,
        playbackBehaviorRepository = playbackBehaviorRepository,
    )
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory =
        DefaultPlaybackDeviceControllerFactory

    val playbackLaunchCoordinator = PlaybackLaunchCoordinator(uriResolver)

    fun createPlaybackStateWriter(): PlaybackStateWriter {
        return PlaybackStateWriter(playbackStore)
    }

    fun createQueueHistoryWriter(): QueueHistoryWriter {
        return QueueHistoryWriter(queueHistoryStore)
    }
}

interface AsukaAppGraphProvider {
    val appGraph: AsukaAppGraph
}

val Application.appGraph: AsukaAppGraph
    get() = (this as? AsukaAppGraphProvider)?.appGraph
        ?: error("Application must expose AsukaAppGraph via AsukaAppGraphProvider.")

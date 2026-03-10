package com.asuka.player.app

import android.app.Application
import android.content.ComponentName
import com.asuka.player.core.PlaybackCoreGraph
import com.asuka.player.core.PlaybackCoreGraphProvider
import com.asuka.player.core.PlaybackDeviceControllerFactory
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.PlaybackUiPersistence
import com.asuka.player.core.QueueHistoryRepository
import com.asuka.player.core.R as CoreR
import com.asuka.player.core.service.PlaybackService
import com.asuka.player.data.SharedPreferencesAppSettingsStore
import com.asuka.player.data.SharedPreferencesPlaybackStore
import com.asuka.player.data.SharedPreferencesQueueHistoryStore
import com.asuka.player.ui.activity.PlaybackActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AsukaAppGraph(
    application: Application,
) : PlaybackCoreGraph {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settingsStore = SharedPreferencesAppSettingsStore(application)
    private val uriResolver = SeekAwarePlaybackUriResolver(
        contentResolver = application.contentResolver,
        cacheDir = application.cacheDir,
    )

    val uiSettingsRepository = UiSettingsRepository(settingsStore)
    val playerSettingsRepository = PlayerSettingsRepository(settingsStore)
    val playbackBehaviorRepository = PlaybackBehaviorRepository(settingsStore)
    override val playbackRuntimeSettingsSource = AppPlaybackRuntimeSettingsSource(
        playerSettingsRepository = playerSettingsRepository,
        playbackBehaviorRepository = playbackBehaviorRepository,
        scope = appScope,
    )

    override val playbackStore = SharedPreferencesPlaybackStore(application)
    override val queueHistoryStore = SharedPreferencesQueueHistoryStore(application)
    val queueHistoryRepository = QueueHistoryRepository(queueHistoryStore)
    override val playbackServiceComponent = ComponentName(application, PlaybackService::class.java)
    override val sessionActivityClass: Class<*> = PlaybackActivity::class.java
    override val notificationSmallIconResId: Int = CoreR.drawable.ic_stat_playback

    override val playbackStateRepository = PlaybackStateRepository(playbackStore)
    override val playbackSessionPlanner = PlaybackSessionPlanner(
        playbackStateRepository = playbackStateRepository,
    )
    override val playbackUiPersistence: PlaybackUiPersistence = PlaybackStateUiPersistence(
        playbackStateRepository = playbackStateRepository,
        playbackBehaviorRepository = playbackBehaviorRepository,
    )
    override val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory =
        DefaultPlaybackDeviceControllerFactory

    val playbackLaunchCoordinator = PlaybackLaunchCoordinator(uriResolver)
}

val Application.appGraph: AsukaAppGraph
    get() = ((this as? PlaybackCoreGraphProvider)?.playbackCoreGraph as? AsukaAppGraph)
        ?: error("Application must expose AsukaAppGraph via PlaybackCoreGraphProvider.")

package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackCoreGraph
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.QueueHistoryRepository
import com.asuka.player.core.R as CoreR
import com.asuka.player.core.SharedPreferencesPlaybackStore
import com.asuka.player.core.SharedPreferencesQueueHistoryStore
import com.asuka.player.ui.activity.PlaybackActivity

internal class AsukaAppGraph(
    application: Application,
) : PlaybackCoreGraph {
    private val settingsStore = AppSettingsStore(application)
    private val uriResolver = SeekAwarePlaybackUriResolver(
        contentResolver = application.contentResolver,
        cacheDir = application.cacheDir,
    )

    val uiSettingsRepository = UiSettingsRepository(settingsStore)
    val playerSettingsRepository = PlayerSettingsRepository(settingsStore)
    val playbackBehaviorRepository = PlaybackBehaviorRepository(settingsStore)

    override val playbackStore = SharedPreferencesPlaybackStore(application)
    override val queueHistoryStore = SharedPreferencesQueueHistoryStore(application)
    override val sessionActivityClass: Class<*> = PlaybackActivity::class.java
    override val notificationSmallIconResId: Int = CoreR.drawable.ic_stat_playback

    override val playbackStateRepository = PlaybackStateRepository(playbackStore)
    private val queueHistoryRepository = QueueHistoryRepository(queueHistoryStore)
    override val playbackSessionPlanner = PlaybackSessionPlanner(
        playbackStateRepository = playbackStateRepository,
        queueHistoryRepository = queueHistoryRepository,
    )
    val playbackLaunchCoordinator = PlaybackLaunchCoordinator(uriResolver)
}

internal val Application.appGraph: AsukaAppGraph
    get() = (this as AsuraPlayerApp).graph

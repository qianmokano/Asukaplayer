package com.asuka.player.app

import android.app.Application
import com.asuka.player.core.PlaybackCoreDependencies
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.QueueHistoryRepository
import com.asuka.player.core.QueueHistoryStore
import com.asuka.player.core.R as CoreR
import com.asuka.player.core.SharedPreferencesPlaybackStore
import com.asuka.player.ui.activity.PlaybackActivity

internal class AsukaAppGraph(
    application: Application,
) : PlaybackCoreDependencies {
    private val settingsStore = AppSettingsStore(application)
    private val uriResolver = SeekAwarePlaybackUriResolver(
        contentResolver = application.contentResolver,
        cacheDir = application.cacheDir,
    )

    val uiSettingsRepository = UiSettingsRepository(settingsStore)
    val playerSettingsRepository = PlayerSettingsRepository(settingsStore)
    val playbackBehaviorRepository = PlaybackBehaviorRepository(settingsStore)

    override val playbackStore = SharedPreferencesPlaybackStore(application)
    override val queueHistoryStore = QueueHistoryStore()
    override val sessionActivityClass: Class<*> = PlaybackActivity::class.java
    override val notificationSmallIconResId: Int = CoreR.drawable.ic_stat_playback

    val playbackStateRepository = PlaybackStateRepository(playbackStore)
    val queueHistoryRepository = QueueHistoryRepository(queueHistoryStore)
    val playbackSessionPlanner = PlaybackSessionPlanner(
        playbackStateRepository = playbackStateRepository,
        queueHistoryRepository = queueHistoryRepository,
    )
    val playbackLaunchCoordinator = PlaybackLaunchCoordinator(uriResolver)
}

internal val Application.appGraph: AsukaAppGraph
    get() = (this as AsuraPlayerApp).graph


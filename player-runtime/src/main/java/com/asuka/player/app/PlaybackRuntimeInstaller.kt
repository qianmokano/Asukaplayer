package com.asuka.player.runtime

import android.app.Application
import android.content.ComponentName
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryRepository
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.data.PlaybackPersistenceStoresFactory
import com.asuka.player.engine.Media3PlaybackControllerConnectorFactory
import com.asuka.player.engine.service.PlaybackService
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.core.R as EngineR

data class PlaybackRuntimeFeature(
    val playbackStore: PlaybackStore,
    val queueHistoryStore: QueueHistoryStore,
    val queueHistoryRepository: QueueHistoryRepository,
    val playbackStateRepository: PlaybackStateRepository,
    val playbackSessionPlanner: PlaybackSessionPlanner,
    val playbackUiPersistence: PlaybackUiPersistence,
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory,
    val playbackControllerConnectorFactory: PlaybackControllerConnectorFactory,
    val playbackLaunchCoordinator: PlaybackLaunchCoordinator,
    val playbackPlatformBindings: PlaybackPlatformBindings,
)

object PlaybackRuntimeInstaller {
    fun install(
        application: Application,
        playbackBehaviorRepository: PlaybackBehaviorRepository,
    ): PlaybackRuntimeFeature {
        val persistenceStores = PlaybackPersistenceStoresFactory.create(
            context = application,
        )
        val playbackStore: PlaybackStore = persistenceStores.playbackStore
        val queueHistoryStore: QueueHistoryStore = persistenceStores.queueHistoryStore
        val playbackStateRepository = PlaybackStateRepository(playbackStore)
        val queueHistoryRepository = QueueHistoryRepository(queueHistoryStore)
        val playbackSessionPlanner = PlaybackSessionPlanner(
            playbackStateRepository = playbackStateRepository,
        )
        val playbackUiPersistence: PlaybackUiPersistence = PlaybackStateUiPersistence(
            playbackStateRepository = playbackStateRepository,
            playbackBehaviorRepository = playbackBehaviorRepository,
        )
        val uriResolver = SeekAwarePlaybackUriResolver(
            contentResolver = application.contentResolver,
            cacheDir = application.cacheDir,
        )
        return PlaybackRuntimeFeature(
            playbackStore = playbackStore,
            queueHistoryStore = queueHistoryStore,
            queueHistoryRepository = queueHistoryRepository,
            playbackStateRepository = playbackStateRepository,
            playbackSessionPlanner = playbackSessionPlanner,
            playbackUiPersistence = playbackUiPersistence,
            playbackDeviceControllerFactory = DefaultPlaybackDeviceControllerFactory,
            playbackControllerConnectorFactory = Media3PlaybackControllerConnectorFactory,
            playbackLaunchCoordinator = PlaybackLaunchCoordinator(uriResolver),
            playbackPlatformBindings = PlaybackPlatformBindings(
                playbackServiceComponent = ComponentName(application, PlaybackService::class.java),
                notificationSmallIconResId = EngineR.drawable.ic_stat_playback,
            ),
        )
    }
}

package com.asuka.player.runtime

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.contract.QueueHistoryRepository
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.data.PlaybackPersistenceStores
import com.asuka.player.data.PlaybackPersistenceStoresFactory
import com.asuka.player.engine.Media3PlaybackControllerConnectorFactory
import com.asuka.player.engine.service.PlaybackService
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.core.R as EngineR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlaybackRuntimeFeature(
    application: Application,
    playbackBehaviorRepository: PlaybackBehaviorRepository,
    scope: CoroutineScope,
) {
    private val appContext = application.applicationContext
    private val persistenceResolver = PlaybackPersistenceResolver(appContext)

    val playbackStore: PlaybackStore = DeferredPlaybackStore {
        persistenceResolver.resolve().playbackStore
    }
    val queueHistoryStore: QueueHistoryStore = DeferredQueueHistoryStore {
        persistenceResolver.resolve().queueHistoryStore
    }
    val queueHistoryRepository: QueueHistoryRepository = QueueHistoryRepository(queueHistoryStore)
    val playbackStateRepository: PlaybackStateRepository = PlaybackStateRepository(playbackStore)
    val playbackSessionPlanner: PlaybackSessionPlanner = PlaybackSessionPlanner(playbackStateRepository)
    val playbackUiPersistence: PlaybackUiPersistence = PlaybackStateUiPersistence(
        playbackStateRepository = playbackStateRepository,
        playbackBehaviorRepository = playbackBehaviorRepository,
    )
    val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory = DefaultPlaybackDeviceControllerFactory
    val playbackControllerConnectorFactory: PlaybackControllerConnectorFactory = Media3PlaybackControllerConnectorFactory
    val playbackLaunchCoordinator: PlaybackLaunchCoordinator by lazy(LazyThreadSafetyMode.NONE) {
        val uriResolver = SeekAwarePlaybackUriResolver(
            contentResolver = appContext.contentResolver,
            cacheDir = appContext.cacheDir,
        )
        PlaybackLaunchCoordinator(uriResolver)
    }
    val playbackPlatformBindings: PlaybackPlatformBindings = PlaybackPlatformBindings(
        playbackServiceComponent = ComponentName(appContext, PlaybackService::class.java),
        notificationSmallIconResId = EngineR.drawable.ic_stat_playback,
    )
}

object PlaybackRuntimeInstaller {
    fun install(
        application: Application,
        playbackBehaviorRepository: PlaybackBehaviorRepository,
        scope: CoroutineScope,
    ): PlaybackRuntimeFeature {
        return PlaybackRuntimeFeature(
            application = application,
            playbackBehaviorRepository = playbackBehaviorRepository,
            scope = scope,
        )
    }
}

private class PlaybackPersistenceResolver(
    private val context: Context,
) {
    private val lock = Mutex()
    @Volatile
    private var stores: PlaybackPersistenceStores? = null

    suspend fun resolve(): PlaybackPersistenceStores {
        stores?.let { return it }
        return lock.withLock {
            stores ?: PlaybackPersistenceStoresFactory.create(context).also { created ->
                stores = created
            }
        }
    }
}

private class DeferredPlaybackStore(
    private val resolveStore: suspend () -> PlaybackStore,
) : PlaybackStore {
    override suspend fun recentMediaIds(limit: Int): List<String> = resolveStore().recentMediaIds(limit)

    override suspend fun loadPosition(mediaId: String): Long? = resolveStore().loadPosition(mediaId)

    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        resolveStore().savePosition(mediaId, positionMs)
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = resolveStore().loadPlaybackSpeed(mediaId)

    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        resolveStore().savePlaybackSpeed(mediaId, speed)
    }

    override suspend fun loadAudioTrackId(mediaId: String): String? = resolveStore().loadAudioTrackId(mediaId)

    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        resolveStore().saveAudioTrackId(mediaId, trackId)
    }

    override suspend fun loadSubtitleTrackId(mediaId: String): String? = resolveStore().loadSubtitleTrackId(mediaId)

    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        resolveStore().saveSubtitleTrackId(mediaId, trackId)
    }

    override suspend fun loadZoom(mediaId: String): Float? = resolveStore().loadZoom(mediaId)

    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        resolveStore().saveZoom(mediaId, zoom)
    }
}

private class DeferredQueueHistoryStore(
    private val resolveStore: suspend () -> QueueHistoryStore,
) : QueueHistoryStore {
    override suspend fun push(mediaId: String) {
        resolveStore().push(mediaId)
    }

    override suspend fun items(): List<String> = resolveStore().items()
}

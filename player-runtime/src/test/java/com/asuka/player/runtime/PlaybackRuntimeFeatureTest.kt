package com.asuka.player.runtime

import android.app.Application
import android.content.ComponentName
import android.content.Context
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.asuka.player.data.AppSettingsSnapshot
import com.asuka.player.data.AppSettingsStore
import com.asuka.player.data.PlaybackPersistenceStores
import com.asuka.player.platform.PlaybackControllerConnector
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackRuntimeFeatureTest {

    @Test
    fun failingPersistenceFactory_degradesToInMemoryStores_withoutBlockingPlaybackPlanning() = runBlocking {
        val application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val feature = createFeature(
            application = application,
            scope = scope,
            persistenceStoresFactory = { _ ->
                error("boom")
            },
        )

        try {
            val plan = feature.playbackSessionPlanner.plan(
                targetUri = "content://videos/current.mp4",
                launchNeighbors = emptyList(),
                policy = PlaybackStartupPolicy(
                    resumePlayback = true,
                    defaultPlaybackSpeed = 1.25f,
                    rememberTrackSelections = true,
                ),
            )

            assertEquals(0L, plan.resumePositionMs)
            assertEquals(1.25f, plan.playbackSpeed)
            assertNull(plan.trackSelectionRestoreRequest)
            assertTrue(feature.persistenceDegraded.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun failingPersistenceFactory_keepsDeferredStoresReadableAndWritable() = runBlocking {
        val application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val feature = createFeature(
            application = application,
            scope = scope,
            persistenceStoresFactory = { _ ->
                error("boom")
            },
        )

        try {
            feature.playbackStore.savePosition("media://one", 123L)
            feature.queueHistoryStore.push("media://one")

            assertEquals(123L, feature.playbackStore.loadPosition("media://one"))
            assertEquals(listOf("media://one"), feature.queueHistoryStore.items())
            assertTrue(feature.persistenceDegraded.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun fallbackStores_recoverAndMigrateStateAfterFactoryStartsWorking() = runBlocking {
        val application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var attempts = 0
        var nowMs = 0L
        val resolvedPlaybackStore = FakePlaybackStore()
        val resolvedQueueHistoryStore = FakeQueueHistoryStore()
        val resolvedStores = PlaybackPersistenceStores(
            playbackStore = resolvedPlaybackStore,
            queueHistoryStore = resolvedQueueHistoryStore,
        )
        val feature = createFeature(
            application = application,
            scope = scope,
            persistenceStoresFactory = { _ ->
                attempts += 1
                if (attempts < 2) {
                    error("boom")
                }
                resolvedStores
            },
            nowMs = { nowMs },
        )

        try {
            feature.playbackStore.savePosition("media://one", 123L)
            feature.queueHistoryStore.push("media://one")

            assertTrue(feature.persistenceDegraded.value)

            nowMs += 6_000L
            feature.playbackStore.loadPosition("media://one")

            assertEquals(123L, resolvedPlaybackStore.loadPosition("media://one"))
            assertEquals(listOf("media://one"), resolvedQueueHistoryStore.items())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun fallbackStores_keepServingFallbackWhenRecoveryMigrationFails() = runBlocking {
        val application = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var attempts = 0
        var nowMs = 0L
        val feature = createFeature(
            application = application,
            scope = scope,
            persistenceStoresFactory = { _ ->
                attempts += 1
                if (attempts < 2) {
                    error("boom")
                }
                PlaybackPersistenceStores(
                    playbackStore = FailingPlaybackStore(),
                    queueHistoryStore = FakeQueueHistoryStore(),
                )
            },
            nowMs = { nowMs },
        )

        try {
            feature.playbackStore.savePosition("media://one", 123L)
            feature.queueHistoryStore.push("media://one")

            nowMs += 6_000L

            assertEquals(123L, feature.playbackStore.loadPosition("media://one"))
            assertEquals(listOf("media://one"), feature.queueHistoryStore.items())
            assertTrue(feature.persistenceDegraded.value)
        } finally {
            scope.cancel()
        }
    }

    private fun createFeature(
        application: Application,
        scope: CoroutineScope,
        persistenceStoresFactory: suspend (Context) -> PlaybackPersistenceStores,
        nowMs: () -> Long = System::currentTimeMillis,
    ): PlaybackRuntimeFeature {
        return PlaybackRuntimeFeature(
            application = application,
            playbackBehaviorRepository = PlaybackBehaviorRepository(
                store = FakeAppSettingsStore(),
                scope = scope,
            ),
            scope = scope,
            controllerConnectorFactory = FakePlaybackControllerConnectorFactory,
            playbackPlatformBindings = PlaybackPlatformBindings(
                playbackServiceComponent = ComponentName(application, Application::class.java),
                notificationSmallIconResId = 1,
            ),
            persistenceStoresFactory = persistenceStoresFactory,
            nowMs = nowMs,
        )
    }
}

private class FakeAppSettingsStore(
    initialSnapshot: AppSettingsSnapshot = AppSettingsSnapshot(),
) : AppSettingsStore {
    private val snapshotsState = MutableStateFlow(initialSnapshot)

    override val snapshots: StateFlow<AppSettingsSnapshot> = snapshotsState

    override fun loadSnapshot(): AppSettingsSnapshot = snapshotsState.value

    override suspend fun saveSnapshot(snapshot: AppSettingsSnapshot) {
        snapshotsState.value = snapshot
    }
}

private class FakePlaybackStore : com.asuka.player.contract.PlaybackStore {
    private val positions = mutableMapOf<String, Long>()

    override suspend fun recentMediaIds(limit: Int): List<String> = positions.keys.toList().takeLast(limit)

    override suspend fun loadPosition(mediaId: String): Long? = positions[mediaId]

    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        positions[mediaId] = positionMs
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = null
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) = Unit
    override suspend fun loadAudioTrackId(mediaId: String): String? = null
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) = Unit
    override suspend fun loadSubtitleTrackId(mediaId: String): String? = null
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) = Unit
    override suspend fun loadZoom(mediaId: String): Float? = null
    override suspend fun saveZoom(mediaId: String, zoom: Float) = Unit
}

private class FailingPlaybackStore : com.asuka.player.contract.PlaybackStore {
    override suspend fun recentMediaIds(limit: Int): List<String> = emptyList()
    override suspend fun loadPosition(mediaId: String): Long? = null
    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        error("migration write failed")
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = null
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) = Unit
    override suspend fun loadAudioTrackId(mediaId: String): String? = null
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) = Unit
    override suspend fun loadSubtitleTrackId(mediaId: String): String? = null
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) = Unit
    override suspend fun loadZoom(mediaId: String): Float? = null
    override suspend fun saveZoom(mediaId: String, zoom: Float) = Unit
}

private class FakeQueueHistoryStore : com.asuka.player.contract.QueueHistoryStore {
    private val items = mutableListOf<String>()

    override suspend fun push(mediaId: String) {
        items += mediaId
    }

    override suspend fun items(): List<String> = items.toList()
}

private object FakePlaybackControllerConnectorFactory : PlaybackControllerConnectorFactory {
    override fun create(
        context: Context,
        playbackServiceComponent: ComponentName,
    ): PlaybackControllerConnector {
        return object : PlaybackControllerConnector {
            override fun buildAsync(): ListenableFuture<androidx.media3.session.MediaController> {
                error("PlaybackControllerConnector should not be used in this test")
            }

            override fun asPlaybackController(mediaController: androidx.media3.session.MediaController): PlaybackController {
                error("PlaybackControllerConnector should not be used in this test")
            }

            override fun asTrackSelectionController(
                mediaController: androidx.media3.session.MediaController,
            ): PlaybackTrackSelectionController {
                error("PlaybackControllerConnector should not be used in this test")
            }

            override fun release() = Unit
        }
    }
}

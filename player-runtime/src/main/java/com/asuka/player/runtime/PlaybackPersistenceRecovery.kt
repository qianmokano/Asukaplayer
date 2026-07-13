package com.asuka.player.runtime

import android.content.Context
import android.util.Log
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.QueueHistoryStore
import com.asuka.player.data.PlaybackPersistenceStores
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PlaybackPersistenceResolver(
    private val context: Context,
    private val createStores: suspend (Context) -> PlaybackPersistenceStores,
    private val nowMs: () -> Long,
) {
    private val retryIntervalMs = 5_000L
    private val lock = Mutex()
    @Volatile
    private var stores: PlaybackPersistenceStores? = null
    private var fallbackState: FallbackState? = null
    private var lastRecoveryAttemptAtMs: Long = 0L
    private val _degraded = MutableStateFlow(false)
    val degraded: StateFlow<Boolean> = _degraded.asStateFlow()

    suspend fun resolve(): PlaybackPersistenceStores {
        return lock.withLock { resolveLocked() }
    }

    suspend fun <T> withPlaybackStore(block: suspend (PlaybackStore) -> T): T {
        return lock.withLock { block(resolveLocked().playbackStore) }
    }

    suspend fun <T> withQueueHistoryStore(block: suspend (QueueHistoryStore) -> T): T {
        return lock.withLock { block(resolveLocked().queueHistoryStore) }
    }

    private suspend fun resolveLocked(): PlaybackPersistenceStores {
        stores?.let { return it }
        fallbackState?.let { fallback ->
            if (shouldRetryRecovery()) {
                lastRecoveryAttemptAtMs = nowMs()
                createStoresSafely("Persistence recovery failed; keeping in-memory stores")?.let { recovered ->
                    if (migrateFallbackStateSafely(fallback, recovered)) {
                        stores = recovered
                        fallbackState = null
                        _degraded.value = false
                        return recovered
                    }
                }
            }
            return fallback.stores
        }
        createStoresSafely("Persistence resolution failed; degrading to in-memory stores")?.let { resolved ->
            stores = resolved
            _degraded.value = false
            return resolved
        }
        val fallbackStores = createFallbackStores()
        fallbackState = FallbackState(fallbackStores)
        stores = null
        lastRecoveryAttemptAtMs = nowMs()
        return fallbackStores
    }

    private fun shouldRetryRecovery(): Boolean {
        return nowMs() - lastRecoveryAttemptAtMs >= retryIntervalMs
    }

    private suspend fun createStoresSafely(errorMessage: String): PlaybackPersistenceStores? {
        return try {
            createStores(context)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, errorMessage, error)
            null
        }
    }

    private fun createFallbackStores(): PlaybackPersistenceStores {
        Log.w(TAG, "Falling back to in-memory persistence stores")
        _degraded.value = true
        return PlaybackPersistenceStores(
            playbackStore = RecoverableInMemoryPlaybackStore(),
            queueHistoryStore = RecoverableInMemoryQueueHistoryStore(),
        )
    }

    private suspend fun migrateFallbackState(
        fallbackState: FallbackState,
        resolved: PlaybackPersistenceStores,
    ) {
        val playbackSnapshot = fallbackState.playbackStore.snapshot()
        playbackSnapshot.recentMediaIds.forEach { mediaId ->
            playbackSnapshot.positions[mediaId]?.let { resolved.playbackStore.savePosition(mediaId, it) }
            playbackSnapshot.speeds[mediaId]?.let { resolved.playbackStore.savePlaybackSpeed(mediaId, it) }
            playbackSnapshot.audioTrackIds[mediaId]?.let { resolved.playbackStore.saveAudioTrackId(mediaId, it) }
            playbackSnapshot.subtitleTrackIds[mediaId]?.let { resolved.playbackStore.saveSubtitleTrackId(mediaId, it) }
            playbackSnapshot.zooms[mediaId]?.let { resolved.playbackStore.saveZoom(mediaId, it) }
        }
        fallbackState.queueHistoryStore.snapshot().forEach { mediaId ->
            resolved.queueHistoryStore.push(mediaId)
        }
    }

    private suspend fun migrateFallbackStateSafely(
        fallbackState: FallbackState,
        resolved: PlaybackPersistenceStores,
    ): Boolean {
        return try {
            migrateFallbackState(fallbackState, resolved)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "Persistence recovery migration failed; keeping in-memory stores", error)
            false
        }
    }

    private data class FallbackState(
        val stores: PlaybackPersistenceStores,
    ) {
        val playbackStore: RecoverableInMemoryPlaybackStore
            get() = stores.playbackStore as RecoverableInMemoryPlaybackStore
        val queueHistoryStore: RecoverableInMemoryQueueHistoryStore
            get() = stores.queueHistoryStore as RecoverableInMemoryQueueHistoryStore
    }

    private companion object {
        private const val TAG = "PersistenceResolver"
    }
}

private data class PlaybackStoreSnapshot(
    val positions: Map<String, Long>,
    val speeds: Map<String, Float>,
    val audioTrackIds: Map<String, String>,
    val subtitleTrackIds: Map<String, String>,
    val zooms: Map<String, Float>,
    val recentMediaIds: List<String>,
)

private class RecoverableInMemoryPlaybackStore : PlaybackStore {
    private val positions = ConcurrentHashMap<String, Long>()
    private val speeds = ConcurrentHashMap<String, Float>()
    private val audioTrackIds = ConcurrentHashMap<String, String>()
    private val subtitleTrackIds = ConcurrentHashMap<String, String>()
    private val zooms = ConcurrentHashMap<String, Float>()
    private val recentIds = LinkedHashSet<String>()

    override suspend fun recentMediaIds(limit: Int): List<String> {
        synchronized(recentIds) {
            return recentIds.toList().takeLast(limit).reversed()
        }
    }

    private fun touch(mediaId: String) {
        synchronized(recentIds) {
            recentIds.remove(mediaId)
            recentIds.add(mediaId)
        }
    }

    override suspend fun loadPosition(mediaId: String): Long? = positions[mediaId]
    override suspend fun savePosition(mediaId: String, positionMs: Long) {
        positions[mediaId] = positionMs
        touch(mediaId)
    }

    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = speeds[mediaId]
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) {
        speeds[mediaId] = speed
        touch(mediaId)
    }

    override suspend fun loadAudioTrackId(mediaId: String): String? = audioTrackIds[mediaId]
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) {
        audioTrackIds[mediaId] = trackId
        touch(mediaId)
    }

    override suspend fun loadSubtitleTrackId(mediaId: String): String? = subtitleTrackIds[mediaId]
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) {
        subtitleTrackIds[mediaId] = trackId
        touch(mediaId)
    }

    override suspend fun loadZoom(mediaId: String): Float? = zooms[mediaId]
    override suspend fun saveZoom(mediaId: String, zoom: Float) {
        zooms[mediaId] = zoom
        touch(mediaId)
    }

    fun snapshot(): PlaybackStoreSnapshot {
        synchronized(recentIds) {
            return PlaybackStoreSnapshot(
                positions = positions.toMap(),
                speeds = speeds.toMap(),
                audioTrackIds = audioTrackIds.toMap(),
                subtitleTrackIds = subtitleTrackIds.toMap(),
                zooms = zooms.toMap(),
                recentMediaIds = recentIds.toList(),
            )
        }
    }
}

private class RecoverableInMemoryQueueHistoryStore : QueueHistoryStore {
    private val lock = Any()
    private val deque = ArrayDeque<String>()

    override suspend fun push(mediaId: String) {
        synchronized(lock) {
            if (deque.lastOrNull() == mediaId) return
            deque.addLast(mediaId)
            while (deque.size > 50) {
                deque.removeFirst()
            }
        }
    }

    override suspend fun items(): List<String> {
        synchronized(lock) {
            return deque.toList()
        }
    }

    fun snapshot(): List<String> {
        synchronized(lock) {
            return deque.toList()
        }
    }
}

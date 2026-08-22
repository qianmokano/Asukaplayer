package com.asuka.player.app

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.asuka.player.data.AsukaMediaLibraryIndexDatabase
import com.asuka.player.data.IndexedVideoDao
import com.asuka.player.data.IndexedVideoEntity
import com.asuka.player.data.MediaLibrarySyncStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

internal class MediaLibraryIndexingCoordinator(
    context: Context,
    private val database: AsukaMediaLibraryIndexDatabase = AsukaMediaLibraryIndexDatabase.open(context),
    scope: CoroutineScope? = null,
    private val mediaStoreVolumeNamesReader: () -> Set<String> = {
        context.applicationContext.readMediaStoreVolumeNames()
    },
    private val currentGenerationReader: (String) -> Long? = { volumeName ->
        context.applicationContext.readMediaStoreGeneration(volumeName)
    },
    private val currentMediaStoreVersionReader: (String) -> String? = { volumeName ->
        context.applicationContext.readMediaStoreVersion(volumeName)
    },
) : AutoCloseable {
    private val ownsScope = scope == null
    private val scope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val mediaStoreIndexReader = MediaStoreIndexReader(appContext)
    private val dao: IndexedVideoDao = database.indexedVideoDao()
    private val syncStateDao = database.mediaLibrarySyncStateDao()
    private val syncMutex = Mutex()
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _syncFailures = MutableSharedFlow<MediaCatalogFailure>(extraBufferCapacity = 1)
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            markObservedChange(emptyList(), requiresFullReconcile = true)
            requestIncrementalSync()
        }
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            markObservedChange(listOfNotNull(uri), requiresFullReconcile = uri == null)
            requestIncrementalSync()
        }
        override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
            markObservedChange(listOfNotNull(uri), requiresFullReconcile = uri == null)
            requestIncrementalSync()
        }
        override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int) {
            markObservedChange(uris, requiresFullReconcile = uris.isEmpty())
            requestIncrementalSync()
        }
    }

    private var observerRegistered = false
    private var scheduledSyncJob: Job? = null
    private var pendingObservedIdsByVolume = mutableMapOf<String, MutableSet<Long>>()
    private var pendingRequiresFullReconcile = false
    private var closed = false
    val changes: Flow<Unit> = _changes.asSharedFlow()
    val syncFailures: Flow<MediaCatalogFailure> = _syncFailures.asSharedFlow()
    fun prepareForQueries() {
        registerObserverIfNeeded()
    }

    suspend fun syncNow(forceFullRescan: Boolean) {
        registerObserverIfNeeded()
        try {
            val changed = syncMutex.withLock { performSync(forceFullRescan = forceFullRescan) }
            if (changed) _changes.tryEmit(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: SecurityException) {
            emitSyncFailure(MediaCatalogFailure.PermissionDenied)
            throw error
        } catch (error: IllegalArgumentException) {
            emitSyncFailure(MediaCatalogFailure.ProviderUnavailable)
            throw error
        } catch (error: IllegalStateException) {
            emitSyncFailure(MediaCatalogFailure.ProviderUnavailable)
            throw error
        } catch (error: Exception) {
            emitSyncFailure(MediaCatalogFailure.Unknown)
            throw error
        }
    }

    internal fun recordObservedChangeForTest(uri: Uri) {
        markObservedChange(listOf(uri), requiresFullReconcile = false)
    }

    override fun close() {
        closed = true
        scheduledSyncJob?.cancel()
        scheduledSyncJob = null
        unregisterObserverIfNeeded()
        if (ownsScope) {
            scope.cancel()
        }
        database.close()
    }

    private fun requestIncrementalSync() {
        if (closed) return
        registerObserverIfNeeded()
        scheduledSyncJob?.cancel()
        scheduledSyncJob = scope.launch {
            delay(SYNC_DEBOUNCE_MS)
            try {
                syncNow(forceFullRescan = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Failure is surfaced via syncFailures; incremental sync keeps running.
            }
        }
    }

    private fun emitSyncFailure(failure: MediaCatalogFailure) {
        _syncFailures.tryEmit(failure)
    }

    private fun registerObserverIfNeeded() {
        if (closed || observerRegistered) return
        contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        observerRegistered = true
    }

    private fun unregisterObserverIfNeeded() {
        if (!observerRegistered) return
        runCatching { contentResolver.unregisterContentObserver(observer) }.also { observerRegistered = false }
    }

    private suspend fun performSync(
        forceFullRescan: Boolean,
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val (observedIdsByVolume, requiresFullReconcile) = consumeObservedChanges()
            val volumeNames = mediaStoreVolumeNamesReader().sorted()
            if (volumeNames.isEmpty()) {
                val deletedVideos = dao.deleteAll()
                syncStateDao.deleteAll()
                return@withContext deletedVideos > 0
            }

            var changed = dao.deleteByMissingVolumes(volumeNames) > 0
            syncStateDao.deleteByMissingVolumes(volumeNames)
            volumeNames.forEach { volumeName ->
                changed = syncVolume(
                    volumeName = volumeName,
                    observedIds = observedIdsByVolume[volumeName].orEmpty(),
                    requiresFullReconcile = requiresFullReconcile,
                    forceFullRescan = forceFullRescan,
                ) || changed
            }
            changed
        }
    }

    private fun syncVolume(
        volumeName: String,
        observedIds: Set<Long>,
        requiresFullReconcile: Boolean,
        forceFullRescan: Boolean,
    ): Boolean {
        val currentGeneration = currentGenerationReader(volumeName)
        val currentMediaStoreVersion = currentMediaStoreVersionReader(volumeName)
        val supportsGenerationTracking = currentGeneration != null
        val lastSyncState = syncStateDao.state(volumeName)
        val mediaStoreVersionChanged = currentMediaStoreVersion != null &&
            (lastSyncState == null || currentMediaStoreVersion != lastSyncState.mediaStoreVersion)
        val shouldForce = forceFullRescan ||
            mediaStoreVersionChanged ||
            (observedIds.isEmpty() && !requiresFullReconcile && dao.count(volumeName) == 0)
        val generationBaseline = if (shouldForce || !supportsGenerationTracking) {
            null
        } else {
            val added = dao.maxGenerationAdded(volumeName)?.takeIf { it > 0L }
            val modified = dao.maxGenerationModified(volumeName)?.takeIf { it > 0L }
            if (added != null || modified != null) {
                GenerationBaseline(
                    addedAfterExclusive = added ?: 0L,
                    modifiedAfterExclusive = modified ?: 0L,
                )
            } else {
                null
            }
        }
        val baselineModified = if (generationBaseline == null && !shouldForce) {
            dao.maxDateModifiedSec(volumeName)
        } else {
            null
        }
        val changedVideos = mediaStoreIndexReader.queryIndexedVideos(
            volumeName = volumeName,
            generationBaseline = generationBaseline,
            modifiedSinceInclusive = baselineModified,
        )
        var hadUpserts = false
        if (changedVideos.isNotEmpty()) {
            dao.upsertAll(changedVideos)
            hadUpserts = true
        }
        val removedIds = mutableSetOf<Long>()
        if (observedIds.isNotEmpty()) {
            val existingObservedIds = mediaStoreIndexReader.queryExistingIds(volumeName, observedIds)
            removedIds += observedIds.filterNot(existingObservedIds::contains)
            if (existingObservedIds.isNotEmpty()) {
                val observedVideos = mediaStoreIndexReader.queryIndexedVideosByIds(volumeName, existingObservedIds)
                if (observedVideos.isNotEmpty()) {
                    dao.upsertAll(observedVideos)
                    hadUpserts = true
                }
            }
        }
        val shouldReconcileAllIds = shouldForce ||
            requiresFullReconcile ||
            (currentGeneration != null &&
                (lastSyncState == null || currentGeneration > lastSyncState.lastReconciledGeneration))
        if (shouldReconcileAllIds) {
            val existingIds = dao.allMediaStoreIds(volumeName)
            val currentIds = if (shouldForce) {
                changedVideos.map(IndexedVideoEntity::mediaStoreId).toSet()
            } else {
                mediaStoreIndexReader.queryAllCurrentIds(volumeName)
            }
            val addedIds = currentIds.filterNot(existingIds::contains).toSet()
            removedIds += existingIds.filterNot(currentIds::contains)
            if (!supportsGenerationTracking && !shouldForce && addedIds.isNotEmpty()) {
                val addedVideos = mediaStoreIndexReader.queryIndexedVideosByIds(volumeName, addedIds)
                if (addedVideos.isNotEmpty()) {
                    dao.upsertAll(addedVideos)
                    hadUpserts = true
                }
            }
        }

        removedIds.chunked(MEDIA_STORE_ID_CHUNK_SIZE).forEach { chunk ->
            if (chunk.isNotEmpty()) {
                dao.deleteByMediaStoreIds(volumeName, chunk)
            }
        }
        if (shouldReconcileAllIds && (currentGeneration != null || currentMediaStoreVersion != null)) {
            syncStateDao.upsert(
                MediaLibrarySyncStateEntity(
                    volumeName = volumeName,
                    lastReconciledGeneration = currentGeneration
                        ?: lastSyncState?.lastReconciledGeneration
                        ?: 0L,
                    mediaStoreVersion = currentMediaStoreVersion,
                ),
            )
        }
        return hadUpserts || removedIds.isNotEmpty()
    }

    private fun markObservedChange(
        uris: Collection<Uri>,
        requiresFullReconcile: Boolean,
    ) {
        synchronized(this) {
            uris.mapNotNull(mediaStoreIndexReader::parseItem).forEach { item ->
                pendingObservedIdsByVolume.getOrPut(item.volumeName) { mutableSetOf() } += item.mediaStoreId
            }
            pendingRequiresFullReconcile =
                pendingRequiresFullReconcile ||
                    requiresFullReconcile ||
                    uris.any { mediaStoreIndexReader.parseItem(it) == null }
        }
    }

    private fun consumeObservedChanges(): Pair<Map<String, Set<Long>>, Boolean> {
        return synchronized(this) {
            val ids = pendingObservedIdsByVolume.mapValues { (_, ids) -> ids.toSet() }
            val requiresFullReconcile = pendingRequiresFullReconcile
            pendingObservedIdsByVolume.clear()
            pendingRequiresFullReconcile = false
            ids to requiresFullReconcile
        }
    }

    companion object {
        private const val SYNC_DEBOUNCE_MS = 750L
    }
}

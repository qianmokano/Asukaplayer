package com.asuka.player.app

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Environment
import android.provider.MediaStore
import com.asuka.player.R
import com.asuka.player.data.AsukaMediaLibraryIndexDatabase
import com.asuka.player.data.IndexedVideoDao
import com.asuka.player.data.IndexedVideoEntity
import java.io.File
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
    private val currentGenerationReader: () -> Long? = { context.applicationContext.readMediaStoreGeneration() },
) : AutoCloseable {
    private val ownsScope = scope == null
    private val scope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val dao: IndexedVideoDao = database.indexedVideoDao()
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
    private var pendingObservedIds = mutableSetOf<Long>()
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
            val (observedIds, requiresFullReconcile) = consumeObservedChanges()
            val shouldForce = forceFullRescan || (observedIds.isEmpty() && !requiresFullReconcile && dao.count() == 0)
            val currentGeneration = currentGenerationReader()
            val supportsGenerationTracking = currentGeneration != null
            val generationBaseline = if (shouldForce || !supportsGenerationTracking) {
                null
            } else {
                val added = dao.maxGenerationAdded()?.takeIf { it > 0L }
                val modified = dao.maxGenerationModified()?.takeIf { it > 0L }
                if (added != null || modified != null) {
                    GenerationBaseline(
                        addedAfterExclusive = added ?: 0L,
                        modifiedAfterExclusive = modified ?: 0L,
                    )
                } else {
                    null
                }
            }
            val baselineModified = if (generationBaseline == null && !shouldForce) dao.maxDateModifiedSec() else null
            val changedVideos = queryIndexedVideos(
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
                val existingObservedIds = queryExistingIds(observedIds)
                removedIds += observedIds.filterNot(existingObservedIds::contains)
                if (existingObservedIds.isNotEmpty()) {
                    val observedVideos = queryIndexedVideosByIds(existingObservedIds)
                    if (observedVideos.isNotEmpty()) {
                        dao.upsertAll(observedVideos)
                        hadUpserts = true
                    }
                }
            }

            val shouldReconcileAllIds = shouldForce ||
                requiresFullReconcile ||
                (currentGeneration != null &&
                    generationBaseline != null &&
                    currentGeneration > maxOf(generationBaseline.addedAfterExclusive, generationBaseline.modifiedAfterExclusive))
            if (shouldReconcileAllIds) {
                val existingIds = dao.allIds()
                val currentIds = if (shouldForce) changedVideos.map(IndexedVideoEntity::mediaStoreId).toSet() else queryAllCurrentIds()
                val addedIds = currentIds.filterNot(existingIds::contains).toSet()
                removedIds += existingIds.filterNot(currentIds::contains)
                if (!supportsGenerationTracking && !shouldForce && addedIds.isNotEmpty()) {
                    val addedVideos = queryIndexedVideosByIds(addedIds)
                    if (addedVideos.isNotEmpty()) {
                        dao.upsertAll(addedVideos)
                        hadUpserts = true
                    }
                }
            }

            removedIds.chunked(DELETE_CHUNK_SIZE).forEach { chunk ->
                if (chunk.isNotEmpty()) {
                    dao.deleteByIds(chunk)
                }
            }
            hadUpserts || removedIds.isNotEmpty()
        }
    }

    private fun markObservedChange(
        uris: Collection<Uri>,
        requiresFullReconcile: Boolean,
    ) {
        synchronized(this) {
            pendingObservedIds += uris.mapNotNull(::parseMediaStoreId)
            pendingRequiresFullReconcile =
                pendingRequiresFullReconcile || requiresFullReconcile || uris.any { parseMediaStoreId(it) == null }
        }
    }

    private suspend fun consumeObservedChanges(): Pair<Set<Long>, Boolean> {
        return synchronized(this) {
            val ids = pendingObservedIds.toSet()
            val requiresFullReconcile = pendingRequiresFullReconcile
            pendingObservedIds.clear()
            pendingRequiresFullReconcile = false
            ids to requiresFullReconcile
        }
    }

    private fun queryAllCurrentIds(): Set<Long> {
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            baseSelection(),
            null,
            null,
        ) ?: throw IllegalStateException("MediaStore query returned a null cursor.")
        return cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(idIdx))
                }
            }
        }
    }

    private fun queryIndexedVideos(
        generationBaseline: GenerationBaseline?,
        modifiedSinceInclusive: Long?,
    ): List<IndexedVideoEntity> {
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaStoreProjection(),
            buildSelection(
                base = baseSelection(),
                generationBaseline = generationBaseline,
                modifiedSinceInclusive = modifiedSinceInclusive,
            ),
            buildSelectionArgs(
                generationBaseline = generationBaseline,
                modifiedSinceInclusive = modifiedSinceInclusive,
            ),
            buildSortOrder(generationBaseline),
        ) ?: throw IllegalStateException("MediaStore query returned a null cursor.")
        return cursor.use { readIndexedVideos(it) }
    }

    private fun queryExistingIds(ids: Set<Long>): Set<Long> {
        if (ids.isEmpty()) return emptySet()
        val placeholders = ids.joinToString(",") { "?" }
        val selection = listOfNotNull(baseSelection(), "${MediaStore.Video.Media._ID} IN ($placeholders)")
            .joinToString(" AND ")
        val cursor = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Video.Media._ID),
            selection,
            ids.map(Long::toString).toTypedArray(),
            null,
        ) ?: throw IllegalStateException("MediaStore query returned a null cursor.")
        return cursor.use {
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            buildSet {
                while (cursor.moveToNext()) {
                    add(cursor.getLong(idIdx))
                }
            }
        }
    }

    private fun queryIndexedVideosByIds(ids: Set<Long>): List<IndexedVideoEntity> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(DELETE_CHUNK_SIZE).flatMap { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = listOfNotNull(baseSelection(), "${MediaStore.Video.Media._ID} IN ($placeholders)")
                .joinToString(" AND ")
            val cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mediaStoreProjection(),
                selection,
                chunk.map(Long::toString).toTypedArray(),
                "${MediaStore.Video.Media._ID} ASC",
            ) ?: throw IllegalStateException("MediaStore query returned a null cursor.")
            cursor.use(::readIndexedVideos)
        }
    }

    private fun readIndexedVideos(cursor: android.database.Cursor): List<IndexedVideoEntity> {
        val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dataPathIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
        val folderNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
        val folderIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
        val dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val dateModifiedIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
        val generationAddedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.GENERATION_ADDED)
        val generationModifiedIdx = cursor.getColumnIndex(MediaStore.MediaColumns.GENERATION_MODIFIED)

        return buildList {
            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idIdx)
                val uri = android.content.ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    mediaStoreId,
                ).toString()
                val fallbackFolderName = cursor.getString(folderNameIdx)
                    ?.takeIf { it.isNotBlank() }
                    ?: appContext.getString(R.string.unknown_folder)
                add(
                    IndexedVideoEntity(
                        mediaStoreId = mediaStoreId,
                        uri = uri,
                        title = cursor.getString(titleIdx) ?: uri.substringAfterLast('/'),
                        durationMs = cursor.getLong(durationIdx).coerceAtLeast(0L),
                        sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L),
                        folderName = fallbackFolderName,
                        folderPath = resolveFolderPath(
                            rawDataPath = if (dataPathIdx >= 0) cursor.getString(dataPathIdx) else null,
                            fallbackFolderName = fallbackFolderName,
                        ),
                        folderId = cursor.getLong(folderIdIdx),
                        dateAddedSec = cursor.getLong(dateAddedIdx).coerceAtLeast(0L),
                        dateModifiedSec = cursor.getLong(dateModifiedIdx).coerceAtLeast(0L),
                        generationAdded = if (generationAddedIdx >= 0) cursor.getLong(generationAddedIdx).coerceAtLeast(0L) else 0L,
                        generationModified = if (generationModifiedIdx >= 0) cursor.getLong(generationModifiedIdx).coerceAtLeast(0L) else 0L,
                    ),
                )
            }
        }
    }

    private fun mediaStoreProjection(): Array<String> {
        val columns = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            columns += MediaStore.MediaColumns.GENERATION_ADDED
            columns += MediaStore.MediaColumns.GENERATION_MODIFIED
        }
        return columns.toTypedArray()
    }

    private fun resolveFolderPath(
        rawDataPath: String?,
        fallbackFolderName: String,
    ): String {
        return rawDataPath
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it).parent }
            ?.takeIf { it.isNotBlank() }
            ?.replace(
                Environment.getExternalStorageDirectory().absolutePath,
                appContext.getString(R.string.internal_storage),
            )
            ?: fallbackFolderName
    }

    private fun baseSelection(): String? {
        return if (Build.VERSION.SDK_INT >= 29) {
            "${MediaStore.Video.Media.IS_PENDING}=0"
        } else {
            null
        }
    }

    private fun parseMediaStoreId(uri: Uri): Long? {
        val lastSegment = uri.lastPathSegment?.toLongOrNull() ?: return null
        return lastSegment.takeIf { uri.authority == MediaStore.AUTHORITY }
    }

    companion object {
        private const val SYNC_DEBOUNCE_MS = 750L
        private const val DELETE_CHUNK_SIZE = 900
    }
}

private fun Context.readMediaStoreGeneration(): Long? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            MediaStore.getGeneration(this, MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }.getOrNull()
    } else {
        null
    }

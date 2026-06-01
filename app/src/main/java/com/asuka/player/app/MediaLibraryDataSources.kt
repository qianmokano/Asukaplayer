package com.asuka.player.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.QueueHistoryRepository
import com.asuka.player.data.AsukaMediaLibraryIndexDatabase
import com.asuka.player.data.IndexedFolderSummaryRow
import com.asuka.player.data.IndexedVideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val MEDIA_STORE_ID_PREFIX = "media-store:"

internal interface VideoAccessDataSource {
    fun readVideoAccessState(): VideoAccessState
}

internal interface LocalVideoCatalogDataSource {
    val changes: Flow<Unit>

    suspend fun syncIndex(forceFullRescan: Boolean = false)

    suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder>

    suspend fun loadVideoPage(
        request: MediaLibraryPageRequest,
        folderId: Long? = null,
    ): MediaLibraryPage<LocalVideoItem>

    suspend fun resolveMediaIds(mediaIds: List<String>): Map<String, LocalVideoItem>

    suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int)

    fun close() = Unit
}

internal interface RecentPlaybackDataSource {
    suspend fun loadRecentMediaIds(limit: Int): List<String>
}

internal class AndroidVideoAccessDataSource(
    context: Context,
) : VideoAccessDataSource {
    private val appContext = context.applicationContext

    override fun readVideoAccessState(): VideoAccessState {
        val permissionGranted = hasFullVideoPermission(appContext)
        return VideoAccessState(
            permissionGranted = permissionGranted,
            userSelectedPermissionGranted = hasUserSelectedVideoPermission(appContext) && !permissionGranted,
        )
    }

    private fun hasFullVideoPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasUserSelectedVideoPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return false
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

internal class AndroidMediaStoreVideoCatalogDataSource(
    context: Context,
    database: AsukaMediaLibraryIndexDatabase = AsukaMediaLibraryIndexDatabase.open(context),
    private val playbackStateRepositoryProvider: (() -> PlaybackStateRepository)? = null,
) : LocalVideoCatalogDataSource {
    private val appContext = context.applicationContext
    private val dao = database.indexedVideoDao()
    private val indexingCoordinator = MediaLibraryIndexingCoordinator(
        context = appContext,
        database = database,
    )

    override val changes: Flow<Unit> = indexingCoordinator.changes

    override suspend fun syncIndex(forceFullRescan: Boolean) {
        indexingCoordinator.syncNow(forceFullRescan)
    }

    override suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder> {
        return withContext(Dispatchers.IO) {
            indexingCoordinator.prepareForQueries()
            val rows = dao.pagedFolders(limit = request.limit + 1, offset = request.offset)
            val pageItems = rows.take(request.limit).map(IndexedFolderSummaryRow::toLocalFolder)
            MediaLibraryPage(
                items = pageItems,
                nextOffset = if (rows.size > request.limit) request.offset + pageItems.size else null,
                totalCount = dao.folderCount(),
            )
        }
    }

    override suspend fun loadVideoPage(
        request: MediaLibraryPageRequest,
        folderId: Long?,
    ): MediaLibraryPage<LocalVideoItem> {
        return withContext(Dispatchers.IO) {
            indexingCoordinator.prepareForQueries()
            val rows = if (folderId == null) {
                dao.pagedVideos(limit = request.limit + 1, offset = request.offset)
            } else {
                dao.pagedVideosByFolder(folderId = folderId, limit = request.limit + 1, offset = request.offset)
            }
            val pageItems = enrichWithResumePositions(
                items = rows.take(request.limit).map(IndexedVideoEntity::toLocalVideoItem),
                playbackStateRepositoryProvider = playbackStateRepositoryProvider,
            )
            MediaLibraryPage(
                items = pageItems,
                nextOffset = if (rows.size > request.limit) request.offset + pageItems.size else null,
                totalCount = if (folderId == null) dao.count() else dao.countByFolder(folderId),
            )
        }
    }

    override suspend fun resolveMediaIds(mediaIds: List<String>): Map<String, LocalVideoItem> {
        return withContext(Dispatchers.IO) {
            indexingCoordinator.prepareForQueries()
            val ids = mediaIds.mapNotNull(::parseMediaStoreId).distinct()
            if (ids.isEmpty()) return@withContext emptyMap()
            enrichWithResumePositions(
                items = dao.findByIds(ids).map(IndexedVideoEntity::toLocalVideoItem),
                playbackStateRepositoryProvider = playbackStateRepositoryProvider,
            ).associateBy { item ->
                item.playbackMediaId
            }
        }
    }

    private suspend fun enrichWithResumePositions(
        items: List<LocalVideoItem>,
        playbackStateRepositoryProvider: (() -> PlaybackStateRepository)?,
    ): List<LocalVideoItem> {
        if (items.isEmpty() || playbackStateRepositoryProvider == null) return items
        val playbackStateRepository = playbackStateRepositoryProvider()
        return items.map { item ->
            val resumePositionMs = playbackStateRepository.resolveResumePositionMs(item)
            if (resumePositionMs <= 0L) {
                item
            } else {
                item.copy(resumePositionMs = resumePositionMs)
            }
        }
    }

    override suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int) {
        withContext(Dispatchers.IO) {
            warmupInitialThumbnails(
                context = appContext,
                videos = videos,
                limit = limit,
            )
        }
    }

    override fun close() {
        indexingCoordinator.close()
    }

    private fun parseMediaStoreId(mediaId: String): Long? {
        return mediaId.removePrefix(MEDIA_STORE_ID_PREFIX)
            .takeIf { mediaId.startsWith(MEDIA_STORE_ID_PREFIX) }
            ?.toLongOrNull()
    }
}

internal class PlaybackRecentMediaDataSource(
    private val playbackStateRepositoryProvider: () -> PlaybackStateRepository,
    private val queueHistoryRepositoryProvider: () -> QueueHistoryRepository,
) : RecentPlaybackDataSource {
    override suspend fun loadRecentMediaIds(limit: Int): List<String> {
        return withContext(Dispatchers.IO) {
            val queueHistoryRepository = queueHistoryRepositoryProvider()
            val playbackStateRepository = playbackStateRepositoryProvider()
            resolveRecentMediaIds(
                historyMediaIds = queueHistoryRepository.items(),
                fallbackMediaIds = playbackStateRepository.recentMediaIds(limit = limit),
                limit = limit,
            )
        }
    }
}

private fun IndexedFolderSummaryRow.toLocalFolder(): LocalVideoFolder {
    return LocalVideoFolder(
        id = folderId,
        name = folderName,
        videoCount = videoCount,
        totalDurationMs = totalDurationMs,
        totalSizeBytes = totalSizeBytes,
    )
}

private fun IndexedVideoEntity.toLocalVideoItem(): LocalVideoItem {
    return LocalVideoItem(
        id = mediaStoreId,
        uri = android.net.Uri.parse(uri),
        title = title,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        folderName = folderName,
        folderPath = folderPath,
        folderId = folderId,
        dateAddedSec = dateAddedSec,
    )
}

private fun IndexedVideoEntity.toPlaybackMediaId(): String = "$MEDIA_STORE_ID_PREFIX$mediaStoreId"

internal suspend fun PlaybackStateRepository.resolveResumePositionMs(
    item: LocalVideoItem,
): Long {
    return readResumeState(item.playbackMediaId).positionMs.coerceAtLeast(0L)
}

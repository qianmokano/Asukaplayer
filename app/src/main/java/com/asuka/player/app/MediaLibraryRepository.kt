package com.asuka.player.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

private const val DEFAULT_FOLDER_PAGE_SIZE = 40
private const val DEFAULT_VIDEO_PAGE_SIZE = 60

internal data class VideoAccessState(
    val permissionGranted: Boolean,
    val userSelectedPermissionGranted: Boolean,
)

internal data class MediaLibraryPageRequest(
    val offset: Int = 0,
    val limit: Int,
) {
    init {
        require(offset >= 0) { "offset must be >= 0" }
        require(limit > 0) { "limit must be > 0" }
    }
}

internal interface MediaLibraryRepository {
    val changes: Flow<Unit>

    fun readVideoAccessState(): VideoAccessState

    suspend fun syncIndex(forceFullRescan: Boolean = false)

    suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder>

    suspend fun loadVideoPage(
        request: MediaLibraryPageRequest,
        folderId: Long? = null,
    ): MediaLibraryPage<LocalVideoItem>

    suspend fun resolveRecentMediaItems(mediaIds: List<String>): Map<String, LocalVideoItem>

    suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int)

    suspend fun loadRecentMediaIds(limit: Int = 100): List<String>
}

internal class AndroidMediaLibraryRepository(
    private val videoAccessDataSource: VideoAccessDataSource,
    private val localVideoCatalogDataSource: LocalVideoCatalogDataSource,
    private val recentPlaybackDataSource: RecentPlaybackDataSource,
) : MediaLibraryRepository {
    override val changes: Flow<Unit> = localVideoCatalogDataSource.changes

    override fun readVideoAccessState(): VideoAccessState {
        return videoAccessDataSource.readVideoAccessState()
    }

    override suspend fun syncIndex(forceFullRescan: Boolean) {
        localVideoCatalogDataSource.syncIndex(forceFullRescan)
    }

    override suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder> {
        return localVideoCatalogDataSource.loadFolderPage(request)
    }

    override suspend fun loadVideoPage(
        request: MediaLibraryPageRequest,
        folderId: Long?,
    ): MediaLibraryPage<LocalVideoItem> {
        return localVideoCatalogDataSource.loadVideoPage(
            request = request,
            folderId = folderId,
        )
    }

    override suspend fun resolveRecentMediaItems(mediaIds: List<String>): Map<String, LocalVideoItem> {
        return localVideoCatalogDataSource.resolveMediaIds(mediaIds)
    }

    override suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int) {
        localVideoCatalogDataSource.warmupInitialThumbnails(videos, limit)
    }

    override suspend fun loadRecentMediaIds(limit: Int): List<String> {
        return recentPlaybackDataSource.loadRecentMediaIds(limit)
    }
}

internal class ResolveVideoAccessUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
) {
    operator fun invoke(): VideoAccessState = mediaLibraryRepository.readVideoAccessState()
}

internal class LoadFolderPageUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val pageSize: Int = DEFAULT_FOLDER_PAGE_SIZE,
    private val minRefreshAnimMs: Long = 500L,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        offset: Int,
        hasLoadedOnce: Boolean,
        syncIndex: Boolean,
        forceFullRescan: Boolean = !hasLoadedOnce,
    ): MediaCatalogOutcome<LocalVideoFolder> {
        val startedAtMs = nowMs()
        return try {
            if (syncIndex) {
                mediaLibraryRepository.syncIndex(forceFullRescan = forceFullRescan)
            }
            val page = mediaLibraryRepository.loadFolderPage(
                request = MediaLibraryPageRequest(
                    offset = offset,
                    limit = pageSize,
                ),
            )
            if (hasLoadedOnce && offset == 0) {
                val remaining = minRefreshAnimMs - (nowMs() - startedAtMs)
                if (remaining > 0L) {
                    delay(remaining)
                }
            }
            MediaCatalogOutcome.Success(page = page)
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.PermissionDenied)
        } catch (error: IllegalArgumentException) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.ProviderUnavailable)
        } catch (error: IllegalStateException) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.ProviderUnavailable)
        } catch (_: Exception) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.Unknown)
        }
    }
}

internal class LoadVideoPageUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val pageSize: Int = DEFAULT_VIDEO_PAGE_SIZE,
    private val minRefreshAnimMs: Long = 500L,
    private val initialThumbWarmupLimit: Int = INITIAL_THUMB_WARMUP_LIMIT,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        offset: Int,
        folderId: Long?,
        hasLoadedOnce: Boolean,
        syncIndex: Boolean,
    ): MediaCatalogOutcome<LocalVideoItem> {
        val startedAtMs = nowMs()
        return try {
            if (syncIndex) {
                mediaLibraryRepository.syncIndex(forceFullRescan = !hasLoadedOnce)
            }
            val page = mediaLibraryRepository.loadVideoPage(
                request = MediaLibraryPageRequest(
                    offset = offset,
                    limit = pageSize,
                ),
                folderId = folderId,
            )
            if (hasLoadedOnce && offset == 0) {
                val remaining = minRefreshAnimMs - (nowMs() - startedAtMs)
                if (remaining > 0L) {
                    delay(remaining)
                }
            }
            MediaCatalogOutcome.Success(
                page = page,
                warmupVideos = if (offset == 0) page.items.take(initialThumbWarmupLimit) else emptyList(),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: SecurityException) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.PermissionDenied)
        } catch (error: IllegalArgumentException) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.ProviderUnavailable)
        } catch (error: IllegalStateException) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.ProviderUnavailable)
        } catch (_: Exception) {
            MediaCatalogOutcome.Failure(MediaCatalogFailure.Unknown)
        }
    }

    suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>) {
        if (videos.isEmpty()) return
        mediaLibraryRepository.warmupInitialThumbnails(
            videos = videos,
            limit = videos.size,
        )
    }
}

internal class ResolveRecentMediaItemsUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
) {
    suspend operator fun invoke(mediaIds: List<String>): Map<String, LocalVideoItem> {
        return mediaLibraryRepository.resolveRecentMediaItems(mediaIds)
    }
}

internal class ObserveMediaLibraryChangesUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
) {
    operator fun invoke(): Flow<Unit> = mediaLibraryRepository.changes
}

internal class LoadRecentMediaIdsUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
) {
    suspend operator fun invoke(limit: Int = 100): List<String> {
        return mediaLibraryRepository.loadRecentMediaIds(limit)
    }
}

internal fun resolveRecentMediaIds(
    historyMediaIds: List<String>,
    fallbackMediaIds: List<String>,
    limit: Int = 100,
): List<String> {
    val safeLimit = limit.coerceAtLeast(0)
    if (safeLimit == 0) return emptyList()
    val historyIds = historyMediaIds
        .asReversed()
        .filter { it.isNotBlank() }
        .distinct()
        .take(safeLimit)
    return historyIds.ifEmpty { fallbackMediaIds.take(safeLimit) }
}

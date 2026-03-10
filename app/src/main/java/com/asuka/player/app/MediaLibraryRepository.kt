package com.asuka.player.app

import kotlinx.coroutines.delay

internal data class VideoAccessState(
    val permissionGranted: Boolean,
    val userSelectedPermissionGranted: Boolean,
)

internal interface MediaLibraryRepository {
    fun readVideoAccessState(): VideoAccessState

    suspend fun scanLocalVideos(): List<LocalVideoItem>

    suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int)

    suspend fun loadRecentMediaIds(limit: Int = 100): List<String>
}

internal class AndroidMediaLibraryRepository(
    private val videoAccessDataSource: VideoAccessDataSource,
    private val localVideoCatalogDataSource: LocalVideoCatalogDataSource,
    private val recentPlaybackDataSource: RecentPlaybackDataSource,
) : MediaLibraryRepository {
    override fun readVideoAccessState(): VideoAccessState {
        return videoAccessDataSource.readVideoAccessState()
    }

    override suspend fun scanLocalVideos(): List<LocalVideoItem> {
        return localVideoCatalogDataSource.scanLocalVideos()
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

internal data class MediaLibraryRefreshResult(
    val items: List<LocalVideoItem>,
    val warmupVideos: List<LocalVideoItem>,
)

internal class RefreshMediaLibraryUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
    private val minRefreshAnimMs: Long = 500L,
    private val initialThumbWarmupLimit: Int = INITIAL_THUMB_WARMUP_LIMIT,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(hasLoadedOnce: Boolean): MediaLibraryRefreshResult {
        val startedAtMs = nowMs()
        val items = mediaLibraryRepository.scanLocalVideos()
        if (hasLoadedOnce) {
            val remaining = minRefreshAnimMs - (nowMs() - startedAtMs)
            if (remaining > 0L) {
                delay(remaining)
            }
        }
        return MediaLibraryRefreshResult(
            items = items,
            warmupVideos = if (hasLoadedOnce) emptyList() else items.take(initialThumbWarmupLimit),
        )
    }

    suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>) {
        if (videos.isEmpty()) return
        mediaLibraryRepository.warmupInitialThumbnails(
            videos = videos,
            limit = videos.size,
        )
    }
}

internal class LoadRecentMediaIdsUseCase(
    private val mediaLibraryRepository: MediaLibraryRepository,
) {
    suspend operator fun invoke(limit: Int = 100): List<String> {
        return mediaLibraryRepository.loadRecentMediaIds(limit)
    }
}

internal fun resolveRecentMediaIds(
    historyUris: List<android.net.Uri>,
    fallbackMediaIds: List<String>,
): List<String> {
    val historyIds = historyUris
        .asReversed()
        .map(android.net.Uri::toString)
        .distinct()
    return historyIds.ifEmpty { fallbackMediaIds }
}

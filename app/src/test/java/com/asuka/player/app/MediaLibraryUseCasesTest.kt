package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MediaLibraryUseCasesTest {

    @Test
    fun loadVideoPageUseCase_returnsWarmupVideosOnFirstPage() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            videos = listOf(
                localVideoItem(id = 1L, name = "one.mp4"),
                localVideoItem(id = 2L, name = "two.mp4"),
                localVideoItem(id = 3L, name = "three.mp4"),
            ),
        )
        val useCase = LoadVideoPageUseCase(
            mediaLibraryRepository = repository,
            minRefreshAnimMs = 0L,
            initialThumbWarmupLimit = 2,
        )

        val result = assertIs<MediaCatalogOutcome.Success<LocalVideoItem>>(
            useCase(offset = 0, folderId = null, hasLoadedOnce = false, syncIndex = false),
        )
        useCase.warmupInitialThumbnails(result.warmupVideos)

        assertEquals(listOf(1L, 2L), result.warmupVideos.map(LocalVideoItem::id))
        assertEquals(1, repository.warmupCallCount)
        assertEquals(2, repository.lastWarmupLimit)
    }

    @Test
    fun loadFolderPageUseCase_mapsProviderFailureToOutcome() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            folderFailure = IllegalStateException("provider unavailable"),
        )
        val useCase = LoadFolderPageUseCase(
            mediaLibraryRepository = repository,
            minRefreshAnimMs = 0L,
        )

        val result = useCase(offset = 0, hasLoadedOnce = true, syncIndex = false)

        assertEquals(
            MediaCatalogOutcome.Failure(MediaCatalogFailure.ProviderUnavailable),
            result,
        )
    }

    @Test
    fun loadVideoPageUseCase_mapsPermissionFailureToOutcome() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            videoFailure = SecurityException("permission denied"),
        )
        val useCase = LoadVideoPageUseCase(
            mediaLibraryRepository = repository,
            minRefreshAnimMs = 0L,
        )

        val result = useCase(offset = 0, folderId = null, hasLoadedOnce = true, syncIndex = false)

        assertEquals(
            MediaCatalogOutcome.Failure(MediaCatalogFailure.PermissionDenied),
            result,
        )
    }

    @Test
    fun resolveVideoAccessRecentIdsAndRecentItems_delegateToRepository() = runBlocking {
        val knownVideo = localVideoItem(id = 1L, name = "current.mp4")
        val repository = FakeMediaLibraryRepository(
            accessState = VideoAccessState(
                permissionGranted = false,
                userSelectedPermissionGranted = true,
            ),
            recentMediaIds = listOf(knownVideo.playbackMediaId),
            recentItems = mapOf(knownVideo.playbackMediaId to knownVideo),
        )

        val access = ResolveVideoAccessUseCase(repository)()
        val recentIds = LoadRecentMediaIdsUseCase(repository)(limit = 10)
        val recentItems = ResolveRecentMediaItemsUseCase(repository)(recentIds)

        assertEquals(false, access.permissionGranted)
        assertEquals(true, access.userSelectedPermissionGranted)
        assertEquals(listOf(knownVideo.playbackMediaId), recentIds)
        assertEquals(mapOf(knownVideo.playbackMediaId to knownVideo), recentItems)
    }
}

private class FakeMediaLibraryRepository(
    private val accessState: VideoAccessState = VideoAccessState(
        permissionGranted = true,
        userSelectedPermissionGranted = false,
    ),
    private val folders: List<LocalVideoFolder> = emptyList(),
    private val videos: List<LocalVideoItem> = emptyList(),
    private val recentMediaIds: List<String> = emptyList(),
    private val recentItems: Map<String, LocalVideoItem> = emptyMap(),
    private val folderFailure: Exception? = null,
    private val videoFailure: Exception? = null,
) : MediaLibraryRepository {
    override val changes: Flow<Unit> = emptyFlow()
    var warmupCallCount: Int = 0
        private set
    var lastWarmupLimit: Int = 0
        private set

    override fun readVideoAccessState(): VideoAccessState = accessState

    override suspend fun syncIndex(forceFullRescan: Boolean) = Unit

    override suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder> {
        folderFailure?.let { throw it }
        val items = folders.drop(request.offset).take(request.limit)
        val nextOffset = (request.offset + items.size).takeIf { it < folders.size }
        return MediaLibraryPage(items = items, nextOffset = nextOffset)
    }

    override suspend fun loadVideoPage(
        request: MediaLibraryPageRequest,
        folderId: Long?,
    ): MediaLibraryPage<LocalVideoItem> {
        videoFailure?.let { throw it }
        val source = videos.filter { folderId == null || it.folderId == folderId }
        val items = source.drop(request.offset).take(request.limit)
        val nextOffset = (request.offset + items.size).takeIf { it < source.size }
        return MediaLibraryPage(items = items, nextOffset = nextOffset)
    }

    override suspend fun resolveRecentMediaItems(mediaIds: List<String>): Map<String, LocalVideoItem> {
        return recentItems.filterKeys(mediaIds::contains)
    }

    override suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int) {
        warmupCallCount += 1
        lastWarmupLimit = limit
    }

    override suspend fun loadRecentMediaIds(limit: Int): List<String> = recentMediaIds.take(limit)
}

private fun localVideoItem(id: Long, name: String): LocalVideoItem {
    return LocalVideoItem(
        id = id,
        uri = Uri.parse("content://videos/$name"),
        title = name,
        durationMs = 1_000L,
        sizeBytes = 2_000L,
        folderName = "Folder",
        folderPath = "/storage/emulated/0/Folder",
        folderId = 42L,
        dateAddedSec = 1L,
    )
}

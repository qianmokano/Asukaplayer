package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MediaLibraryUseCasesTest {

    @Test
    fun refreshMediaLibraryUseCase_returnsWarmupVideosOnFirstLoad() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            videos = listOf(
                localVideoItem(id = 1L, name = "one.mp4"),
                localVideoItem(id = 2L, name = "two.mp4"),
                localVideoItem(id = 3L, name = "three.mp4"),
            ),
        )
        val useCase = RefreshMediaLibraryUseCase(
            mediaLibraryRepository = repository,
            minRefreshAnimMs = 0L,
            initialThumbWarmupLimit = 2,
        )

        val result = assertIs<MediaLibraryRefreshOutcome.Success>(useCase(hasLoadedOnce = false))
        useCase.warmupInitialThumbnails(result.warmupVideos)

        assertEquals(listOf(1L, 2L), result.warmupVideos.map(LocalVideoItem::id))
        assertEquals(1, repository.warmupCallCount)
        assertEquals(2, repository.lastWarmupLimit)
    }

    @Test
    fun refreshMediaLibraryUseCase_mapsProviderFailureToOutcome() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            scanFailure = IllegalStateException("provider unavailable"),
        )
        val useCase = RefreshMediaLibraryUseCase(
            mediaLibraryRepository = repository,
            minRefreshAnimMs = 0L,
        )

        val result = useCase(hasLoadedOnce = true)

        assertEquals(
            MediaLibraryRefreshOutcome.Failure(MediaLibraryRefreshFailure.ProviderUnavailable),
            result,
        )
    }

    @Test
    fun refreshMediaLibraryUseCase_mapsPermissionFailureToOutcome() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            scanFailure = SecurityException("permission denied"),
        )
        val useCase = RefreshMediaLibraryUseCase(
            mediaLibraryRepository = repository,
            minRefreshAnimMs = 0L,
        )

        val result = useCase(hasLoadedOnce = true)

        assertEquals(
            MediaLibraryRefreshOutcome.Failure(MediaLibraryRefreshFailure.PermissionDenied),
            result,
        )
    }

    @Test
    fun resolveVideoAccessAndRecentIds_delegateToRepository() = runBlocking {
        val repository = FakeMediaLibraryRepository(
            accessState = VideoAccessState(
                permissionGranted = false,
                userSelectedPermissionGranted = true,
            ),
            recentMediaIds = listOf("content://videos/current.mp4"),
        )

        val access = ResolveVideoAccessUseCase(repository)()
        val recentIds = LoadRecentMediaIdsUseCase(repository)(limit = 10)

        assertEquals(false, access.permissionGranted)
        assertEquals(true, access.userSelectedPermissionGranted)
        assertEquals(listOf("content://videos/current.mp4"), recentIds)
    }
}

private class FakeMediaLibraryRepository(
    private val accessState: VideoAccessState = VideoAccessState(
        permissionGranted = true,
        userSelectedPermissionGranted = false,
    ),
    private val videos: List<LocalVideoItem> = emptyList(),
    private val recentMediaIds: List<String> = emptyList(),
    private val scanFailure: Exception? = null,
) : MediaLibraryRepository {
    var warmupCallCount: Int = 0
        private set
    var lastWarmupLimit: Int = 0
        private set

    override fun readVideoAccessState(): VideoAccessState = accessState

    override suspend fun scanLocalVideos(): List<LocalVideoItem> {
        scanFailure?.let { throw it }
        return videos
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

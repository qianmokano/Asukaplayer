package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MainLibraryCatalogStoreTest {

    @Test
    fun ensureFolderLoaded_ignoresSupersededFolderResult() = runBlocking {
        val firstFolderStarted = CompletableDeferred<Unit>()
        val releaseFirstFolder = CompletableDeferred<Unit>()
        val repository = object : MediaLibraryRepository {
            override val changes: Flow<Unit> = emptyFlow()

            override fun readVideoAccessState(): VideoAccessState {
                return VideoAccessState(
                    permissionGranted = true,
                    userSelectedPermissionGranted = false,
                )
            }

            override suspend fun syncIndex(forceFullRescan: Boolean) = Unit

            override suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder> {
                return MediaLibraryPage(items = emptyList(), nextOffset = null)
            }

            override suspend fun loadVideoPage(
                request: MediaLibraryPageRequest,
                folderId: Long?,
            ): MediaLibraryPage<LocalVideoItem> {
                return when (folderId) {
                    1L -> {
                        firstFolderStarted.complete(Unit)
                        try {
                            releaseFirstFolder.await()
                        } catch (_: CancellationException) {
                            // Simulate a non-cooperative dependency that still returns stale data.
                        }
                        MediaLibraryPage(
                            items = listOf(video(id = 1L, folderId = 1L, title = "Folder A")),
                            nextOffset = null,
                        )
                    }

                    2L -> MediaLibraryPage(
                        items = listOf(video(id = 2L, folderId = 2L, title = "Folder B")),
                        nextOffset = null,
                    )

                    else -> MediaLibraryPage(items = emptyList(), nextOffset = null)
                }
            }

            override suspend fun resolveRecentMediaItems(mediaIds: List<String>): Map<String, LocalVideoItem> {
                return emptyMap()
            }

            override suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int) = Unit

            override suspend fun loadRecentMediaIds(limit: Int): List<String> = emptyList()
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = MainLibraryCatalogStore(
            resolveVideoAccessUseCase = ResolveVideoAccessUseCase(repository),
            loadFolderPageUseCase = LoadFolderPageUseCase(repository, minRefreshAnimMs = 0L),
            loadVideoPageUseCase = LoadVideoPageUseCase(
                mediaLibraryRepository = repository,
                minRefreshAnimMs = 0L,
            ),
            loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(repository),
            resolveRecentMediaItemsUseCase = ResolveRecentMediaItemsUseCase(repository),
            observeMediaLibraryChangesUseCase = ObserveMediaLibraryChangesUseCase(repository),
            scope = scope,
            publishMessage = {},
        )

        store.ensureFolderLoaded(1L)
        firstFolderStarted.await()
        store.ensureFolderLoaded(2L)

        waitForCondition {
            store.currentFolderVideosState.value.items.map(LocalVideoItem::id) == listOf(2L)
        }

        releaseFirstFolder.complete(Unit)

        waitForCondition {
            store.currentFolderId.value == 2L &&
                store.currentFolderVideosState.value.items.map(LocalVideoItem::id) == listOf(2L)
        }

        assertEquals(2L, store.currentFolderId.value)
        assertEquals(listOf(2L), store.currentFolderVideosState.value.items.map(LocalVideoItem::id))
    }

    private fun waitForCondition(predicate: () -> Boolean) {
        repeat(50) {
            if (predicate()) return
            Thread.sleep(10)
        }
        error("Expected condition to become true")
    }

    private fun video(
        id: Long,
        folderId: Long,
        title: String,
    ): LocalVideoItem {
        return LocalVideoItem(
            id = id,
            uri = Uri.parse("content://videos/$id.mp4"),
            title = title,
            durationMs = 1_000L,
            sizeBytes = 2_000L,
            folderName = "Folder $folderId",
            folderPath = "/videos/$folderId",
            folderId = folderId,
            dateAddedSec = 10L,
        )
    }
}

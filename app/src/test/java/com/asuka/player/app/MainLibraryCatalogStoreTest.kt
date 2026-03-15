package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
    fun ensureFoldersLoaded_showsCachedFolders_beforeBackgroundFullSyncCompletes() = runBlocking {
        val syncStarted = CompletableDeferred<Boolean>()
        val releaseSync = CompletableDeferred<Unit>()
        var synced = false
        val repository = object : MediaLibraryRepository {
            override val changes: Flow<Unit> = emptyFlow()

            override fun readVideoAccessState(): VideoAccessState {
                return VideoAccessState(
                    permissionGranted = true,
                    userSelectedPermissionGranted = false,
                )
            }

            override suspend fun syncIndex(forceFullRescan: Boolean) {
                syncStarted.complete(forceFullRescan)
                releaseSync.await()
                synced = true
            }

            override suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder> {
                return if (synced) {
                    MediaLibraryPage(
                        items = listOf(
                            folder(id = 1L, name = "Movies", videoCount = 3),
                            folder(id = 2L, name = "Clips", videoCount = 4),
                        ),
                        nextOffset = null,
                        totalCount = 7,
                    )
                } else {
                    MediaLibraryPage(
                        items = listOf(folder(id = 1L, name = "Movies", videoCount = 3)),
                        nextOffset = null,
                        totalCount = 3,
                    )
                }
            }

            override suspend fun loadVideoPage(
                request: MediaLibraryPageRequest,
                folderId: Long?,
            ): MediaLibraryPage<LocalVideoItem> {
                return MediaLibraryPage(items = emptyList(), nextOffset = null, totalCount = 0)
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
            loadFolderPageUseCase = LoadFolderPageUseCase(
                mediaLibraryRepository = repository,
                minRefreshAnimMs = 0L,
            ),
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

        store.ensureFoldersLoaded()

        waitForCondition {
            store.foldersState.value.hasLoadedOnce &&
                store.foldersState.value.items.map(LocalVideoFolder::id) == listOf(1L)
        }
        assertEquals(true, syncStarted.await())

        releaseSync.complete(Unit)

        waitForCondition {
            store.foldersState.value.items.map(LocalVideoFolder::id) == listOf(1L, 2L) &&
                !store.foldersState.value.isLoading
        }
    }

    @Test
    fun refreshFolders_publishesTotalVideoCountFromIndex() = runBlocking {
        val messages = mutableListOf<MainLibraryText>()
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
                return MediaLibraryPage(
                    items = listOf(
                        folder(id = 1L, name = "Movies", videoCount = 3),
                        folder(id = 2L, name = "Clips", videoCount = 4),
                    ),
                    nextOffset = null,
                    totalCount = 42,
                )
            }

            override suspend fun loadVideoPage(
                request: MediaLibraryPageRequest,
                folderId: Long?,
            ): MediaLibraryPage<LocalVideoItem> {
                return MediaLibraryPage(items = emptyList(), nextOffset = null, totalCount = 0)
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
            loadFolderPageUseCase = LoadFolderPageUseCase(
                mediaLibraryRepository = repository,
                minRefreshAnimMs = 0L,
            ),
            loadVideoPageUseCase = LoadVideoPageUseCase(
                mediaLibraryRepository = repository,
                minRefreshAnimMs = 0L,
            ),
            loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(repository),
            resolveRecentMediaItemsUseCase = ResolveRecentMediaItemsUseCase(repository),
            observeMediaLibraryChangesUseCase = ObserveMediaLibraryChangesUseCase(repository),
            scope = scope,
            publishMessage = messages::add,
        )

        store.ensureFoldersLoaded()
        waitForCondition { store.foldersState.value.hasLoadedOnce }
        assertTrue(messages.isEmpty())

        store.refreshFolders()
        waitForCondition { messages.isNotEmpty() }

        assertEquals(listOf<MainLibraryText>(MainLibraryText.RefreshDone(42)), messages)
    }

    @Test
    fun ensureFolderLoaded_reportsLoadingUntilPreloadCompletes() = runBlocking {
        val preloadStarted = CompletableDeferred<Unit>()
        val releasePreload = CompletableDeferred<Unit>()
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
                assertEquals(null, folderId)
                preloadStarted.complete(Unit)
                releasePreload.await()
                return MediaLibraryPage(
                    items = listOf(video(id = 1L, folderId = 1L, title = "Folder A")),
                    nextOffset = null,
                )
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
        preloadStarted.await()

        waitForCondition {
            store.currentFolderId.value == 1L &&
                store.currentFolderVideosState.value.isLoading &&
                !store.currentFolderVideosState.value.hasLoadedOnce &&
                store.currentFolderVideosState.value.items.isEmpty()
        }

        releasePreload.complete(Unit)

        waitForCondition {
            store.currentFolderVideosState.value.hasLoadedOnce &&
                !store.currentFolderVideosState.value.isLoading &&
                store.currentFolderVideosState.value.items.map(LocalVideoItem::id) == listOf(1L)
        }

        assertEquals(1L, store.currentFolderId.value)
        assertEquals(listOf(1L), store.currentFolderVideosState.value.items.map(LocalVideoItem::id))
    }

    @Test
    fun refreshFolder_failureKeepsPreviousSnapshotAndExposesError() = runBlocking {
        var failRefresh = false
        val messages = mutableListOf<MainLibraryText>()
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
                assertEquals(null, folderId)
                return when {
                    !failRefresh && request.offset == 0 -> MediaLibraryPage(
                        items = listOf(video(id = 1L, folderId = 1L, title = "Folder A1")),
                        nextOffset = 1,
                    )

                    !failRefresh && request.offset == 1 -> MediaLibraryPage(
                        items = listOf(video(id = 2L, folderId = 1L, title = "Folder A2")),
                        nextOffset = null,
                    )

                    failRefresh && request.offset == 0 -> MediaLibraryPage(
                        items = listOf(video(id = 1L, folderId = 1L, title = "Folder A1 refreshed")),
                        nextOffset = 1,
                    )

                    else -> throw IllegalStateException("provider unavailable")
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
            publishMessage = messages::add,
        )

        store.ensureFolderLoaded(1L)
        waitForCondition {
            store.currentFolderVideosState.value.items.map(LocalVideoItem::id) == listOf(1L, 2L)
        }

        failRefresh = true
        store.refreshFolder(1L)

        waitForCondition {
            store.currentFolderVideosState.value.isLoading &&
                store.currentFolderVideosState.value.hasLoadedOnce &&
                store.currentFolderVideosState.value.items.map(LocalVideoItem::id) == listOf(1L, 2L)
        }

        waitForCondition {
            store.currentFolderVideosState.value.errorMessage == MainLibraryText.MediaLibraryProviderUnavailable
        }

        assertFalse(store.currentFolderVideosState.value.isLoading)
        assertEquals(listOf(1L, 2L), store.currentFolderVideosState.value.items.map(LocalVideoItem::id))
        assertEquals(
            MainLibraryText.MediaLibraryProviderUnavailable,
            store.currentFolderVideosState.value.errorMessage,
        )
        assertTrue(messages.isEmpty())
    }

    private fun waitForCondition(predicate: () -> Boolean) {
        repeat(200) {
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

    private fun folder(
        id: Long,
        name: String,
        videoCount: Int,
    ): LocalVideoFolder {
        return LocalVideoFolder(
            id = id,
            name = name,
            videoCount = videoCount,
            totalDurationMs = 1_000L,
            totalSizeBytes = 2_000L,
        )
    }
}

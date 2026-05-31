package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class MainLibraryViewModelLogicTest {

    @Test
    fun resolveRecentMediaIds_prefersPlaybackHistoryOrder_andAppliesLimit() {
        val older = Uri.parse("content://videos/older.mp4")
        val current = Uri.parse("content://videos/current.mp4")
        val fallback = listOf("content://videos/fallback.mp4")

        val result = resolveRecentMediaIds(
            historyMediaIds = listOf(older.toString(), current.toString(), current.toString()),
            fallbackMediaIds = fallback,
            limit = 1,
        )

        assertEquals(
            listOf(current.toString()),
            result,
        )
    }

    @Test
    fun resolveRecentMediaIds_fallsBackWhenHistoryEmpty_andAppliesLimit() {
        val fallback = listOf(
            "content://videos/current.mp4",
            "content://videos/older.mp4",
        )

        val result = resolveRecentMediaIds(
            historyMediaIds = emptyList(),
            fallbackMediaIds = fallback,
            limit = 1,
        )

        assertEquals(listOf("content://videos/current.mp4"), result)
    }

    @Test
    fun resolveRecentMediaIds_returnsEmptyWhenLimitIsZero() {
        val result = resolveRecentMediaIds(
            historyMediaIds = listOf(Uri.parse("content://videos/current.mp4").toString()),
            fallbackMediaIds = listOf("content://videos/fallback.mp4"),
            limit = 0,
        )

        assertEquals(emptyList(), result)
    }

    @Test
    fun refreshTargetForRoute_routesRecentPageToRecentRefresh() {
        assertSame(MainLibraryRefreshTarget.Recent, refreshTargetForRoute(ROUTE_RECENT))
    }

    @Test
    fun refreshTargetForRoute_routesFolderPageToFolderRefresh() {
        assertSame(MainLibraryRefreshTarget.Folder, refreshTargetForRoute(folderRoute(42L)))
    }

    @Test
    fun applyPage_replacesItemsOnRefresh_andTracksNextOffset() {
        val currentState = MediaCatalogState(
            items = listOf(localVideoItem(id = 1L, name = "one.mp4")),
            status = MediaCatalogStatus.Loading,
            hasLoadedOnce = true,
        )
        val state = currentState.applyPage(
            page = MediaLibraryPage(
                items = listOf(localVideoItem(id = 2L, name = "two.mp4")),
                nextOffset = 1,
            ),
            append = false,
        )

        assertFalse(state.isLoading)
        assertEquals(true, state.hasLoadedOnce)
        assertEquals(listOf(2L), state.items.map(LocalVideoItem::id))
        assertEquals(1, state.nextOffset)
        assertEquals(true, state.hasMore)
    }

    @Test
    fun applyFailure_onAppend_preservesItemsAndSurfacesFooterError() {
        val currentState = MediaCatalogState(
            items = listOf(localVideoItem(id = 1L, name = "one.mp4")),
            status = MediaCatalogStatus.Appending,
            hasLoadedOnce = true,
            errorMessage = null,
        )

        val state = currentState.applyFailure(
            offset = 40,
            message = MainLibraryText.MediaLibraryProviderUnavailable,
        )

        assertEquals(MediaCatalogStatus.Idle, state.status)
        assertEquals(listOf(1L), state.items.map(LocalVideoItem::id))
        assertEquals(null, state.errorMessage)
        assertEquals(MainLibraryText.MediaLibraryProviderUnavailable, state.appendErrorMessage)
    }

    @Test
    fun applyFailure_onRefresh_clearsFooterErrorAndSurfacesPrimaryError() {
        val currentState = MediaCatalogState(
            items = listOf(localVideoItem(id = 1L, name = "one.mp4")),
            status = MediaCatalogStatus.Loading,
            hasLoadedOnce = true,
            appendErrorMessage = MainLibraryText.MediaLibraryUnknown,
        )

        val state = currentState.applyFailure(
            offset = 0,
            message = MainLibraryText.MediaLibraryPermissionDenied,
        )

        assertEquals(MediaCatalogStatus.Idle, state.status)
        assertEquals(MainLibraryText.MediaLibraryPermissionDenied, state.errorMessage)
        assertEquals(null, state.appendErrorMessage)
    }

    @Test
    fun localVideoItem_resumeProgressFraction_isDerivedFromResumePosition() {
        val item = localVideoItem(
            id = 1L,
            name = "resume.mp4",
            durationMs = 100_000L,
            resumePositionMs = 25_000L,
        )

        assertEquals(0.25f, item.resumeProgressFraction)
    }

    @Test
    fun localVideoItem_resumeProgressFraction_isNullWithoutValidProgress() {
        val noResume = localVideoItem(
            id = 1L,
            name = "resume.mp4",
            durationMs = 100_000L,
            resumePositionMs = 0L,
        )
        val noDuration = localVideoItem(
            id = 2L,
            name = "resume.mp4",
            durationMs = 0L,
            resumePositionMs = 25_000L,
        )

        assertNull(noResume.resumeProgressFraction)
        assertNull(noDuration.resumeProgressFraction)
    }

    @Test
    fun validateNetworkStreamUrl_acceptsSupportedNetworkStreams() {
        val messages = mutableListOf<MainLibraryText>()
        val store = catalogStore(messages::add)

        assertEquals("https://example.com/movie.m3u8", store.validateNetworkStreamUrl(" https://example.com/movie.m3u8 "))
        assertEquals("rtsp://example.com/live", store.validateNetworkStreamUrl("rtsp://example.com/live"))
        assertEquals(emptyList(), messages)
    }

    @Test
    fun validateNetworkStreamUrl_rejectsUnsupportedOrIncompleteUrls() {
        val messages = mutableListOf<MainLibraryText>()
        val store = catalogStore(messages::add)

        assertNull(store.validateNetworkStreamUrl("foo:bar"))
        assertNull(store.validateNetworkStreamUrl("http://"))
        assertNull(store.validateNetworkStreamUrl("https://example.com"))

        val expectedMessages: List<MainLibraryText> = listOf(
            MainLibraryText.OpenNetworkStreamInvalid,
            MainLibraryText.OpenNetworkStreamInvalid,
            MainLibraryText.OpenNetworkStreamInvalid,
        )
        assertEquals(
            expectedMessages,
            messages,
        )
    }

    private fun localVideoItem(
        id: Long,
        name: String,
        durationMs: Long = 1_000L,
        resumePositionMs: Long = 0L,
    ): LocalVideoItem {
        return LocalVideoItem(
            id = id,
            uri = Uri.parse("content://videos/$name"),
            title = name,
            durationMs = durationMs,
            sizeBytes = 2_000L,
            folderName = "Folder",
            folderPath = "/storage/emulated/0/Folder",
            folderId = 42L,
            dateAddedSec = 1L,
            resumePositionMs = resumePositionMs,
        )
    }

    private fun catalogStore(
        publishMessage: (MainLibraryText) -> Unit,
    ): MainLibraryCatalogStore {
        val repository = EmptyMediaLibraryRepository()
        return MainLibraryCatalogStore(
            resolveVideoAccessUseCase = ResolveVideoAccessUseCase(repository),
            loadFolderPageUseCase = LoadFolderPageUseCase(repository, minRefreshAnimMs = 0L),
            loadVideoPageUseCase = LoadVideoPageUseCase(repository, minRefreshAnimMs = 0L),
            loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(repository),
            resolveRecentMediaItemsUseCase = ResolveRecentMediaItemsUseCase(repository),
            observeMediaLibraryChangesUseCase = ObserveMediaLibraryChangesUseCase(repository),
            scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined),
            publishMessage = publishMessage,
        )
    }
}

private class EmptyMediaLibraryRepository : MediaLibraryRepository {
    override val changes: kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow()

    override fun readVideoAccessState(): VideoAccessState =
        VideoAccessState(permissionGranted = false, userSelectedPermissionGranted = false)

    override suspend fun syncIndex(forceFullRescan: Boolean) = Unit

    override suspend fun loadFolderPage(request: MediaLibraryPageRequest): MediaLibraryPage<LocalVideoFolder> =
        MediaLibraryPage(emptyList(), nextOffset = null, totalCount = 0)

    override suspend fun loadVideoPage(
        request: MediaLibraryPageRequest,
        folderId: Long?,
    ): MediaLibraryPage<LocalVideoItem> =
        MediaLibraryPage(emptyList(), nextOffset = null, totalCount = 0)

    override suspend fun resolveRecentMediaItems(mediaIds: List<String>): Map<String, LocalVideoItem> = emptyMap()

    override suspend fun warmupInitialThumbnails(videos: List<LocalVideoItem>, limit: Int) = Unit

    override suspend fun loadRecentMediaIds(limit: Int): List<String> = emptyList()
}

package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}

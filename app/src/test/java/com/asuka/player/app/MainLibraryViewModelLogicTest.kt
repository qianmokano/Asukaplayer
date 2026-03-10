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
    fun resolveRecentMediaIds_prefersPlaybackHistoryOrder() {
        val older = Uri.parse("content://videos/older.mp4")
        val current = Uri.parse("content://videos/current.mp4")
        val fallback = listOf("content://videos/fallback.mp4")

        val result = resolveRecentMediaIds(
            historyUris = listOf(older, current, current),
            fallbackMediaIds = fallback,
        )

        assertEquals(
            listOf(current.toString(), older.toString()),
            result,
        )
    }

    @Test
    fun resolveRecentMediaIds_fallsBackWhenHistoryEmpty() {
        val fallback = listOf(
            "content://videos/current.mp4",
            "content://videos/older.mp4",
        )

        val result = resolveRecentMediaIds(
            historyUris = emptyList(),
            fallbackMediaIds = fallback,
        )

        assertEquals(fallback, result)
    }

    @Test
    fun reduceMediaLibraryRefreshState_retainsExistingItemsWhenRefreshFails() {
        val currentState = MediaLibraryRefreshState(
            items = listOf(localVideoItem(id = 1L, name = "one.mp4")),
            status = MediaLibraryRefreshStatus.Loading,
            hasLoadedOnce = true,
        )
        val state = reduceMediaLibraryRefreshState(
            currentState = currentState,
            result = MediaLibraryRefreshOutcome.Failure(MediaLibraryRefreshFailure.ProviderUnavailable),
            errorMessage = "Provider unavailable",
        )

        assertFalse(state.isLoading)
        assertEquals(true, state.hasLoadedOnce)
        assertEquals(listOf(1L), state.items.map(LocalVideoItem::id))
        assertEquals("Provider unavailable", state.errorMessage)
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

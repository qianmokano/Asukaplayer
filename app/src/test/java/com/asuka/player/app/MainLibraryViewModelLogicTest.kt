package com.asuka.player.app

import android.net.Uri
import kotlin.test.Test
import kotlin.test.assertEquals
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
}

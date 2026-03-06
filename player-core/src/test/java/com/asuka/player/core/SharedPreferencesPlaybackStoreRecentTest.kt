package com.asuka.player.core

import com.asuka.player.data.SharedPreferencesPlaybackStore
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesPlaybackStoreRecentTest {

    @Test
    fun recentMediaIds_returnsNewestFirst() {
        val context = RuntimeEnvironment.getApplication()
        val store = SharedPreferencesPlaybackStore(context)

        store.savePosition("a", 1L)
        store.savePosition("b", 2L)
        store.savePosition("c", 3L)

        assertEquals(listOf("c", "b", "a"), store.recentMediaIds(limit = 10))
        assertEquals(listOf("c", "b"), store.recentMediaIds(limit = 2))
    }

    @Test
    fun recentMediaIds_updatesOnOtherKeys() {
        val context = RuntimeEnvironment.getApplication()
        val store = SharedPreferencesPlaybackStore(context)

        store.savePosition("a", 1L)
        store.savePlaybackSpeed("b", 1.5f)

        assertEquals(listOf("b", "a"), store.recentMediaIds(limit = 10))
    }
}

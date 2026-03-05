package com.asuka.player.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesPlaybackStoreThreadingTest {

    @Test
    fun saveLoad_onMainThread_works() {
        val context = RuntimeEnvironment.getApplication()
        val store = SharedPreferencesPlaybackStore(context)
        store.savePosition("media://test", 123L)
        assertEquals(123L, store.loadPosition("media://test"))
    }

    @Test
    fun load_offMainThread_throws() {
        val context = RuntimeEnvironment.getApplication()
        val store = SharedPreferencesPlaybackStore(context)

        var failure: Throwable? = null
        val t = Thread {
            failure = runCatching { store.loadPosition("media://bg") }.exceptionOrNull()
        }
        t.start()
        t.join()

        val error = failure
        assertNotNull(error)
        assertIs<IllegalStateException>(error)
    }
}


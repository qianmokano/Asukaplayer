package com.asuka.player.core

import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.ResumeState
import com.asuka.player.data.SharedPreferencesPlaybackStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
    fun saveAndLoad_offMainThread_work() {
        val context = RuntimeEnvironment.getApplication()
        val store = SharedPreferencesPlaybackStore(context)

        var loaded: Long? = null
        val t = Thread {
            store.savePosition("media://bg", 456L)
            loaded = store.loadPosition("media://bg")
        }
        t.start()
        t.join()

        assertEquals(456L, loaded)
        assertEquals(456L, store.loadPosition("media://bg"))
    }

    @Test
    fun backgroundWrite_isVisibleToRepositoryReadOnAnotherThread() {
        val context = RuntimeEnvironment.getApplication()
        val store = SharedPreferencesPlaybackStore(context)
        val repository = PlaybackStateRepository(store)

        val writer = Thread {
            store.savePosition("media://resume", 789L)
            store.savePlaybackSpeed("media://resume", 1.25f)
        }
        writer.start()
        writer.join()

        var resumeState: ResumeState? = null
        val reader = Thread {
            resumeState = repository.readResumeState("media://resume")
        }
        reader.start()
        reader.join()

        assertEquals(789L, resumeState?.positionMs)
        assertEquals(1.25f, resumeState?.speed)
        assertNull(resumeState?.audioTrackSelection)
    }
}

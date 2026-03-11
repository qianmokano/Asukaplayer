package com.asuka.player.core

import android.net.Uri
import com.asuka.player.data.SharedPreferencesQueueHistoryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesQueueHistoryStoreTest {

    @Test
    fun items_surviveAcrossInstances() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val prefsName = "queue-history-persist"
        context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE).edit().clear().commit()

        SharedPreferencesQueueHistoryStore(
            context = context,
            preferencesName = prefsName,
            maxSize = 3,
        ).apply {
            push("file:///1.mp4")
            push("file:///2.mp4")
        }

        val restored = SharedPreferencesQueueHistoryStore(
            context = context,
            preferencesName = prefsName,
            maxSize = 3,
        )

        assertEquals(
            listOf("file:///1.mp4", "file:///2.mp4"),
            restored.items(),
        )
    }

    @Test
    fun push_respectsMaxSizeAcrossPersistedHistory() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val prefsName = "queue-history-max-size"
        context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE).edit().clear().commit()

        val store = SharedPreferencesQueueHistoryStore(
            context = context,
            preferencesName = prefsName,
            maxSize = 2,
        )
        store.push("file:///1.mp4")
        store.push("file:///2.mp4")
        store.push("file:///3.mp4")

        assertEquals(
            listOf("file:///2.mp4", "file:///3.mp4"),
            store.items(),
        )
    }
}

package com.asuka.player.core

import android.net.Uri
import com.asuka.player.data.InMemoryQueueHistoryStore
import com.asuka.player.data.SharedPreferencesQueueHistoryStore
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class QueueHistoryStoreThreadingTest {

    @Test
    fun inMemoryStore_writeOnOneThreadReadOnAnotherThread() {
        val store = InMemoryQueueHistoryStore(maxSize = 3)
        val expected = listOf(
            Uri.parse("file:///1.mp4"),
            Uri.parse("file:///2.mp4"),
        )

        val writer = Thread {
            expected.forEach(store::push)
        }
        writer.start()
        writer.join()

        var loaded: List<Uri> = emptyList()
        val reader = Thread {
            loaded = store.items()
        }
        reader.start()
        reader.join()

        assertEquals(expected, loaded)
    }

    @Test
    fun sharedPreferencesStore_writeOnOneThreadReadOnAnotherThread() {
        val context = RuntimeEnvironment.getApplication()
        val prefsName = "queue-history-threading"
        context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val store = SharedPreferencesQueueHistoryStore(
            context = context,
            preferencesName = prefsName,
            maxSize = 3,
        )
        val expected = listOf(
            Uri.parse("file:///1.mp4"),
            Uri.parse("file:///2.mp4"),
        )

        val writer = Thread {
            expected.forEach(store::push)
        }
        writer.start()
        writer.join()

        var loaded: List<Uri> = emptyList()
        val reader = Thread {
            loaded = store.items()
        }
        reader.start()
        reader.join()

        assertEquals(expected, loaded)
    }
}

package com.asuka.player.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SerialTaskQueueTest {

    private fun queue() = SerialTaskQueue(Dispatchers.Unconfined, tag = "test")

    // -- serial ordering --

    @Test
    fun dispatch_executesTasksInOrder() = runTest {
        val log = mutableListOf<Int>()
        val q = queue()

        q.dispatch { log += 1 }
        q.dispatch { log += 2 }
        q.dispatch { log += 3 }
        q.awaitIdle()

        assertEquals(listOf(1, 2, 3), log)
        q.close()
    }

    // -- dispatchAndAwait --

    @Test
    fun dispatchAndAwait_completesBeforeReturning() = runTest {
        val log = mutableListOf<String>()
        val q = queue()

        q.dispatchAndAwait { log += "task" }
        log += "after"

        assertEquals(listOf("task", "after"), log)
        q.close()
    }

    @Test
    fun dispatchAndAwait_propagatesException() = runTest {
        val q = queue()

        assertFailsWith<IllegalStateException> {
            q.dispatchAndAwait { error("boom") }
        }
        q.close()
    }

    // -- awaitIdle (barrier) --

    @Test
    fun awaitIdle_completesAfterAllPriorTasks() = runTest {
        val log = mutableListOf<Int>()
        val q = queue()

        q.dispatch { log += 1 }
        q.dispatch { log += 2 }
        q.awaitIdle()

        assertEquals(listOf(1, 2), log)
        q.close()
    }

    // -- error handling --

    @Test
    fun dispatch_failingTaskDoesNotBlockSubsequentTasks() = runTest {
        val log = mutableListOf<Int>()
        val q = queue()

        q.dispatch { error("fail") }
        q.dispatch { log += 2 }
        q.awaitIdle()

        assertEquals(listOf(2), log)
        q.close()
    }

    // -- close --

    @Test
    fun close_dropsNewTasks() = runTest {
        val log = mutableListOf<Int>()
        val q = queue()

        q.dispatch { log += 1 }
        q.awaitIdle()
        q.close()

        q.dispatch { log += 2 }
        assertEquals(listOf(1), log)
    }

    @Test
    fun dispatchAndAwait_afterClose_returnsImmediately() = runTest {
        val q = queue()
        q.close()

        // Should not hang or throw
        q.dispatchAndAwait { error("should not run") }
    }

    @Test
    fun awaitIdle_afterClose_returnsImmediately() = runTest {
        val q = queue()
        q.close()

        q.awaitIdle()
    }

    // -- interleaving dispatch and dispatchAndAwait --

    @Test
    fun mixedDispatchAndDispatchAndAwait_preservesOrdering() = runTest {
        val log = mutableListOf<Int>()
        val q = queue()

        q.dispatch { log += 1 }
        q.dispatchAndAwait { log += 2 }
        q.dispatch { log += 3 }
        q.awaitIdle()

        assertEquals(listOf(1, 2, 3), log)
        q.close()
    }

    // -- factory --

    @Test
    fun ioFactory_createsWorkingQueue() = runTest {
        val q = SerialTaskQueue.io("test-io")
        val result = mutableListOf<Int>()
        q.dispatchAndAwait { result += 42 }
        assertEquals(listOf(42), result)
        q.close()
    }
}

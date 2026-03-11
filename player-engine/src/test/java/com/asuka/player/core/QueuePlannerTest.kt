package com.asuka.player.core

import android.net.Uri
import com.asuka.player.contract.QueuePlanner
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QueuePlannerTest {

    @Test
    fun plan_preservesExplicitNeighborOrder() {
        val current = Uri.parse("file:///b.mp4")
        val neighbors = listOf(
            Uri.parse("file:///c.mp4"),
            Uri.parse("file:///a.mp4"),
        )
        val result = QueuePlanner.plan(current.toString(), neighbors.map(Uri::toString))
        assertEquals(listOf(current.toString(), "file:///c.mp4", "file:///a.mp4"), result)
    }

    @Test
    fun plan_preservesExplicitQueuePosition_whenCurrentAlreadyIncluded() {
        val a = Uri.parse("file:///a.mp4")
        val current = Uri.parse("file:///b.mp4")
        val c = Uri.parse("file:///c.mp4")

        val result = QueuePlanner.plan(
            current = current.toString(),
            neighbors = listOf(a.toString(), current.toString(), c.toString()),
        )

        assertEquals(listOf(a.toString(), current.toString(), c.toString()), result)
    }

    @Test
    fun plan_returnsOnlyCurrentWhenNoExplicitQueueProvided() {
        val current = Uri.parse("file:///b.mp4")
        val result = QueuePlanner.plan(current.toString(), neighbors = emptyList())

        assertEquals(listOf(current.toString()), result)
    }

    @Test
    fun plan_withExplicitQueueDoesNotIncludeExtraItems() {
        val current = Uri.parse("file:///current.mp4")
        val neighbors = listOf(Uri.parse("file:///next.mp4"))

        val result = QueuePlanner.plan(current.toString(), neighbors.map(Uri::toString))

        assertEquals(listOf(current.toString(), "file:///next.mp4"), result)
    }
}

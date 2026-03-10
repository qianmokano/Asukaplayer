package com.asuka.player.core

import android.net.Uri
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
        val result = QueuePlanner.plan(current, neighbors)
        assertEquals(listOf(current, Uri.parse("file:///c.mp4"), Uri.parse("file:///a.mp4")), result)
    }

    @Test
    fun plan_preservesExplicitQueuePosition_whenCurrentAlreadyIncluded() {
        val a = Uri.parse("file:///a.mp4")
        val current = Uri.parse("file:///b.mp4")
        val c = Uri.parse("file:///c.mp4")

        val result = QueuePlanner.plan(
            current = current,
            neighbors = listOf(a, current, c),
        )

        assertEquals(listOf(a, current, c), result)
    }

    @Test
    fun plan_returnsOnlyCurrentWhenNoExplicitQueueProvided() {
        val current = Uri.parse("file:///b.mp4")
        val result = QueuePlanner.plan(current, neighbors = emptyList())

        assertEquals(listOf(current), result)
    }

    @Test
    fun plan_withExplicitQueueDoesNotIncludeExtraItems() {
        val current = Uri.parse("file:///current.mp4")
        val neighbors = listOf(Uri.parse("file:///next.mp4"))

        val result = QueuePlanner.plan(current, neighbors)

        assertEquals(listOf(current, Uri.parse("file:///next.mp4")), result)
    }
}

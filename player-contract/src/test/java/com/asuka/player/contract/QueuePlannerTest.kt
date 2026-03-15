package com.asuka.player.contract

import kotlin.test.Test
import kotlin.test.assertEquals

class QueuePlannerTest {

    @Test
    fun plan_preservesExplicitNeighborOrder() {
        val current = "file:///b.mp4"
        val neighbors = listOf("file:///c.mp4", "file:///a.mp4")
        val result = QueuePlanner.plan(current, neighbors)
        assertEquals(listOf(current, "file:///c.mp4", "file:///a.mp4"), result)
    }

    @Test
    fun plan_preservesExplicitQueuePosition_whenCurrentAlreadyIncluded() {
        val a = "file:///a.mp4"
        val current = "file:///b.mp4"
        val c = "file:///c.mp4"

        val result = QueuePlanner.plan(
            current = current,
            neighbors = listOf(a, current, c),
        )

        assertEquals(listOf(a, current, c), result)
    }

    @Test
    fun plan_returnsOnlyCurrentWhenNoExplicitQueueProvided() {
        val current = "file:///b.mp4"
        val result = QueuePlanner.plan(current, neighbors = emptyList())

        assertEquals(listOf(current), result)
    }

    @Test
    fun plan_withExplicitQueueDoesNotIncludeExtraItems() {
        val current = "file:///current.mp4"
        val neighbors = listOf("file:///next.mp4")

        val result = QueuePlanner.plan(current, neighbors)

        assertEquals(listOf(current, "file:///next.mp4"), result)
    }
}

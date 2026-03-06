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
        val result = QueuePlanner.plan(current, neighbors, history = emptyList())
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
            history = emptyList(),
        )

        assertEquals(listOf(a, current, c), result)
    }

    @Test
    fun plan_mergesHistoryDistinct() {
        val current = Uri.parse("file:///b.mp4")
        val history = listOf(Uri.parse("file:///x.mp4"), current)
        val result = QueuePlanner.plan(current, neighbors = emptyList(), history = history)
        // current is always first; history items not already in the base trail at the end;
        // current appears in history so it is deduplicated and stays at index 0.
        assertEquals(listOf(current, Uri.parse("file:///x.mp4")), result)
    }

    @Test
    fun plan_ignoresHistoryWhenExplicitQueueProvided() {
        val current = Uri.parse("file:///current.mp4")
        val neighbors = listOf(Uri.parse("file:///next.mp4"))
        val history = listOf(Uri.parse("file:///history.mp4"))

        val result = QueuePlanner.plan(current, neighbors, history)

        assertEquals(listOf(current, Uri.parse("file:///next.mp4")), result)
    }
}

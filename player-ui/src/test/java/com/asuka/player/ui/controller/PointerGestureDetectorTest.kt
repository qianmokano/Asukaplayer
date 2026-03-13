package com.asuka.player.ui.controller

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerGestureDetectorTest {

    @Test
    fun dragStartGuard_blocksLeftEdgeStarts() {
        val guard = PointerGestureDetector.DragStartGuard(
            leftEdgePx = 32f,
            rightEdgePx = 32f,
            topEdgePx = 32f,
            bottomEdgePx = 32f,
        )

        assertTrue(guard.blocksDragStart(startPosition = androidx.compose.ui.geometry.Offset(16f, 200f), width = 400, height = 240))
    }

    @Test
    fun dragStartGuard_blocksRightEdgeStarts() {
        val guard = PointerGestureDetector.DragStartGuard(
            leftEdgePx = 32f,
            rightEdgePx = 32f,
            topEdgePx = 32f,
            bottomEdgePx = 32f,
        )

        assertTrue(guard.blocksDragStart(startPosition = androidx.compose.ui.geometry.Offset(388f, 200f), width = 400, height = 240))
    }

    @Test
    fun dragStartGuard_blocksTopEdgeStarts() {
        val guard = PointerGestureDetector.DragStartGuard(
            leftEdgePx = 32f,
            rightEdgePx = 32f,
            topEdgePx = 32f,
            bottomEdgePx = 32f,
        )

        assertTrue(guard.blocksDragStart(startPosition = androidx.compose.ui.geometry.Offset(200f, 12f), width = 400, height = 240))
    }

    @Test
    fun dragStartGuard_blocksBottomEdgeStarts() {
        val guard = PointerGestureDetector.DragStartGuard(
            leftEdgePx = 32f,
            rightEdgePx = 32f,
            topEdgePx = 32f,
            bottomEdgePx = 32f,
        )

        assertTrue(guard.blocksDragStart(startPosition = androidx.compose.ui.geometry.Offset(200f, 228f), width = 400, height = 240))
    }

    @Test
    fun dragStartGuard_allowsInteriorStarts() {
        val guard = PointerGestureDetector.DragStartGuard(
            leftEdgePx = 32f,
            rightEdgePx = 32f,
            topEdgePx = 32f,
            bottomEdgePx = 32f,
        )

        assertFalse(guard.blocksDragStart(startPosition = androidx.compose.ui.geometry.Offset(200f, 120f), width = 400, height = 240))
    }
}

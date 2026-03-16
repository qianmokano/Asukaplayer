package com.asuka.player.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GestureAlgorithmsTest {

    @Test
    fun calculateSeek_clampsToDuration() {
        val input = GestureAlgorithms.SeekInput(
            startPositionMs = 90_000,
            startX = 0f,
            currentX = 1000f,
            sensitivity = 1f,
            durationMs = 100_000,
        )
        val result = GestureAlgorithms.calculateSeek(input)
        assertEquals(100_000, result.newPositionMs)
    }

    @Test
    fun calculateSeek_clampsToZero() {
        val input = GestureAlgorithms.SeekInput(
            startPositionMs = 5_000,
            startX = 1000f,
            currentX = 0f,
            sensitivity = 1f,
            durationMs = 100_000,
        )
        val result = GestureAlgorithms.calculateSeek(input)
        assertEquals(0, result.newPositionMs)
    }

    @Test
    fun calculateVerticalAdjust_increasesOnSwipeUp() {
        val input = GestureAlgorithms.VerticalAdjustInput(
            startValue = 50,
            startY = 200f,
            currentY = 100f,
            sensitivity = 1f,
            maxValue = 100,
        )
        val result = GestureAlgorithms.calculateVerticalAdjust(input)
        assertEquals(true, result.newValue > 50)
    }

    @Test
    fun clampZoom_respectsBounds() {
        val input = GestureAlgorithms.ZoomInput(
            currentZoom = 10f,
            zoomChange = 2f,
            minZoom = 0.25f,
            maxZoom = 4f,
        )
        val zoom = GestureAlgorithms.clampZoom(input)
        assertEquals(4f, zoom)
    }

    @Test
    fun calculateProgressBarSeek_tracksTouchDeltaFromDragStart() {
        val result = GestureAlgorithms.calculateProgressBarSeek(
            GestureAlgorithms.ProgressBarSeekInput(
                startPositionMs = 30_000L,
                startTouchPositionMs = 30_000L,
                currentTouchPositionMs = 70_000L,
                durationMs = 120_000L,
            ),
        )

        assertEquals(70_000L, result.newPositionMs)
        assertEquals(40_000L, result.deltaMs)
    }
}

package com.asuka.player.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestureAlgorithmsClampTest {

    @Test
    fun calculateSeek_zeroDurationReturnsZero() {
        val input = GestureAlgorithms.SeekInput(
            startPositionMs = 0,
            startX = 0f,
            currentX = 100f,
            sensitivity = 1f,
            durationMs = 0,
        )
        val result = GestureAlgorithms.calculateSeek(input)
        assertEquals(0, result.newPositionMs)
    }

    @Test
    fun clampPan_zoomOneNoPan() {
        // At zoom=1 the overhang is zero, so any pan is clamped to 0.
        val result = GestureAlgorithms.clampPan(
            GestureAlgorithms.PanClampInput(
                zoom = 1f, viewWidth = 100f, viewHeight = 100f,
                currentX = 10f, currentY = -10f,
                panX = 50f, panY = 50f,
            ),
        )
        assertEquals(0f, result.clampedX)
        assertEquals(0f, result.clampedY)
    }

    @Test
    fun clampPan_oneToOneTracking() {
        // At zoom=2, viewWidth=1000: overhang = 1000, bound = 500
        // panX = 100 from current=0 should yield exactly 100 (1:1 tracking)
        val result = GestureAlgorithms.clampPan(
            GestureAlgorithms.PanClampInput(
                zoom = 2f, viewWidth = 1000f, viewHeight = 1000f,
                currentX = 0f, currentY = 0f,
                panX = 100f, panY = -50f,
            ),
        )
        assertEquals(100f, result.clampedX)
        assertEquals(-50f, result.clampedY)
    }

    @Test
    fun clampPan_clampsAtBound() {
        // At zoom=2, viewWidth=1000: bound = 500
        // currentX=400 + panX=200 = 600 → clamped to 500
        val result = GestureAlgorithms.clampPan(
            GestureAlgorithms.PanClampInput(
                zoom = 2f, viewWidth = 1000f, viewHeight = 1000f,
                currentX = 400f, currentY = 0f,
                panX = 200f, panY = 0f,
            ),
        )
        assertEquals(500f, result.clampedX)
    }

    @Test
    fun clampPan_zoomBelowOneClampsBothToZero() {
        // At zoom=0.5, overhang is negative → coerced to 0
        val result = GestureAlgorithms.clampPan(
            GestureAlgorithms.PanClampInput(
                zoom = 0.5f, viewWidth = 1000f, viewHeight = 1000f,
                currentX = 100f, currentY = 100f,
                panX = 50f, panY = 50f,
            ),
        )
        assertEquals(0f, result.clampedX)
        assertEquals(0f, result.clampedY)
    }

    @Test
    fun clampPan_negativePanClamped() {
        // At zoom=2, viewWidth=1000: bound = 500
        // currentX=-400 + panX=-200 = -600 → clamped to -500
        val result = GestureAlgorithms.clampPan(
            GestureAlgorithms.PanClampInput(
                zoom = 2f, viewWidth = 1000f, viewHeight = 1000f,
                currentX = -400f, currentY = 0f,
                panX = -200f, panY = 0f,
            ),
        )
        assertEquals(-500f, result.clampedX)
    }

    @Test
    fun calculateSeek_sensitivityScalesRate() {
        val base = GestureAlgorithms.calculateSeek(
            GestureAlgorithms.SeekInput(0, 0f, 100f, 1f, 1_000_000),
        )
        val double = GestureAlgorithms.calculateSeek(
            GestureAlgorithms.SeekInput(0, 0f, 100f, 2f, 1_000_000),
        )
        // Double sensitivity should seek roughly twice as far
        assertTrue(double.newPositionMs > base.newPositionMs, "Higher sensitivity should seek further")
        assertEquals(base.newPositionMs * 2, double.newPositionMs)
    }

    @Test
    fun calculateProgressBarSeek_ratioScalesDelta() {
        val result = GestureAlgorithms.calculateProgressBarSeek(
            GestureAlgorithms.ProgressBarSeekInput(
                startPositionMs = 30_000L,
                startTouchPositionMs = 30_000L,
                currentTouchPositionMs = 60_000L,
                durationMs = 120_000L,
                distanceRatio = 0.5f,
            ),
        )

        assertEquals(45_000L, result.newPositionMs)
        assertEquals(15_000L, result.deltaMs)
    }

    @Test
    fun calculateProgressBarSeek_clampsToDuration() {
        val result = GestureAlgorithms.calculateProgressBarSeek(
            GestureAlgorithms.ProgressBarSeekInput(
                startPositionMs = 110_000L,
                startTouchPositionMs = 60_000L,
                currentTouchPositionMs = 120_000L,
                durationMs = 120_000L,
            ),
        )

        assertEquals(120_000L, result.newPositionMs)
        assertEquals(10_000L, result.deltaMs)
    }

    @Test
    fun calculateVerticalAdjust_clampsToZero() {
        val result = GestureAlgorithms.calculateVerticalAdjust(
            GestureAlgorithms.VerticalAdjustInput(
                startValue = 5,
                startY = 0f,
                currentY = 1000f,  // large downward swipe → decrease
                sensitivity = 1f,
                maxValue = 100,
            ),
        )
        assertEquals(0, result.newValue)
    }

    @Test
    fun clampZoom_belowMinClamped() {
        val result = GestureAlgorithms.clampZoom(
            GestureAlgorithms.ZoomInput(currentZoom = 0.6f, zoomChange = 0.5f),
        )
        assertEquals(0.5f, result)
    }

    @Test
    fun clampZoom_aboveMaxClamped() {
        val result = GestureAlgorithms.clampZoom(
            GestureAlgorithms.ZoomInput(currentZoom = 2.5f, zoomChange = 2f),
        )
        assertEquals(3.0f, result)
    }
}

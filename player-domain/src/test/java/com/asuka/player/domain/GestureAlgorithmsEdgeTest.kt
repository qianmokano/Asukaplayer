package com.asuka.player.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class GestureAlgorithmsEdgeTest {

    @Test
    fun calculateVerticalAdjust_clampsMax() {
        val input = GestureAlgorithms.VerticalAdjustInput(
            startValue = 90,
            startY = 200f,
            currentY = 0f,
            sensitivity = 2f,
            maxValue = 100,
        )
        val result = GestureAlgorithms.calculateVerticalAdjust(input)
        assertEquals(100, result.newValue)
    }

    @Test
    fun clampPan_zeroZoomKeepsOffset() {
        val input = GestureAlgorithms.PanClampInput(
            zoom = 1f,
            viewWidth = 100f,
            viewHeight = 100f,
            currentX = 10f,
            currentY = -10f,
            panX = 50f,
            panY = 50f,
        )
        val result = GestureAlgorithms.clampPan(input)
        assertEquals(0f, result.clampedX)
        assertEquals(0f, result.clampedY)
    }
}

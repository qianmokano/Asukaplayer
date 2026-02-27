package com.asuka.player.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun calculateSeek_rejectZeroSensitivity() {
        assertFailsWith<IllegalArgumentException> {
            GestureAlgorithms.calculateSeek(
                GestureAlgorithms.SeekInput(0, 0f, 100f, 0f, 10_000),
            )
        }
    }

    @Test
    fun calculateSeek_rejectNegativeSensitivity() {
        assertFailsWith<IllegalArgumentException> {
            GestureAlgorithms.calculateSeek(
                GestureAlgorithms.SeekInput(0, 0f, 100f, -1f, 10_000),
            )
        }
    }

    @Test
    fun calculateVerticalAdjust_rejectZeroSensitivity() {
        assertFailsWith<IllegalArgumentException> {
            GestureAlgorithms.calculateVerticalAdjust(
                GestureAlgorithms.VerticalAdjustInput(50, 100f, 50f, 0f, 100),
            )
        }
    }

    @Test
    fun calculateSeek_noDragReturnsSamePosition() {
        val result = GestureAlgorithms.calculateSeek(
            GestureAlgorithms.SeekInput(50_000, 100f, 100f, 1f, 100_000),
        )
        assertEquals(50_000, result.newPositionMs)
        assertEquals(0, result.deltaMs)
    }

    @Test
    fun calculateVerticalAdjust_noDragReturnsSameValue() {
        val result = GestureAlgorithms.calculateVerticalAdjust(
            GestureAlgorithms.VerticalAdjustInput(50, 100f, 100f, 1f, 100),
        )
        assertEquals(50, result.newValue)
        assertEquals(0, result.delta)
    }

    @Test
    fun calculateSeek_leftDragDecreasesPosition() {
        val result = GestureAlgorithms.calculateSeek(
            GestureAlgorithms.SeekInput(50_000, 200f, 100f, 1f, 100_000),
        )
        assertTrue(result.newPositionMs < 50_000)
        assertTrue(result.deltaMs < 0)
    }

    @Test
    fun clampZoom_identityChangeReturnsCurrentZoom() {
        val result = GestureAlgorithms.clampZoom(
            GestureAlgorithms.ZoomInput(currentZoom = 1.5f, zoomChange = 1f),
        )
        assertEquals(1.5f, result)
    }
}

package com.asuka.player.domain

import kotlin.test.Test
import kotlin.test.assertEquals

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
}

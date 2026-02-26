package com.asuka.player.domain

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Pure gesture math utilities for unit testing.
 */
object GestureAlgorithms {
    data class SeekInput(
        val startPositionMs: Long,
        val startX: Float,
        val currentX: Float,
        val sensitivity: Float,
        val durationMs: Long,
    )

    data class SeekResult(
        val newPositionMs: Long,
        val deltaMs: Long,
    )

    fun calculateSeek(input: SeekInput): SeekResult {
        val raw = input.startPositionMs + ((input.currentX - input.startX) * (input.sensitivity * 100f)).roundToLong()
        val clamped = raw.coerceIn(0L, input.durationMs)
        return SeekResult(newPositionMs = clamped, deltaMs = clamped - input.startPositionMs)
    }

    data class VerticalAdjustInput(
        val startValue: Int,
        val startY: Float,
        val currentY: Float,
        val sensitivity: Float,
        val maxValue: Int,
    )

    data class VerticalAdjustResult(
        val newValue: Int,
        val delta: Int,
    )

    fun calculateVerticalAdjust(input: VerticalAdjustInput): VerticalAdjustResult {
        val delta = ((input.startY - input.currentY) * (input.sensitivity / 10f)).roundToInt()
        val newValue = (input.startValue + delta).coerceIn(0, input.maxValue)
        return VerticalAdjustResult(newValue = newValue, delta = newValue - input.startValue)
    }

    data class ZoomInput(
        val currentZoom: Float,
        val zoomChange: Float,
        val minZoom: Float = 0.25f,
        val maxZoom: Float = 4f,
    )

    fun clampZoom(input: ZoomInput): Float {
        return (input.currentZoom * input.zoomChange).coerceIn(input.minZoom, input.maxZoom)
    }

    data class PanClampInput(
        val zoom: Float,
        val viewWidth: Float,
        val viewHeight: Float,
        val currentX: Float,
        val currentY: Float,
        val panX: Float,
        val panY: Float,
    )

    data class PanClampResult(
        val clampedX: Float,
        val clampedY: Float,
    )

    fun clampPan(input: PanClampInput): PanClampResult {
        val extraWidth = (input.zoom - 1f) * input.viewWidth
        val extraHeight = (input.zoom - 1f) * input.viewHeight
        val maxX = abs(extraWidth / 2f)
        val maxY = abs(extraHeight / 2f)
        val x = (input.currentX + input.zoom * input.panX).coerceIn(-maxX, maxX)
        val y = (input.currentY + input.zoom * input.panY).coerceIn(-maxY, maxY)
        return PanClampResult(clampedX = x, clampedY = y)
    }
}

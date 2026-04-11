package com.asuka.player.domain

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Pure gesture math utilities — no Android dependencies, fully unit-testable.
 *
 * Seek rate model:
 *   Horizontal seek speed scales with media duration so the same swipe covers a
 *   similar fraction of the timeline across short and long videos. At sensitivity
 *   = 1.0, a 300-pixel swipe targets roughly 15 % of the media duration, clamped
 *   to a practical per-pixel range for very short and very long content.
 *
 * Vertical adjust model:
 *   DRAG_PX_PER_UNIT is the number of display pixels of vertical drag required to
 *   change the value by 1 unit at sensitivity = 1.0. Calibrated so that traversing
 *   the full 0–100 range requires ~700 pixels at speed 1.0.
 *
 * Zoom range:
 *   SCALE_MIN = 0.5  — at 50 % of original size the video remains legible.
 *   SCALE_MAX = 3.0  — beyond 3× magnification, pixel artifacts dominate on
 *                       standard 1080p content.
 */
object GestureAlgorithms {

    // ── Seek ─────────────────────────────────────────────────────────────────

    private const val SEEK_REFERENCE_SWIPE_PX = 300f
    private const val SEEK_DURATION_FRACTION = 0.15f
    private const val MIN_SEEK_RATE_MS = 10L
    private const val MAX_SEEK_RATE_MS = 1_500L

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
        require(input.sensitivity > 0f) { "sensitivity must be positive, was ${input.sensitivity}" }
        if (input.durationMs <= 0L) return SeekResult(newPositionMs = input.startPositionMs.coerceAtLeast(0L), deltaMs = 0L)
        val clampedStart = input.startPositionMs.coerceIn(0L, input.durationMs)
        val dragPx = input.currentX - input.startX
        val seekRateMs = input.sensitivity * seekRateMsForDuration(input.durationMs)
        val rawPositionMs = clampedStart + (dragPx * seekRateMs).roundToLong()
        val clampedMs = rawPositionMs.coerceIn(0L, input.durationMs)
        return SeekResult(newPositionMs = clampedMs, deltaMs = clampedMs - clampedStart)
    }

    private fun seekRateMsForDuration(durationMs: Long): Long {
        val proportionalRateMs = durationMs * SEEK_DURATION_FRACTION / SEEK_REFERENCE_SWIPE_PX
        return proportionalRateMs.roundToLong().coerceIn(MIN_SEEK_RATE_MS, MAX_SEEK_RATE_MS)
    }

    data class ProgressBarSeekInput(
        val startPositionMs: Long,
        val startTouchPositionMs: Long,
        val currentTouchPositionMs: Long,
        val durationMs: Long,
        val distanceRatio: Float = 1f,
    )

    fun calculateProgressBarSeek(input: ProgressBarSeekInput): SeekResult {
        require(input.distanceRatio > 0f) { "distanceRatio must be positive, was ${input.distanceRatio}" }
        if (input.durationMs <= 0L) {
            return SeekResult(newPositionMs = input.startPositionMs.coerceAtLeast(0L), deltaMs = 0L)
        }
        val clampedStart = input.startPositionMs.coerceIn(0L, input.durationMs)
        val clampedStartTouch = input.startTouchPositionMs.coerceIn(0L, input.durationMs)
        val clampedCurrentTouch = input.currentTouchPositionMs.coerceIn(0L, input.durationMs)
        val touchDeltaMs = clampedCurrentTouch - clampedStartTouch
        val scaledDeltaMs = (touchDeltaMs.toFloat() * input.distanceRatio).roundToLong()
        val clampedMs = (clampedStart + scaledDeltaMs).coerceIn(0L, input.durationMs)
        return SeekResult(newPositionMs = clampedMs, deltaMs = clampedMs - clampedStart)
    }

    // ── Volume / Brightness ───────────────────────────────────────────────────

    private const val DRAG_PX_PER_UNIT = 7f

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
        require(input.sensitivity > 0f) { "sensitivity must be positive, was ${input.sensitivity}" }
        val verticalDragPx = input.startY - input.currentY   // positive = swipe up = increase
        val steps = (verticalDragPx * input.sensitivity / DRAG_PX_PER_UNIT).roundToInt()
        val newValue = (input.startValue + steps).coerceIn(0, input.maxValue)
        return VerticalAdjustResult(newValue = newValue, delta = newValue - input.startValue)
    }

    // ── Zoom ─────────────────────────────────────────────────────────────────

    data class ZoomInput(
        val currentZoom: Float,
        val zoomChange: Float,
        val minZoom: Float = 0.5f,
        val maxZoom: Float = 3.0f,
    )

    fun clampZoom(input: ZoomInput): Float =
        (input.currentZoom * input.zoomChange).coerceIn(input.minZoom, input.maxZoom)

    // ── Pan ──────────────────────────────────────────────────────────────────

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

    /**
     * Clamps the accumulated pan offset so the video never scrolls beyond its
     * visible edges. When scaled by [PanClampInput.zoom], each axis overhangs the
     * viewport by (zoom − 1) × dimension; the centre-anchored limit on each side
     * is therefore half that value.
     */
    fun clampPan(input: PanClampInput): PanClampResult {
        val overhangW = (input.zoom - 1f) * input.viewWidth
        val overhangH = (input.zoom - 1f) * input.viewHeight
        val boundX = (overhangW * 0.5f).coerceAtLeast(0f)
        val boundY = (overhangH * 0.5f).coerceAtLeast(0f)
        val x = (input.currentX + input.panX).coerceIn(-boundX, boundX)
        val y = (input.currentY + input.panY).coerceIn(-boundY, boundY)
        return PanClampResult(clampedX = x, clampedY = y)
    }
}

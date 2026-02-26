package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Holds the current video transform (scale + pan) for a pinch-zoom / pan session.
 *
 * [scale]     – multiplicative zoom factor; 1.0 = fit to viewport.
 * [panOffset] – translation applied after scaling, in display pixels.
 * [pinching]  – true while a pinch-zoom gesture is in progress.
 */
class ZoomState {
    var scale: Float by mutableFloatStateOf(1f)
        private set

    var panOffset: Offset by mutableStateOf(Offset.Zero)
        private set

    var pinching: Boolean by mutableStateOf(false)
        private set

    fun setTransform(scale: Float, panOffset: Offset, pinching: Boolean) {
        this.scale = scale
        this.panOffset = panOffset
        this.pinching = pinching
    }
}

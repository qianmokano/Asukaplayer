package com.asuka.player.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

class ZoomState {
    var zoom: Float by mutableFloatStateOf(1f)
        private set

    var offset: Offset by mutableStateOf(Offset.Zero)
        private set

    var isZooming: Boolean by mutableStateOf(false)
        private set

    fun update(zoom: Float, offset: Offset, isZooming: Boolean) {
        this.zoom = zoom
        this.offset = offset
        this.isZooming = isZooming
    }
}

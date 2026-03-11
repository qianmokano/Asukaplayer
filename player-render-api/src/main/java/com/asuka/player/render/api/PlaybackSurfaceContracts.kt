package com.asuka.player.render.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.asuka.player.contract.VideoScaleMode

interface PlaybackSurfaceState

data class PlaybackSurfaceTransform(
    val zoomScale: Float = 1f,
    val panOffset: Offset = Offset.Zero,
    val videoScaleMode: VideoScaleMode = VideoScaleMode.FIT,
)

interface PlaybackSurfaceRenderer {
    @Composable
    fun Render(
        modifier: Modifier,
        surfaceState: PlaybackSurfaceState,
        transform: PlaybackSurfaceTransform,
    )
}

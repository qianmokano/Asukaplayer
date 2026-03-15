package com.asuka.player.render.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asuka.player.contract.VideoScaleMode

interface PlaybackSurfaceState

data class PlaybackSurfaceTransform(
    val zoomScale: Float = 1f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
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

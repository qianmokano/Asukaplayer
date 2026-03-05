package com.asuka.player.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import com.asuka.player.ui.utils.VideoScaleModeMapper
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.state.ZoomState

/**
 * Video surface with zoom/pan applied.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoSurfaceWithTransform(
    modifier: Modifier = Modifier,
    player: Player,
    zoomState: ZoomState,
    scaleState: ScaleState,
) {
    val presentationState = rememberPresentationState(player)
    val sourceSizeDp = presentationState.videoSizeDp
    val measuredSourceSize = if (sourceSizeDp != null && sourceSizeDp != Size.Unspecified) {
        sourceSizeDp
    } else {
        val size = player.videoSize
        if (size.width > 0 && size.height > 0) {
            Size(size.width.toFloat(), size.height.toFloat())
        } else {
            null
        }
    }
    val hasStableSourceSize = measuredSourceSize != null

    PlayerSurface(
        player = player,
        // TextureView makes contentScale and transform changes visible and reliable across devices.
        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
        modifier = if (hasStableSourceSize) {
            modifier
                .fillMaxSize()
                .resizeWithContentScale(
                    contentScale = VideoScaleModeMapper.toContentScale(scaleState.mode),
                    sourceSizeDp = measuredSourceSize,
                )
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.panOffset.x
                    translationY = zoomState.panOffset.y
                }
        } else {
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomState.scale
                    scaleY = zoomState.scale
                    translationX = zoomState.panOffset.x
                    translationY = zoomState.panOffset.y
                    alpha = 0f
                }
        },
    )
}

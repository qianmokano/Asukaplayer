package com.asuka.player.renderer.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.rememberPresentationState
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import com.asuka.player.renderer.Media3PlaybackSurfaceState
import com.asuka.player.render.api.PlaybackSurfaceRenderer
import com.asuka.player.render.api.PlaybackSurfaceState
import com.asuka.player.render.api.PlaybackSurfaceTransform
import com.asuka.player.ui.utils.VideoScaleModeMapper

/**
 * Video surface with zoom/pan applied.
 */
@OptIn(UnstableApi::class)
internal object Media3PlaybackSurfaceRenderer : PlaybackSurfaceRenderer {
    @Composable
    override fun Render(
        modifier: Modifier,
        surfaceState: PlaybackSurfaceState,
        transform: PlaybackSurfaceTransform,
    ) {
        val player = (surfaceState as? Media3PlaybackSurfaceState)?.player ?: return
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
        // coverSurface is true during media transitions before the new video size is known.
        // Without this check, the old video's size would be used for aspect ratio calculation,
        // causing a brief flash of incorrect proportions when switching videos.
        val hasStableSourceSize = measuredSourceSize != null && !presentationState.coverSurface

        PlayerSurface(
            player = player,
            // TextureView makes contentScale and transform changes visible and reliable across devices.
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            modifier = if (hasStableSourceSize) {
                modifier
                    .fillMaxSize()
                    .resizeWithContentScale(
                        contentScale = VideoScaleModeMapper.toContentScale(transform.videoScaleMode),
                        sourceSizeDp = measuredSourceSize,
                    )
                    .graphicsLayer {
                        scaleX = transform.zoomScale
                        scaleY = transform.zoomScale
                        translationX = transform.panOffsetX
                        translationY = transform.panOffsetY
                    }
            } else {
                modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = transform.zoomScale
                        scaleY = transform.zoomScale
                        translationX = transform.panOffsetX
                        translationY = transform.panOffsetY
                        alpha = 0f
                    }
            },
        )
    }
}

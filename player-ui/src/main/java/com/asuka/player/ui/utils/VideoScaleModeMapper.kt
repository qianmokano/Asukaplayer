package com.asuka.player.ui.utils

import androidx.compose.ui.layout.ContentScale
import com.asuka.player.core.VideoScaleMode

object VideoScaleModeMapper {
    fun toContentScale(mode: VideoScaleMode): ContentScale {
        return when (mode) {
            VideoScaleMode.FIT -> ContentScale.Fit
            VideoScaleMode.FILL -> ContentScale.FillWidth
            VideoScaleMode.CROP -> ContentScale.Crop
            VideoScaleMode.STRETCH -> ContentScale.FillBounds
        }
    }
}

package com.asuka.player.renderer

import androidx.media3.common.Player
import com.asuka.player.render.api.PlaybackSurfaceState

internal data class Media3PlaybackSurfaceState(
    val player: Player,
) : PlaybackSurfaceState

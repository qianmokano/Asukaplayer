package com.asuka.player.ui.controller

import androidx.media3.session.MediaController
import com.asuka.player.core.TrackSelectionFacade

class ControllerBindings(
    val trackSelection: TrackSelectionFacade,
) {
    companion object {
        fun from(mediaController: MediaController): ControllerBindings {
            return ControllerBindings(trackSelection = TrackSelectionFacade(mediaController))
        }
    }
}

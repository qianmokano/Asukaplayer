package com.asuka.player.platform

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackTrackSelectionController
import com.google.common.util.concurrent.ListenableFuture

interface PlaybackControllerConnector {
    fun buildAsync(): ListenableFuture<MediaController>

    fun asPlaybackController(mediaController: MediaController): PlaybackController

    fun asTrackSelectionController(mediaController: MediaController): PlaybackTrackSelectionController

    fun release()
}

fun interface PlaybackControllerConnectorFactory {
    fun create(
        context: Context,
        playbackServiceComponent: ComponentName,
    ): PlaybackControllerConnector
}

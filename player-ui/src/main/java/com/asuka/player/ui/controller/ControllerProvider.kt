package com.asuka.player.ui.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.asuka.player.core.impl.Media3PlaybackController

/**
 * Binds a MediaController from an injected SessionToken and exposes UI-specific wrappers.
 */
class ControllerProvider(
    private val context: Context,
    private val playbackServiceComponent: ComponentName,
) {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    fun buildAsync(): ListenableFuture<MediaController> {
        val future = MediaController.Builder(
            context,
            SessionToken(context, playbackServiceComponent),
        ).buildAsync()
        controllerFuture = future
        return future
    }

    fun asPlaybackController(mediaController: MediaController): Media3PlaybackController {
        return Media3PlaybackController(mediaController)
    }

    fun asTrackSelectionController(mediaController: MediaController): PlaybackTrackSelectionController {
        return DefaultPlaybackTrackSelectionController(mediaController)
    }

    fun release() {
        controllerFuture?.let {
            // If the future is already resolved, MediaController.releaseFuture()
            // both disconnects the controller and releases the underlying service binding.
            // If not yet resolved, it cancels the pending connection.
            MediaController.releaseFuture(it)
        }
        controllerFuture = null
    }
}

package com.asuka.player.ui.controller

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.asuka.player.core.impl.Media3PlaybackController
import com.asuka.player.core.service.PlaybackService

/**
 * Builds a MediaController and wraps it as PlaybackController.
 * This is UI-facing glue and can be replaced by DI in later milestones.
 */
class ControllerProvider(private val context: Context) {

    private var controllerFuture: ListenableFuture<MediaController>? = null

    fun buildAsync(): ListenableFuture<MediaController> {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        return future
    }

    fun asPlaybackController(mediaController: MediaController): Media3PlaybackController {
        return Media3PlaybackController(mediaController)
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

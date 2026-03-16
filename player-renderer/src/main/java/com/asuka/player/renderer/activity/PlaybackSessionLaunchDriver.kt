package com.asuka.player.renderer.activity

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.renderer.controller.PlaybackSessionCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PlaybackSessionLaunchDriver(
    private val scope: CoroutineScope,
    private val launchOrchestrator: PlaybackLaunchOrchestrator,
    private val mediaControllerProvider: () -> MediaController?,
    private val sessionCoordinatorProvider: () -> PlaybackSessionCoordinator?,
    private val applyArtwork: (MediaController, PlaybackSessionRequest, Int, Uri) -> Unit,
) {
    private var playbackStartJob: Job? = null
    private var appliedRequestId: Long = 0L
    private var boundPlayer: Player? = null

    val playbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_READY || !shouldHandlePlaybackEvent()) return
            val mediaController = mediaControllerProvider() ?: return
            val currentUri = mediaController.currentMediaItem?.localConfiguration?.uri ?: return
            launchOrchestrator.handlePlaybackReady(
                requestId = launchOrchestrator.currentRequestId(),
                currentUri = currentUri,
                isSeekable = mediaController.isCurrentMediaItemSeekable,
            ) {
                startCurrentRequest()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (!shouldHandlePlaybackEvent()) return
            val mediaController = mediaControllerProvider() ?: return
            val currentUri = mediaController.currentMediaItem?.localConfiguration?.uri
                ?: launchOrchestrator.currentPlaybackUri()
            launchOrchestrator.handlePlaybackError(
                requestId = launchOrchestrator.currentRequestId(),
                currentUri = currentUri,
                error = error,
            ) {
                startCurrentRequest()
            }
        }
    }

    fun bindPlayer(player: Player) {
        if (boundPlayer !== player) {
            boundPlayer?.removeListener(playbackListener)
            boundPlayer = player
        }
        player.removeListener(playbackListener)
        player.addListener(playbackListener)
    }

    fun unbindPlayer() {
        boundPlayer?.removeListener(playbackListener)
        boundPlayer = null
    }

    fun updateIntent(
        intent: android.content.Intent?,
        supersedeRequest: Boolean = false,
        clearSeekFallbackAttempts: Boolean = false,
    ): Long {
        return launchOrchestrator.updateIntent(
            intent = intent,
            supersedeRequest = supersedeRequest,
            clearSeekFallbackAttempts = clearSeekFallbackAttempts,
        )
    }

    fun currentRequestId(): Long = launchOrchestrator.currentRequestId()

    fun clearAppliedRequest() {
        appliedRequestId = 0L
    }

    fun cancelPending() {
        launchOrchestrator.cancelPending()
        playbackStartJob?.cancel()
        playbackStartJob = null
    }

    fun startCurrentRequest(
        requestId: Long = launchOrchestrator.currentRequestId(),
    ) {
        if (requestId == 0L) return
        playbackStartJob?.cancel()
        val job = scope.launch {
            startRequest(requestId)
        }
        playbackStartJob = job
        job.invokeOnCompletion {
            if (playbackStartJob === job) {
                playbackStartJob = null
            }
        }
    }

    private suspend fun startRequest(requestId: Long) {
        val mediaController = mediaControllerProvider() ?: return
        launchOrchestrator.startPlayback(
            requestId = requestId,
            prepareLaunch = { request ->
                sessionCoordinatorProvider()?.prepareStart(request)?.let { startResult ->
                    PlaybackLaunchResult(
                        request = startResult.request,
                        plan = startResult.plan,
                    )
                }
            },
            applyLaunch = { result ->
                sessionCoordinatorProvider()?.applyStart(
                    PlaybackSessionCoordinator.StartResult(
                        request = result.request,
                        plan = result.plan,
                    ),
                )
                appliedRequestId = requestId
            },
            applyArtwork = { request, startIndex, targetUri ->
                applyArtwork(mediaController, request, startIndex, targetUri)
            },
        )
    }

    private fun shouldHandlePlaybackEvent(): Boolean {
        val currentRequestId = launchOrchestrator.currentRequestId()
        return appliedRequestId != 0L && appliedRequestId == currentRequestId
    }
}

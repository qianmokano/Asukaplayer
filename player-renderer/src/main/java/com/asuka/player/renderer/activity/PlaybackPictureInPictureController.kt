package com.asuka.player.renderer.activity

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.ui.R

internal data class PictureInPictureAspectRatio(
    val numerator: Int,
    val denominator: Int,
) {
    fun toRational(): Rational = Rational(numerator, denominator)
}

internal class PlaybackPictureInPictureController(
    private val activity: ComponentActivity,
    private val currentPlayerProvider: () -> Player?,
    private val currentControllerProvider: () -> PlaybackController?,
    private val setPictureInPictureParams: (android.app.PictureInPictureParams) -> Unit = activity::setPictureInPictureParams,
    private val enterPictureInPicture: (android.app.PictureInPictureParams) -> Boolean = activity::enterPictureInPictureMode,
) {
    private var runtimeSettings: PlaybackRuntimeSettings = PlaybackRuntimeSettings()
    private var videoRect: Rect? = null
    private var receiverRegistered = false
    private var attachedPlayer: Player? = null
    private var stableAspectRatio: PictureInPictureAspectRatio? = null

    private val pipPlayerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePictureInPictureParamsIfSupported(syncAspectRatio = false)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (refreshStableAspectRatio(videoSize.width, videoSize.height)) {
                updatePictureInPictureParamsIfSupported(syncAspectRatio = false)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val controller = currentControllerProvider() ?: return
            when (intent?.getIntExtra(EXTRA_PIP_CONTROL, 0)) {
                PIP_CONTROL_PLAY -> controller.play()
                PIP_CONTROL_PAUSE -> controller.pause()
                PIP_CONTROL_PREV -> controller.skipToPrevious()
                PIP_CONTROL_NEXT -> controller.skipToNext()
            }
        }
    }

    fun updateRuntimeSettings(settings: PlaybackRuntimeSettings) {
        runtimeSettings = settings
    }

    fun updateVideoBounds(bounds: Rect?) {
        videoRect = bounds
    }

    fun onPictureInPictureModeChanged(transition: PictureInPictureTransition) {
        if (transition.shouldRegisterReceiver) {
            registerReceiver()
        } else {
            unregisterReceiver()
        }
        if (transition.shouldAttachPlayStateListener) {
            attachPlayerListener()
        } else {
            detachPlayerListener()
        }
    }

    fun updatePictureInPictureParamsIfSupported() {
        updatePictureInPictureParamsIfSupported(syncAspectRatio = true)
    }

    private fun updatePictureInPictureParamsIfSupported(syncAspectRatio: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (syncAspectRatio) {
            refreshStableAspectRatio(currentPlayerProvider())
        }
        runCatching {
            setPictureInPictureParams(buildPictureInPictureParams())
        }
    }

    fun enterPictureInPictureMode(
        beforeEnter: () -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        beforeEnter()
        registerReceiver()
        attachPlayerListener()
        refreshStableAspectRatio(currentPlayerProvider())
        enterPictureInPicture(buildPictureInPictureParams())
    }

    fun release() {
        detachPlayerListener()
        unregisterReceiver()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPictureInPictureParams(): android.app.PictureInPictureParams {
        val builder = android.app.PictureInPictureParams.Builder()
        stableAspectRatio
            ?.toRational()
            ?.let(builder::setAspectRatio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(runtimeSettings.autoPip)
            builder.setSeamlessResizeEnabled(true)
        }
        videoRect?.let(builder::setSourceRectHint)
        builder.setActions(buildActions())
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildActions(): List<android.app.RemoteAction> {
        val player = currentPlayerProvider() ?: return emptyList()

        fun pendingIntent(control: Int): PendingIntent {
            return PendingIntent.getBroadcast(
                activity,
                RC_PIP_BASE + control,
                Intent(ACTION_PIP_CONTROL)
                    .setPackage(activity.packageName)
                    .putExtra(EXTRA_PIP_CONTROL, control),
                PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val previous = android.app.RemoteAction(
            Icon.createWithResource(activity, R.drawable.pip_ic_skip_previous),
            activity.getString(R.string.prev),
            activity.getString(R.string.prev),
            pendingIntent(PIP_CONTROL_PREV),
        )
        val playPause = if (player.isPlaying) {
            android.app.RemoteAction(
                Icon.createWithResource(activity, R.drawable.pip_ic_pause),
                activity.getString(R.string.play_pause),
                activity.getString(R.string.play_pause),
                pendingIntent(PIP_CONTROL_PAUSE),
            )
        } else {
            android.app.RemoteAction(
                Icon.createWithResource(activity, R.drawable.pip_ic_play),
                activity.getString(R.string.play_pause),
                activity.getString(R.string.play_pause),
                pendingIntent(PIP_CONTROL_PLAY),
            )
        }
        val next = android.app.RemoteAction(
            Icon.createWithResource(activity, R.drawable.pip_ic_skip_next),
            activity.getString(R.string.next),
            activity.getString(R.string.next),
            pendingIntent(PIP_CONTROL_NEXT),
        )
        return listOf(previous, playPause, next)
    }

    private fun registerReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            activity,
            receiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun unregisterReceiver() {
        if (!receiverRegistered) return
        runCatching { activity.unregisterReceiver(receiver) }
        receiverRegistered = false
    }

    private fun attachPlayerListener() {
        val player = currentPlayerProvider() ?: return
        if (attachedPlayer === player) return
        detachPlayerListener()
        refreshStableAspectRatio(player)
        player.addListener(pipPlayerListener)
        attachedPlayer = player
    }

    private fun detachPlayerListener() {
        attachedPlayer?.removeListener(pipPlayerListener)
        attachedPlayer = null
    }

    private fun refreshStableAspectRatio(player: Player?): Boolean {
        val videoSize = player?.videoSize ?: return false
        return refreshStableAspectRatio(videoSize.width, videoSize.height)
    }

    private fun refreshStableAspectRatio(width: Int, height: Int): Boolean {
        val next = resolveAspectRatioPreservingPrevious(
            previous = stableAspectRatio,
            width = width,
            height = height,
        )
        if (next == stableAspectRatio) return false
        stableAspectRatio = next
        return true
    }

    companion object {
        private const val ACTION_PIP_CONTROL = "com.asuka.player.pip.CONTROL"
        private const val EXTRA_PIP_CONTROL = "pip_control"
        private const val PIP_CONTROL_PLAY = 1
        private const val PIP_CONTROL_PAUSE = 2
        private const val PIP_CONTROL_PREV = 3
        private const val PIP_CONTROL_NEXT = 4
        private const val RC_PIP_BASE = 100

        internal fun resolveAspectRatio(
            width: Int,
            height: Int,
        ): PictureInPictureAspectRatio? {
            if (width <= 0 || height <= 0) return null
            val ratio = width.toFloat() / height.toFloat()
            return when {
                ratio < 0.5f -> PictureInPictureAspectRatio(1, 2)
                ratio > 2.39f -> PictureInPictureAspectRatio(239, 100)
                else -> PictureInPictureAspectRatio(width, height)
            }
        }

        internal fun resolveAspectRatioPreservingPrevious(
            previous: PictureInPictureAspectRatio?,
            width: Int,
            height: Int,
        ): PictureInPictureAspectRatio? {
            return resolveAspectRatio(width, height) ?: previous
        }
    }
}

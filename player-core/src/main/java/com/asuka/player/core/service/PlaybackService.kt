package com.asuka.player.core.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.asuka.player.core.PlaybackStateWriter
import com.asuka.player.core.PlaybackStoreProvider
import com.asuka.player.core.QueueHistoryWriter
import com.asuka.player.core.impl.Media3PlaybackController

/**
 * Clean-room playback service. Owns ExoPlayer + MediaSession.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var writer: PlaybackStateWriter? = null
    private var historyWriter: QueueHistoryWriter? = null

    private val sessionCallback = object : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == Media3PlaybackController.CMD_SET_VIDEO_SCALE_TYPE) {
                val scaleType = args.getInt(Media3PlaybackController.ARG_SCALE_TYPE, C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                player?.setVideoScalingMode(scaleType)
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    override fun onCreate() {
        super.onCreate()
        PlaybackStoreProvider.init(this)
        val w = PlaybackStateWriter(PlaybackStoreProvider.store)
        writer = w
        val hw = QueueHistoryWriter(PlaybackStoreProvider.history)
        historyWriter = hw

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val renderersFactory = DefaultRenderersFactory(this)
            .setEnableDecoderFallback(true)

        val exoPlayer = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .build()
        player = exoPlayer

        val builder = MediaSession.Builder(this, exoPlayer)
        val activity = buildSessionActivity()
        if (activity != null) {
            builder.setSessionActivity(activity)
        }
        session = builder.setCallback(sessionCallback).build()

        w.attach(exoPlayer)
        exoPlayer.addListener(hw)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return session
    }

    override fun onDestroy() {
        try {
            player?.let { writer?.detach(it) }
            historyWriter?.let { player?.removeListener(it) }
            session?.release()
            player?.release()
        } finally {
            session = null
            player = null
            writer = null
            historyWriter = null
            super.onDestroy()
        }
    }

    private fun buildSessionActivity(): PendingIntent? {
        val cls = activityClass
        val intent = if (cls != null) {
            Intent(this, cls).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
                ?: return null
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        /**
         * Set by PlaybackActivity before connecting so the media-session notification
         * navigates back to the playback screen instead of the launcher activity.
         * PlaybackActivity sets this in onCreate(), which always runs before the service
         * is created (the service is started by the MediaController connection).
         */
        @Volatile var activityClass: Class<*>? = null
    }
}

package com.asuka.player.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.asuka.player.core.R
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

    private val notificationManager: NotificationManager by lazy {
        getSystemService(NotificationManager::class.java)
    }

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
        ensurePlaybackNotificationChannel()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.playback_notification_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(notificationSmallIconResId) },
        )
        // When playback pauses briefly (e.g. buffering transitions or rapid play/pause),
        // keep the service in the foreground for a short grace period to avoid churn.
        setForegroundServiceTimeoutMs(10_000L)
        // Allow the notification to remain visible after playback is paused/stopped, but avoid
        // showing a notification for a brand-new idle player that hasn't started playback yet.
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_AFTER_STOP_OR_ERROR)

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
        session = builder.setCallback(sessionCallback).build().also { addSession(it) }

        w.attach(exoPlayer)
        exoPlayer.addListener(hw)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return session
    }

    override fun onDestroy() {
        try {
            session?.let { removeSession(it) }
            player?.let { writer?.detach(it) }
            historyWriter?.let { player?.removeListener(it) }
        } finally {
            try {
                session?.release()
            } finally {
                try {
                    player?.release()
                } finally {
                    session = null
                    player = null
                    writer = null
                    historyWriter = null
                    super.onDestroy()
                }
            }
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

    private fun ensurePlaybackNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.playback_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.playback_notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "asuka_playback"
        private const val NOTIFICATION_ID = 1001

        /**
         * Set in Application.onCreate() so the media-session notification
         * navigates back to the playback screen instead of the launcher activity.
         *
         * **Ordering guarantee:** Application.onCreate() is guaranteed to run
         * before any Activity, Service, or BroadcastReceiver, so this is always
         * set before the service is created.
         */
        @Volatile var activityClass: Class<*>? = null

        @DrawableRes
        @Volatile
        var notificationSmallIconResId: Int = R.drawable.ic_stat_playback
    }
}

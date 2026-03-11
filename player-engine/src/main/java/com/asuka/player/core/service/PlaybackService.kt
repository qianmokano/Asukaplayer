package com.asuka.player.engine.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.asuka.player.platform.PlaybackCustomCommands
import com.asuka.player.platform.PlaybackDependenciesProvider
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.platform.PlaybackStateWriter
import com.asuka.player.platform.QueueHistoryWriter
import com.asuka.player.core.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private val playbackDependencies: PlaybackServiceDependencies by lazy(LazyThreadSafetyMode.NONE) {
        (application as? PlaybackDependenciesProvider)?.playbackServiceDependencies
            ?: error("Application does not provide PlaybackServiceDependencies.")
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val persistenceDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var writer: PlaybackStateWriter? = null
    private var historyWriter: QueueHistoryWriter? = null
    private val positionCheckpointRunnable = object : Runnable {
        override fun run() {
            writer?.checkpoint(SystemClock.elapsedRealtime())
            mainHandler.postDelayed(this, PlaybackStateWriter.POSITION_CHECKPOINT_INTERVAL_MS)
        }
    }

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
            if (customCommand.customAction == PlaybackCustomCommands.CMD_SET_VIDEO_SCALE_TYPE) {
                val scaleType = args.getInt(PlaybackCustomCommands.ARG_SCALE_TYPE, C.VIDEO_SCALING_MODE_SCALE_TO_FIT)
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
                .apply { setSmallIcon(playbackDependencies.notificationSmallIconResId) },
        )
        setForegroundServiceTimeoutMs(10_000L)
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_AFTER_STOP_OR_ERROR)

        val w = PlaybackStateWriter(
            store = playbackDependencies.playbackStore,
            writeDispatcher = persistenceDispatcher,
        )
        writer = w
        val hw = QueueHistoryWriter(
            store = playbackDependencies.queueHistoryStore,
            writeDispatcher = persistenceDispatcher,
        )
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
        mainHandler.postDelayed(
            positionCheckpointRunnable,
            PlaybackStateWriter.POSITION_CHECKPOINT_INTERVAL_MS,
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return session
    }

    override fun onDestroy() {
        runCatching { mainHandler.removeCallbacks(positionCheckpointRunnable) }
        runCatching {
            runBlocking(Dispatchers.IO) {
                writer?.flushCurrentPositionAndAwait()
                writer?.awaitIdle()
                historyWriter?.awaitIdle()
            }
        }
        runCatching { session?.let { removeSession(it) } }
        runCatching { player?.let { writer?.detach(it) } }
        runCatching { historyWriter?.let { player?.removeListener(it) } }
        runCatching { session?.release() }
        runCatching { player?.release() }
        runCatching { writer?.close() }
        runCatching { historyWriter?.close() }
        session = null
        player = null
        writer = null
        historyWriter = null
        super.onDestroy()
    }

    private fun buildSessionActivity(): PendingIntent? {
        val cls = playbackDependencies.sessionActivityClass
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
    }
}

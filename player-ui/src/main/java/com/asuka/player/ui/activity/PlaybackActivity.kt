package com.asuka.player.ui.activity

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.core.requirePlaybackCoreGraph
import com.asuka.player.ui.PlayerScreen
import com.asuka.player.ui.R

/**
 * Minimal playback Activity for M0.
 * Starts MediaController, sets a single media item, and renders minimal UI.
 */
class PlaybackActivity : ComponentActivity() {
    companion object {
        private const val ACTION_PIP_CONTROL = "com.asuka.player.pip.CONTROL"
        private const val EXTRA_PIP_CONTROL = "pip_control"
        private const val PIP_CONTROL_PLAY = 1
        private const val PIP_CONTROL_PAUSE = 2
        private const val PIP_CONTROL_PREV = 3
        private const val PIP_CONTROL_NEXT = 4
        private const val RC_PIP_BASE = 100
    }

    private var composableIsPip by mutableStateOf(false)
    private var videoRect: android.graphics.Rect? = null
    private val backgroundPolicy = com.asuka.player.ui.controller.BackgroundPlaybackPolicy()
    private val playbackGraph by lazy { applicationContext.requirePlaybackCoreGraph() }
    private val playbackPrefs by lazy { getSharedPreferences("player_runtime", MODE_PRIVATE) }
    private val playbackStateRepository by lazy { playbackGraph.playbackStateRepository }
    private val sessionHost by lazy {
        PlaybackSessionHost(
            contentResolver = contentResolver,
            cacheDir = cacheDir,
            scope = lifecycleScope,
            graph = playbackGraph,
            controllerContext = this,
        )
    }
    private lateinit var runtimeSettings: PlaybackRuntimeSettings
    private var pipReceiverRegistered = false

    private val pipPlayStateListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!composableIsPip) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setPictureInPictureParams(buildPipParams())
            }
        }
    }

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val ctrl = sessionHost.currentController ?: return
            when (intent?.getIntExtra(EXTRA_PIP_CONTROL, 0)) {
                PIP_CONTROL_PLAY  -> ctrl.play()
                PIP_CONTROL_PAUSE -> ctrl.pause()
                PIP_CONTROL_PREV  -> ctrl.skipToPrevious()
                PIP_CONTROL_NEXT  -> ctrl.skipToNext()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtimeSettings = readRuntimeSettings(intent)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addOnPictureInPictureModeChangedListener { info ->
                composableIsPip = info.isInPictureInPictureMode
                backgroundPolicy.setPictureInPicture(info.isInPictureInPictureMode)
                if (info.isInPictureInPictureMode) {
                    registerPipReceiver()
                    sessionHost.currentPlayer?.addListener(pipPlayStateListener)
                } else {
                    sessionHost.currentPlayer?.removeListener(pipPlayStateListener)
                    unregisterPipReceiver()
                }
            }
        }

        applyRememberedBrightnessIfNeeded()
        backgroundPolicy.update(
            retainControllerConnection = runtimeSettings.keepSessionConnectionInBackground,
            autoBackgroundPlaybackEnabled = runtimeSettings.autoBackgroundPlay,
        )
        applyStatusBarVisibilityForOrientation()

        setContent {
            val hostState by sessionHost.state.collectAsState()
            val controller = hostState.controller ?: return@setContent
            PlayerScreen(
                uiState = hostState.uiState,
                player = hostState.player,
                controller = controller,
                bindings = hostState.bindings,
                playbackStateRepository = playbackStateRepository,
                settings = runtimeSettings,
                isInPip = composableIsPip,
                onVideoBoundsChanged = { videoRect = it },
                onBack = { finish() },
                onPip = { enterPipMode() },
                onBackground = {
                    backgroundPolicy.requestBackgroundPlayback()
                    finish()
                },
                onRotate = { toggleOrientation() },
            )
        }

        sessionHost.ensureControllerReady(intent, runtimeSettings)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runtimeSettings = readRuntimeSettings(intent)
        backgroundPolicy.update(
            retainControllerConnection = runtimeSettings.keepSessionConnectionInBackground,
            autoBackgroundPlaybackEnabled = runtimeSettings.autoBackgroundPlay,
        )
        sessionHost.onNewIntent(intent, runtimeSettings)
    }

    override fun onStart() {
        super.onStart()
        backgroundPolicy.update(
            retainControllerConnection = runtimeSettings.keepSessionConnectionInBackground,
            autoBackgroundPlaybackEnabled = runtimeSettings.autoBackgroundPlay,
        )
        backgroundPolicy.clearManualBackgroundPlaybackRequest()
        applyStatusBarVisibilityForOrientation()
        sessionHost.ensureControllerReady(intent, runtimeSettings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyStatusBarVisibilityForOrientation()
    }

    override fun onStop() {
        if (runtimeSettings.rememberBrightness) {
            val brightness = window.attributes.screenBrightness
            if (brightness >= 0f) {
                playbackPrefs.edit().putFloat("last_brightness", brightness).apply()
            }
        }
        sessionHost.onStop(backgroundPolicy.shouldRetainSession())
        super.onStop()
    }

    override fun onDestroy() {
        sessionHost.currentPlayer?.removeListener(pipPlayStateListener)
        unregisterPipReceiver()
        sessionHost.releaseAll()
        super.onDestroy()
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Enable background before enterPictureInPictureMode so that onStop() —
            // which fires synchronously after the transition — does not release resources.
            backgroundPolicy.setPictureInPicture(true)
            registerPipReceiver()
            enterPictureInPictureMode(buildPipParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): android.app.PictureInPictureParams {
        val builder = android.app.PictureInPictureParams.Builder()
        val player = sessionHost.currentPlayer
        if (player != null) {
            val w = player.videoSize.width
            val h = player.videoSize.height
            if (w > 0 && h > 0) {
                val ratio = w.toFloat() / h.toFloat()
                if (ratio < 0.5f) {
                    builder.setAspectRatio(Rational(1, 2))
                } else if (ratio > 2.39f) {
                    builder.setAspectRatio(Rational(239, 100))
                } else {
                    builder.setAspectRatio(Rational(w, h))
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(runtimeSettings.autoPip)
            builder.setSeamlessResizeEnabled(true)
        }
        videoRect?.let { builder.setSourceRectHint(it) }
        builder.setActions(buildPipActions())
        return builder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipActions(): List<android.app.RemoteAction> {
        val player = sessionHost.currentPlayer ?: return emptyList()

        fun makeIntent(control: Int): PendingIntent = PendingIntent.getBroadcast(
            this,
            RC_PIP_BASE + control,
            Intent(ACTION_PIP_CONTROL).setPackage(packageName).putExtra(EXTRA_PIP_CONTROL, control),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val prevAction = android.app.RemoteAction(
            Icon.createWithResource(this, R.drawable.pip_ic_skip_previous),
            getString(R.string.prev), getString(R.string.prev),
            makeIntent(PIP_CONTROL_PREV),
        )
        val playPauseAction = if (player.isPlaying) {
            android.app.RemoteAction(
                Icon.createWithResource(this, R.drawable.pip_ic_pause),
                getString(R.string.play_pause), getString(R.string.play_pause),
                makeIntent(PIP_CONTROL_PAUSE),
            )
        } else {
            android.app.RemoteAction(
                Icon.createWithResource(this, R.drawable.pip_ic_play),
                getString(R.string.play_pause), getString(R.string.play_pause),
                makeIntent(PIP_CONTROL_PLAY),
            )
        }
        val nextAction = android.app.RemoteAction(
            Icon.createWithResource(this, R.drawable.pip_ic_skip_next),
            getString(R.string.next), getString(R.string.next),
            makeIntent(PIP_CONTROL_NEXT),
        )

        return listOf(prevAction, playPauseAction, nextAction)
    }

    private fun registerPipReceiver() {
        if (pipReceiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            pipReceiver,
            IntentFilter(ACTION_PIP_CONTROL),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        pipReceiverRegistered = true
    }

    private fun unregisterPipReceiver() {
        if (!pipReceiverRegistered) return
        runCatching { unregisterReceiver(pipReceiver) }
        pipReceiverRegistered = false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (runtimeSettings.autoPip) {
            enterPipMode()
        }
    }

    private fun toggleOrientation() {
        val current = if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            requestedOrientation
        } else {
            if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        requestedOrientation = if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    private fun applyStatusBarVisibilityForOrientation() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun readRuntimeSettings(intent: Intent?): PlaybackRuntimeSettings {
        @Suppress("DEPRECATION")
        val fromParcel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(PlaybackRuntimeSettings.EXTRA_KEY, PlaybackRuntimeSettings::class.java)
        } else {
            intent?.getParcelableExtra(PlaybackRuntimeSettings.EXTRA_KEY)
        }
        return fromParcel ?: PlaybackRuntimeSettings()
    }

    private fun applyRememberedBrightnessIfNeeded() {
        if (!runtimeSettings.rememberBrightness) return
        val remembered = playbackPrefs.getFloat("last_brightness", -1f)
        if (remembered < 0f) return
        val attrs = window.attributes
        attrs.screenBrightness = remembered.coerceIn(0f, 1f)
        window.attributes = attrs
    }
}

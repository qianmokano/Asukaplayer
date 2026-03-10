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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import com.asuka.player.core.PlaybackActivityDependencies
import com.asuka.player.core.PlaybackDeviceController
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.core.PlaybackUiPersistence
import com.asuka.player.core.requirePlaybackActivityDependencies
import com.asuka.player.ui.PlaybackScreenDependencies
import com.asuka.player.ui.PlaybackScreenModel
import com.asuka.player.ui.PlayerScreen
import com.asuka.player.ui.R
import kotlinx.coroutines.launch

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
    private val activityBehavior = PlaybackActivityBehavior()
    private val playbackDependencies: PlaybackActivityDependencies by lazy {
        applicationContext.requirePlaybackActivityDependencies()
    }
    private val playbackUiPersistence: PlaybackUiPersistence by lazy { playbackDependencies.playbackUiPersistence }
    private val playbackDeviceController: PlaybackDeviceController by lazy {
        playbackDependencies.playbackDeviceControllerFactory.create(
            context = this,
            window = window,
        )
    }
    private val sessionHost by lazy {
        PlaybackSessionHost(
            contentResolver = contentResolver,
            cacheDir = cacheDir,
            scope = lifecycleScope,
            dependencies = playbackDependencies,
            controllerContext = this,
        )
    }
    private var runtimeSettings by mutableStateOf(PlaybackRuntimeSettings())
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
        runtimeSettings = playbackDependencies.playbackRuntimeSettingsSource.current()
        activityBehavior.onRuntimeSettingsChanged(runtimeSettings)
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
                val transition = activityBehavior.onPictureInPictureModeChanged(info.isInPictureInPictureMode)
                composableIsPip = transition.isInPictureInPicture
                if (transition.shouldRegisterReceiver) {
                    registerPipReceiver()
                    sessionHost.currentPlayer?.addListener(pipPlayStateListener)
                } else {
                    sessionHost.currentPlayer?.removeListener(pipPlayStateListener)
                    unregisterPipReceiver()
                }
            }
        }

        observeRuntimeSettings()
        applyRememberedBrightnessIfNeeded()
        applyStatusBarVisibilityForOrientation()

        setContent {
            val hostState by sessionHost.state.collectAsState()
            val controller = hostState.controller
            if (controller == null) {
                PlaybackStartupScreen(
                    errorMessage = hostState.controllerErrorMessage,
                    onRetry = { sessionHost.ensureControllerReady(intent) },
                    onClose = { finish() },
                )
                return@setContent
            }
            PlayerScreen(
                model = PlaybackScreenModel(
                    uiState = hostState.uiState,
                    surfacePlayer = hostState.surfacePlayer,
                    trackUiState = hostState.trackUiState,
                    settings = runtimeSettings,
                    isInPip = composableIsPip,
                ),
                dependencies = PlaybackScreenDependencies(
                    controller = controller,
                    trackSelectionController = hostState.trackSelectionController,
                    playbackPersistence = playbackUiPersistence,
                    deviceController = playbackDeviceController,
                ),
                onVideoBoundsChanged = { videoRect = it },
                onBack = { finish() },
                onPip = { enterPipMode() },
                onBackground = {
                    activityBehavior.onBackgroundPlaybackRequested()
                    finish()
                },
                onRotate = { toggleOrientation() },
            )
        }

        sessionHost.ensureControllerReady(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runtimeSettings = playbackDependencies.playbackRuntimeSettingsSource.current()
        activityBehavior.onRuntimeSettingsChanged(runtimeSettings)
        sessionHost.onNewIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        activityBehavior.onStart()
        applyStatusBarVisibilityForOrientation()
        sessionHost.ensureControllerReady(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setPictureInPictureParams(buildPipParams())
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyStatusBarVisibilityForOrientation()
    }

    override fun onStop() {
        if (activityBehavior.shouldRememberBrightness()) {
            val brightness = window.attributes.screenBrightness
            if (brightness >= 0f) {
                playbackUiPersistence.saveRememberedBrightness(brightness)
            }
        }
        sessionHost.onStop(activityBehavior.shouldRetainSessionOnStop())
        super.onStop()
    }

    override fun onDestroy() {
        sessionHost.currentPlayer?.removeListener(pipPlayStateListener)
        unregisterPipReceiver()
        sessionHost.releaseAll()
        super.onDestroy()
    }

    private fun observeRuntimeSettings() {
        lifecycleScope.launch {
            playbackDependencies.playbackRuntimeSettingsSource.settings.collect { latest ->
                runtimeSettings = latest
                activityBehavior.onRuntimeSettingsChanged(latest)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    runCatching { setPictureInPictureParams(buildPipParams()) }
                }
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Enable background before enterPictureInPictureMode so that onStop() —
            // which fires synchronously after the transition — does not release resources.
            activityBehavior.onEnterPictureInPictureRequested()
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
        if (activityBehavior.shouldAutoEnterPictureInPictureOnUserLeave()) {
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

    private fun applyRememberedBrightnessIfNeeded() {
        if (!runtimeSettings.rememberBrightness) return
        val remembered = playbackUiPersistence.readRememberedBrightness() ?: return
        val attrs = window.attributes
        attrs.screenBrightness = remembered.coerceIn(0f, 1f)
        window.attributes = attrs
    }
}

@Composable
private fun PlaybackStartupScreen(
    errorMessage: String?,
    onRetry: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (errorMessage.isNullOrBlank()) {
            CircularProgressIndicator()
            return@Box
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = errorMessage,
                color = androidx.compose.ui.graphics.Color.White,
            )
            TextButton(onClick = onRetry) {
                Text(text = androidx.compose.ui.res.stringResource(id = R.string.retry))
            }
            TextButton(onClick = onClose) {
                Text(text = androidx.compose.ui.res.stringResource(id = R.string.close))
            }
        }
    }
}

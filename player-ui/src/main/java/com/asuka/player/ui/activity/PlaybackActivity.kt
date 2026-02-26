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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.asuka.player.core.PlaybackStateRestorer
import com.asuka.player.core.PlaybackStoreProvider
import com.asuka.player.core.IntentQueueReader
import com.asuka.player.core.QueueBuilder
import com.asuka.player.core.QueuePlanner
import com.asuka.player.core.PlaybackController
import com.asuka.player.core.SeekFallbackCopier
import com.asuka.player.core.impl.Media3PlaybackController
import com.asuka.player.core.service.PlaybackService
import com.asuka.player.ui.PlayerScreen
import com.asuka.player.ui.PlayerRuntimeSettings
import com.asuka.player.ui.R
import com.asuka.player.ui.controller.ControllerProvider
import com.asuka.player.ui.controller.ControllerBindings
import com.asuka.player.ui.controller.PlayerUiStateHolder
import com.asuka.player.ui.state.PlayerUiState
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Minimal playback Activity for M0.
 * Starts MediaController, sets a single media item, and renders minimal UI.
 */
class PlaybackActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SEEK_GESTURE_ENABLED = "player_seek_gesture_enabled"
        const val EXTRA_BRIGHTNESS_GESTURE_ENABLED = "player_brightness_gesture_enabled"
        const val EXTRA_VOLUME_GESTURE_ENABLED = "player_volume_gesture_enabled"
        const val EXTRA_ZOOM_GESTURE_ENABLED = "player_zoom_gesture_enabled"
        const val EXTRA_PAN_GESTURE_ENABLED = "player_pan_gesture_enabled"
        const val EXTRA_DOUBLE_TAP_GESTURE_ENABLED = "player_double_tap_gesture_enabled"
        const val EXTRA_DOUBLE_TAP_ACTION = "player_double_tap_action"
        const val EXTRA_LONG_PRESS_GESTURE_ENABLED = "player_long_press_gesture_enabled"
        const val EXTRA_SEEK_INCREMENT_SEC = "player_seek_increment_sec"
        const val EXTRA_SEEK_SENSITIVITY = "player_seek_sensitivity"
        const val EXTRA_LONG_PRESS_SPEED = "player_long_press_speed"
        const val EXTRA_CONTROLLER_TIMEOUT_SEC = "player_controller_timeout_sec"
        const val EXTRA_HIDE_BUTTON_BG = "player_hide_button_bg"
        const val EXTRA_RESUME_PLAYBACK = "player_resume_playback"
        const val EXTRA_DEFAULT_SPEED = "player_default_speed"
        const val EXTRA_AUTOPLAY = "player_autoplay"
        const val EXTRA_AUTO_PIP = "player_auto_pip"
        const val EXTRA_AUTO_BACKGROUND_PLAY = "player_auto_background_play"
        const val EXTRA_REMEMBER_BRIGHTNESS = "player_remember_brightness"
        const val EXTRA_REMEMBER_SELECTIONS = "player_remember_selections"

        private const val ACTION_PIP_CONTROL = "com.asuka.player.pip.CONTROL"
        private const val EXTRA_PIP_CONTROL = "pip_control"
        private const val PIP_CONTROL_PLAY = 1
        private const val PIP_CONTROL_PAUSE = 2
        private const val PIP_CONTROL_PREV = 3
        private const val PIP_CONTROL_NEXT = 4
        private const val RC_PIP_BASE = 100
    }

    private lateinit var controllerProvider: ControllerProvider
    private var mediaController: MediaController? = null
    private var playbackController: Media3PlaybackController? = null
    private var stateHolder: PlayerUiStateHolder? = null
    private var bindings: ControllerBindings? = null
    private var initJob: Job? = null
    private var seekFallbackJob: Job? = null
    private val seekFallbackAttemptedUris = mutableSetOf<String>()
    private val seekFallbackCopier by lazy { SeekFallbackCopier(contentResolver, cacheDir) }
    private val uiStateFlow = MutableStateFlow(PlayerUiState())
    private var composableController by mutableStateOf<PlaybackController?>(null)
    private var composablePlayer by mutableStateOf<Player?>(null)
    private var composableBindings by mutableStateOf<ControllerBindings?>(null)
    private var composableIsPip by mutableStateOf(false)
    private var videoRect: android.graphics.Rect? = null
    private var uiStateFeedJob: Job? = null
    private val backgroundPolicy = com.asuka.player.ui.controller.BackgroundPlaybackPolicy()
    private val playbackPrefs by lazy { getSharedPreferences("player_runtime", MODE_PRIVATE) }
    private lateinit var runtimeSettings: PlayerRuntimeSettings
    private var pipReceiverRegistered = false

    private val seekFallbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_READY) return
            val mc = mediaController ?: return
            val currentUri = mc.currentMediaItem?.localConfiguration?.uri ?: return
            if (mc.isCurrentMediaItemSeekable) return
            trySeekFallback(currentUri, "not_seekable_ready")
        }

        override fun onPlayerError(error: PlaybackException) {
            val mc = mediaController ?: return
            val currentUri = mc.currentMediaItem?.localConfiguration?.uri ?: intent?.data ?: return
            trySeekFallback(currentUri, "player_error_${error.errorCode}")
        }
    }

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
            val ctrl = playbackController ?: return
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
        PlaybackStoreProvider.init(this)
        PlaybackService.activityClass = PlaybackActivity::class.java
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
                if (info.isInPictureInPictureMode) {
                    backgroundPolicy.enableBackground()
                    registerPipReceiver()
                    mediaController?.addListener(pipPlayStateListener)
                } else {
                    mediaController?.removeListener(pipPlayStateListener)
                    unregisterPipReceiver()
                    if (!runtimeSettings.autoBackgroundPlay) {
                        backgroundPolicy.disableBackground()
                    }
                }
            }
        }

        applyRememberedBrightnessIfNeeded()
        if (runtimeSettings.autoBackgroundPlay) {
            backgroundPolicy.enableBackground()
        }
        controllerProvider = ControllerProvider(applicationContext)
        applyStatusBarVisibilityForOrientation()

        setContent {
            val uiState by uiStateFlow.collectAsState()
            val controller = composableController ?: return@setContent
            PlayerScreen(
                uiState = uiState,
                player = composablePlayer,
                controller = controller,
                bindings = composableBindings,
                store = PlaybackStoreProvider.store,
                settings = runtimeSettings,
                isInPip = composableIsPip,
                onVideoBoundsChanged = { videoRect = it },
                onBack = { finish() },
                onPip = { enterPipMode() },
                onBackground = { backgroundPolicy.enableBackground(); finish() },
                onRotate = { toggleOrientation() },
            )
        }

        ensureControllerReady()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runtimeSettings = readRuntimeSettings(intent)
        if (mediaController == null) {
            ensureControllerReady()
        } else {
            startSingleMedia(intent.data)
        }
    }

    override fun onStart() {
        super.onStart()
        if (runtimeSettings.autoBackgroundPlay) {
            backgroundPolicy.enableBackground()
        } else {
            backgroundPolicy.disableBackground()
        }
        applyStatusBarVisibilityForOrientation()
        ensureControllerReady()
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
        seekFallbackJob?.cancel()
        seekFallbackJob = null
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        stateHolder?.detach()
        stateHolder = null
        mediaController?.removeListener(seekFallbackListener)
        if (!backgroundPolicy.allowBackground) {
            initJob?.cancel()
            initJob = null
            bindings = null
            composableController = null
            composablePlayer = null
            composableBindings = null
            mediaController?.pause()
            controllerProvider.release()
            mediaController = null
            playbackController = null
        }
        super.onStop()
    }

    override fun onDestroy() {
        seekFallbackJob?.cancel()
        seekFallbackJob = null
        uiStateFeedJob?.cancel()
        uiStateFeedJob = null
        initJob?.cancel()
        initJob = null
        stateHolder?.detach()
        stateHolder = null
        bindings = null
        composableController = null
        composablePlayer = null
        composableBindings = null
        mediaController?.removeListener(seekFallbackListener)
        mediaController?.removeListener(pipPlayStateListener)
        unregisterPipReceiver()
        controllerProvider.release()
        mediaController = null
        playbackController = null
        super.onDestroy()
    }

    private fun ensureControllerReady() {
        mediaController?.let { mc ->
            mc.removeListener(seekFallbackListener)
            mc.addListener(seekFallbackListener)
            if (playbackController == null) {
                val ctrl = controllerProvider.asPlaybackController(mc)
                playbackController = ctrl
                composableController = ctrl
                composablePlayer = mc
            }
            if (bindings == null) {
                val b = ControllerBindings.from(mc)
                bindings = b
                composableBindings = b
            }
            if (stateHolder == null) {
                val holder = PlayerUiStateHolder(mc)
                holder.attach()
                holder.startProgressTicker(lifecycleScope)
                stateHolder = holder
                uiStateFeedJob?.cancel()
                uiStateFeedJob = lifecycleScope.launch { holder.state.collect { uiStateFlow.value = it } }
            }
            return
        }
        if (initJob?.isActive == true) return
        initJob = lifecycleScope.launch {
            try {
                val future = controllerProvider.buildAsync()
                val mc = future.await()
                mediaController = mc
                mc.addListener(seekFallbackListener)
                val ctrl = controllerProvider.asPlaybackController(mc)
                playbackController = ctrl
                composableController = ctrl
                composablePlayer = mc
                val b = ControllerBindings.from(mc)
                bindings = b
                composableBindings = b
                startSingleMedia(intent?.data)
                val holder = PlayerUiStateHolder(mc)
                holder.attach()
                holder.startProgressTicker(lifecycleScope)
                stateHolder = holder
                uiStateFeedJob?.cancel()
                uiStateFeedJob = lifecycleScope.launch { holder.state.collect { uiStateFlow.value = it } }
            } catch (_: CancellationException) {
                // Lifecycle moved on; connection will be retried on next start.
            } catch (error: Throwable) {
                Log.e("AsukaPlayback", "failed to connect controller", error)
            } finally {
                initJob = null
            }
        }
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Enable background before enterPictureInPictureMode so that onStop() —
            // which fires synchronously after the transition — does not release resources.
            backgroundPolicy.enableBackground()
            registerPipReceiver()
            enterPictureInPictureMode(buildPipParams())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildPipParams(): android.app.PictureInPictureParams {
        val builder = android.app.PictureInPictureParams.Builder()
        val mc = mediaController
        if (mc != null) {
            val w = mc.videoSize.width
            val h = mc.videoSize.height
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
        val mc = mediaController ?: return emptyList()

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
        val playPauseAction = if (mc.isPlaying) {
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

    private fun startSingleMedia(uri: Uri?) {
        val controller = mediaController ?: return
        val target = uri ?: return
        val extras = IntentQueueReader.read(intent).filter { it != target }
        val uris = QueuePlanner.plan(target, extras, PlaybackStoreProvider.history.items())
        val queue = QueueBuilder.build(uris, target)
        val mediaId = target.toString()
        val resume = PlaybackStateRestorer(PlaybackStoreProvider.store).read(mediaId)
        val resumePosition = if (runtimeSettings.resumePlayback) resume.positionMs else 0L
        val speedFromStore = if (runtimeSettings.rememberSelections) {
            PlaybackStoreProvider.store.loadPlaybackSpeed(mediaId)
        } else {
            null
        }
        controller.setMediaItems(queue.items, queue.startIndex, resumePosition)
        controller.setPlaybackSpeed(speedFromStore ?: runtimeSettings.defaultPlaybackSpeed)
        controller.prepare()
        if (runtimeSettings.rememberSelections) {
            bindings?.trackSelection?.let { selection ->
                val subtitleIndex = resume.subtitleTrackIndex
                when (subtitleIndex) {
                    null -> {}
                    -1 -> selection.disableSubtitles()
                    else -> {
                        val (g, t) = com.asuka.player.core.TrackIndexCodec.decode(subtitleIndex)
                        selection.setSubtitleTrack(g, t)
                    }
                }
                resume.audioTrackIndex?.let {
                    val (g, t) = com.asuka.player.core.TrackIndexCodec.decode(it)
                    selection.setAudioTrack(g, t)
                }
            }
        }
        if (runtimeSettings.autoplay) {
            controller.play()
        } else {
            controller.pause()
        }
    }

    private fun readRuntimeSettings(intent: Intent?): PlayerRuntimeSettings {
        return PlayerRuntimeSettings(
            seekGestureEnabled = intent?.getBooleanExtra(EXTRA_SEEK_GESTURE_ENABLED, true) ?: true,
            brightnessGestureEnabled = intent?.getBooleanExtra(EXTRA_BRIGHTNESS_GESTURE_ENABLED, true) ?: true,
            volumeGestureEnabled = intent?.getBooleanExtra(EXTRA_VOLUME_GESTURE_ENABLED, true) ?: true,
            zoomGestureEnabled = intent?.getBooleanExtra(EXTRA_ZOOM_GESTURE_ENABLED, true) ?: true,
            panGestureEnabled = intent?.getBooleanExtra(EXTRA_PAN_GESTURE_ENABLED, true) ?: true,
            doubleTapGestureEnabled = intent?.getBooleanExtra(EXTRA_DOUBLE_TAP_GESTURE_ENABLED, true) ?: true,
            doubleTapAction = when (intent?.getStringExtra(EXTRA_DOUBLE_TAP_ACTION)) {
                "toggle_play_pause" -> PlayerRuntimeSettings.DoubleTapAction.TogglePlayPause
                "both" -> PlayerRuntimeSettings.DoubleTapAction.Both
                else -> PlayerRuntimeSettings.DoubleTapAction.Seek
            },
            longPressGestureEnabled = intent?.getBooleanExtra(EXTRA_LONG_PRESS_GESTURE_ENABLED, true) ?: true,
            seekIncrementSec = intent?.getIntExtra(EXTRA_SEEK_INCREMENT_SEC, 10) ?: 10,
            seekSensitivity = intent?.getFloatExtra(EXTRA_SEEK_SENSITIVITY, 1.0f) ?: 1.0f,
            longPressSpeed = intent?.getFloatExtra(EXTRA_LONG_PRESS_SPEED, 2.0f) ?: 2.0f,
            controllerTimeoutSec = intent?.getIntExtra(EXTRA_CONTROLLER_TIMEOUT_SEC, 3) ?: 3,
            hideButtonsBackground = intent?.getBooleanExtra(EXTRA_HIDE_BUTTON_BG, true) ?: true,
            resumePlayback = intent?.getBooleanExtra(EXTRA_RESUME_PLAYBACK, true) ?: true,
            defaultPlaybackSpeed = intent?.getFloatExtra(EXTRA_DEFAULT_SPEED, 1.0f) ?: 1.0f,
            autoplay = intent?.getBooleanExtra(EXTRA_AUTOPLAY, true) ?: true,
            autoPip = intent?.getBooleanExtra(EXTRA_AUTO_PIP, true) ?: true,
            autoBackgroundPlay = intent?.getBooleanExtra(EXTRA_AUTO_BACKGROUND_PLAY, false) ?: false,
            rememberBrightness = intent?.getBooleanExtra(EXTRA_REMEMBER_BRIGHTNESS, false) ?: false,
            rememberSelections = intent?.getBooleanExtra(EXTRA_REMEMBER_SELECTIONS, true) ?: true,
        )
    }

    private fun applyRememberedBrightnessIfNeeded() {
        if (!runtimeSettings.rememberBrightness) return
        val remembered = playbackPrefs.getFloat("last_brightness", -1f)
        if (remembered < 0f) return
        val attrs = window.attributes
        attrs.screenBrightness = remembered.coerceIn(0f, 1f)
        window.attributes = attrs
    }

    private fun trySeekFallback(currentUri: Uri, reason: String) {
        if (currentUri.scheme != "content") return
        val key = currentUri.toString()
        if (!seekFallbackAttemptedUris.add(key)) return
        if (seekFallbackJob?.isActive == true) return
        seekFallbackJob = lifecycleScope.launch {
            val copiedUri = withContext(Dispatchers.IO) {
                seekFallbackCopier.copy(currentUri, checkSize = true)
            } ?: return@launch
            Log.i("AsukaSeekFallback", "fallback[$reason] from $currentUri to $copiedUri")
            setIntent(Intent(intent).apply { data = copiedUri })
            startSingleMedia(copiedUri)
        }
    }
}

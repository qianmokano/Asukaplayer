package com.asuka.player.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDependenciesProvider
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackUiPersistence
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
    private val playbackDependencies: PlaybackActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        (application as? PlaybackDependenciesProvider)?.playbackActivityDependencies
            ?: error("Application does not provide PlaybackActivityDependencies.")
    }

    private var composableIsPip by mutableStateOf(false)
    private val activityBehavior = PlaybackActivityBehavior()
    private val playbackUiPersistence: PlaybackUiPersistence by lazy { playbackDependencies.playbackUiPersistence }
    private val playbackDeviceController: PlaybackDeviceController by lazy {
        playbackDependencies.playbackDeviceControllerFactory.create(this)
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
    private val windowChromeController by lazy {
        PlaybackWindowChromeController(
            window = window,
            playbackUiPersistence = playbackUiPersistence,
        )
    }
    private val pictureInPictureController by lazy {
        PlaybackPictureInPictureController(
            activity = this,
            currentPlayerProvider = { sessionHost.currentPlayer },
            currentControllerProvider = { sessionHost.currentController },
        )
    }
    private var runtimeSettings by mutableStateOf(PlaybackRuntimeSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runtimeSettings = playbackDependencies.playbackRuntimeSettingsSource.current()
        activityBehavior.onRuntimeSettingsChanged(runtimeSettings)
        pictureInPictureController.updateRuntimeSettings(runtimeSettings)
        windowChromeController.applyBaseWindowStyle()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addOnPictureInPictureModeChangedListener { info ->
                val transition = activityBehavior.onPictureInPictureModeChanged(info.isInPictureInPictureMode)
                composableIsPip = transition.isInPictureInPicture
                pictureInPictureController.onPictureInPictureModeChanged(transition)
            }
        }

        observeRuntimeSettings()
        windowChromeController.applyRememberedBrightnessIfNeeded(runtimeSettings)
        windowChromeController.applySystemBarsForOrientation(resources.configuration.orientation)

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
                onVideoBoundsChanged = { pictureInPictureController.updateVideoBounds(it) },
                onBack = { finish() },
                onPip = {
                    pictureInPictureController.enterPictureInPictureMode(
                        beforeEnter = { activityBehavior.onEnterPictureInPictureRequested() },
                    )
                },
                onBackground = {
                    activityBehavior.onBackgroundPlaybackRequested()
                    finish()
                },
                onRotate = {
                    requestedOrientation = windowChromeController.toggleOrientation(
                        requestedOrientation = requestedOrientation,
                        currentOrientation = resources.configuration.orientation,
                    )
                },
            )
        }

        sessionHost.ensureControllerReady(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        runtimeSettings = playbackDependencies.playbackRuntimeSettingsSource.current()
        activityBehavior.onRuntimeSettingsChanged(runtimeSettings)
        pictureInPictureController.updateRuntimeSettings(runtimeSettings)
        sessionHost.onNewIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        activityBehavior.onStart()
        windowChromeController.applySystemBarsForOrientation(resources.configuration.orientation)
        sessionHost.ensureControllerReady(intent)
        pictureInPictureController.updatePictureInPictureParamsIfSupported()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowChromeController.applySystemBarsForOrientation(newConfig.orientation)
    }

    override fun onStop() {
        windowChromeController.saveRememberedBrightnessIfNeeded(runtimeSettings)
        sessionHost.onStop(activityBehavior.shouldRetainSessionOnStop())
        super.onStop()
    }

    override fun onDestroy() {
        pictureInPictureController.release()
        sessionHost.releaseAll()
        super.onDestroy()
    }

    private fun observeRuntimeSettings() {
        lifecycleScope.launch {
            playbackDependencies.playbackRuntimeSettingsSource.settings.collect { latest ->
                runtimeSettings = latest
                activityBehavior.onRuntimeSettingsChanged(latest)
                pictureInPictureController.updateRuntimeSettings(latest)
                pictureInPictureController.updatePictureInPictureParamsIfSupported()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (activityBehavior.shouldAutoEnterPictureInPictureOnUserLeave()) {
            pictureInPictureController.enterPictureInPictureMode(
                beforeEnter = { activityBehavior.onEnterPictureInPictureRequested() },
            )
        }
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

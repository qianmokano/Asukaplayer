package com.asuka.player.renderer.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDependenciesProvider
import com.asuka.player.renderer.components.Media3PlaybackSurfaceRenderer
import com.asuka.player.ui.PlaybackScreenDependencies
import com.asuka.player.ui.PlaybackScreenModel
import com.asuka.player.ui.PlayerScreen
import com.asuka.player.ui.R as PlayerUiR
import com.asuka.player.ui.theme.PlayerUiTokens

class PlaybackActivity : ComponentActivity() {
    private val playbackDependencies: PlaybackActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        (application as? PlaybackDependenciesProvider)?.playbackActivityDependencies
            ?: error("Application does not provide PlaybackActivityDependencies.")
    }

    private val viewModel: PlaybackViewModel by viewModels {
        PlaybackViewModel.Factory(application, playbackDependencies)
    }

    private val playbackUiPersistence: PlaybackUiPersistence by lazy(LazyThreadSafetyMode.NONE) {
        playbackDependencies.playbackUiPersistence
    }

    private val windowChromeController by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackWindowChromeController(
            window = window,
            playbackUiPersistence = playbackUiPersistence,
        )
    }
    private val pictureInPictureController by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackPictureInPictureController(
            activity = this,
            currentPlayerProvider = { viewModel.sessionHost.currentPlayer },
            currentControllerProvider = { viewModel.sessionHost.currentController },
        )
    }
    private val playbackDeviceController by lazy(LazyThreadSafetyMode.NONE) {
        playbackDependencies.playbackDeviceControllerFactory.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.applyRuntimeSettings(playbackDependencies.playbackRuntimeSettingsSource.current())
        pictureInPictureController.updateRuntimeSettings(viewModel.state.value.runtimeSettings)
        windowChromeController.applyBaseWindowStyle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addOnPictureInPictureModeChangedListener { info ->
                val transition = viewModel.activityBehavior.onPictureInPictureModeChanged(info.isInPictureInPictureMode)
                viewModel.updatePictureInPicture(transition.isInPictureInPicture)
                pictureInPictureController.onPictureInPictureModeChanged(transition)
            }
        }
        windowChromeController.applyRememberedBrightnessIfNeeded(viewModel.state.value.runtimeSettings)
        windowChromeController.applySystemBarsForOrientation(resources.configuration.orientation)
        viewModel.sessionHost.ensureControllerReady(intent)

        val playbackThemeProvider = application as? PlaybackThemeProvider
        setContent {
            if (playbackThemeProvider != null) {
                playbackThemeProvider.ProvidePlaybackTheme {
                    PlaybackActivityContent()
                }
            } else {
                PlaybackActivityContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.applyRuntimeSettings(playbackDependencies.playbackRuntimeSettingsSource.current())
        pictureInPictureController.updateRuntimeSettings(viewModel.state.value.runtimeSettings)
        viewModel.sessionHost.onNewIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.activityBehavior.onStart()
        pictureInPictureController.updateRuntimeSettings(viewModel.state.value.runtimeSettings)
        windowChromeController.applySystemBarsForOrientation(resources.configuration.orientation)
        viewModel.sessionHost.ensureControllerReady(intent)
        pictureInPictureController.updatePictureInPictureParamsIfSupported()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        windowChromeController.applySystemBarsForOrientation(newConfig.orientation)
    }

    override fun onStop() {
        windowChromeController.saveRememberedBrightnessIfNeeded(viewModel.state.value.runtimeSettings)
        viewModel.sessionHost.onStop(viewModel.activityBehavior.shouldRetainSessionOnStop())
        super.onStop()
    }

    override fun onDestroy() {
        pictureInPictureController.release()
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.activityBehavior.shouldAutoEnterPictureInPictureOnUserLeave()) {
            enterPip()
        }
    }

    private fun enterPip() {
        pictureInPictureController.enterPictureInPictureMode(
            beforeEnter = { viewModel.activityBehavior.onEnterPictureInPictureRequested() },
        )
    }

    @Composable
    private fun PlaybackActivityContent() {
        val state by viewModel.state.collectAsState()
        val controller = state.controller
        if (controller == null) {
            PlaybackStartupScreen(
                errorMessage = state.controllerErrorMessage,
                onRetry = { viewModel.sessionHost.ensureControllerReady(intent) },
                onClose = { finish() },
            )
            return
        }
        val isControllerConnected by controller.isConnected.collectAsState()
        PlayerScreen(
            model = PlaybackScreenModel(
                uiState = state.uiState,
                surfaceState = state.surfaceState,
                trackUiState = state.trackUiState,
                settings = state.runtimeSettings,
                isInPip = state.isInPictureInPicture,
                isControllerConnected = isControllerConnected,
                isPersistenceDegraded = state.isPersistenceDegraded,
            ),
            dependencies = PlaybackScreenDependencies(
                controller = controller,
                trackSelectionController = state.trackSelectionController,
                playbackPersistence = playbackDependencies.playbackUiPersistence,
                previewFrameProvider = playbackDependencies.playbackPreviewFrameProvider,
                deviceController = playbackDeviceController,
                surfaceRenderer = Media3PlaybackSurfaceRenderer,
            ),
            onVideoBoundsChanged = { bounds ->
                pictureInPictureController.updateVideoBounds(bounds)
            },
            onBack = { finish() },
            onPip = ::enterPip,
            onBackground = {
                viewModel.activityBehavior.onBackgroundPlaybackRequested()
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
            CircularProgressIndicator(
                color = PlayerUiTokens.loadingIndicatorColor(),
            )
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
                Text(text = androidx.compose.ui.res.stringResource(id = PlayerUiR.string.retry))
            }
            TextButton(onClick = onClose) {
                Text(text = androidx.compose.ui.res.stringResource(id = PlayerUiR.string.close))
            }
        }
    }
}

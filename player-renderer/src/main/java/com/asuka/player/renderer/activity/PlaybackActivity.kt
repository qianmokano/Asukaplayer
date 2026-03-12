package com.asuka.player.renderer.activity

import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDependenciesProvider
import com.asuka.player.renderer.components.Media3PlaybackSurfaceRenderer
import com.asuka.player.ui.PlaybackScreenDependencies
import com.asuka.player.ui.PlaybackScreenModel
import com.asuka.player.ui.PlayerScreen
import com.asuka.player.ui.R

/**
 * Minimal playback Activity for M0.
 * Starts MediaController, sets a single media item, and renders minimal UI.
 */
class PlaybackActivity : ComponentActivity() {
    private val playbackDependencies: PlaybackActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        (application as? PlaybackDependenciesProvider)?.playbackActivityDependencies
            ?: error("Application does not provide PlaybackActivityDependencies.")
    }

    private val playbackSession by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackActivitySession(
            activity = this,
            dependencies = playbackDependencies,
            scope = lifecycleScope,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playbackSession.onCreate(intent)
        val playbackThemeProvider = application as? PlaybackThemeProvider

        setContent {
            if (playbackThemeProvider != null) {
                playbackThemeProvider.ProvidePlaybackTheme {
                    PlaybackActivityContent(
                        playbackSession = playbackSession,
                        playbackDependencies = playbackDependencies,
                        onClose = { finish() },
                        onRotate = {
                            requestedOrientation = playbackSession.toggleOrientation(
                                requestedOrientation = requestedOrientation,
                                currentOrientation = resources.configuration.orientation,
                            )
                        },
                    )
                }
            } else {
                PlaybackActivityContent(
                    playbackSession = playbackSession,
                    playbackDependencies = playbackDependencies,
                    onClose = { finish() },
                    onRotate = {
                        requestedOrientation = playbackSession.toggleOrientation(
                            requestedOrientation = requestedOrientation,
                            currentOrientation = resources.configuration.orientation,
                        )
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        playbackSession.onNewIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        playbackSession.onStart()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        playbackSession.onConfigurationChanged(newConfig)
    }

    override fun onStop() {
        playbackSession.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        playbackSession.onDestroy()
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        playbackSession.onUserLeaveHint()
    }
}

@Composable
private fun PlaybackActivityContent(
    playbackSession: PlaybackActivitySession,
    playbackDependencies: PlaybackActivityDependencies,
    onClose: () -> Unit,
    onRotate: () -> Unit,
) {
    val sessionState by playbackSession.uiState.collectAsState()
    val hostState = sessionState.hostState
    val controller = hostState.controller
    if (controller == null) {
        PlaybackStartupScreen(
            errorMessage = hostState.controllerErrorMessage,
            onRetry = playbackSession::retryCurrentIntent,
            onClose = onClose,
        )
        return
    }
    PlayerScreen(
        model = PlaybackScreenModel(
            uiState = hostState.uiState,
            surfaceState = hostState.surfaceState,
            trackUiState = hostState.trackUiState,
            settings = sessionState.runtimeSettings,
            isInPip = sessionState.isInPictureInPicture,
        ),
        dependencies = PlaybackScreenDependencies(
            controller = controller,
            trackSelectionController = hostState.trackSelectionController,
            playbackPersistence = playbackDependencies.playbackUiPersistence,
            deviceController = playbackSession.playbackDeviceController,
            surfaceRenderer = Media3PlaybackSurfaceRenderer,
        ),
        onVideoBoundsChanged = playbackSession::updateVideoBounds,
        onBack = onClose,
        onPip = playbackSession::enterPictureInPictureMode,
        onBackground = {
            playbackSession.requestBackgroundPlayback()
            onClose()
        },
        onRotate = onRotate,
    )
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

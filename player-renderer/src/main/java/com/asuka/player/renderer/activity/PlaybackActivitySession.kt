package com.asuka.player.renderer.activity

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleCoroutineScope
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.platform.PlaybackActivityDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PlaybackActivityUiState(
    val hostState: PlaybackHostState = PlaybackHostState(),
    val runtimeSettings: PlaybackRuntimeSettings = PlaybackRuntimeSettings(),
    val isInPictureInPicture: Boolean = false,
)

internal class PlaybackActivitySession(
    private val activity: ComponentActivity,
    private val dependencies: PlaybackActivityDependencies,
    private val scope: LifecycleCoroutineScope,
) {
    private val activityBehavior = PlaybackActivityBehavior()
    private val playbackUiPersistence: PlaybackUiPersistence by lazy(LazyThreadSafetyMode.NONE) {
        dependencies.playbackUiPersistence
    }
    val playbackDeviceController: PlaybackDeviceController by lazy(LazyThreadSafetyMode.NONE) {
        dependencies.playbackDeviceControllerFactory.create(activity)
    }

    private val sessionHost by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackSessionHost(
            contentResolver = activity.contentResolver,
            cacheDir = activity.cacheDir,
            scope = scope,
            dependencies = dependencies,
            controllerContext = activity,
        )
    }
    private val windowChromeController by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackWindowChromeController(
            window = activity.window,
            playbackUiPersistence = playbackUiPersistence,
        )
    }
    private val pictureInPictureController by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackPictureInPictureController(
            activity = activity,
            currentPlayerProvider = { sessionHost.currentPlayer },
            currentControllerProvider = { sessionHost.currentController },
        )
    }

    private val _uiState = MutableStateFlow(
        PlaybackActivityUiState(
            runtimeSettings = dependencies.playbackRuntimeSettingsSource.current(),
        ),
    )
    val uiState: StateFlow<PlaybackActivityUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            sessionHost.state.collect { hostState ->
                _uiState.update { current -> current.copy(hostState = hostState) }
            }
        }
        scope.launch {
            dependencies.playbackRuntimeSettingsSource.settings.collect(::updateRuntimeSettings)
        }
    }

    fun onCreate(initialIntent: Intent?) {
        updateRuntimeSettings(dependencies.playbackRuntimeSettingsSource.current())
        windowChromeController.applyBaseWindowStyle()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.addOnPictureInPictureModeChangedListener { info ->
                val transition = activityBehavior.onPictureInPictureModeChanged(info.isInPictureInPictureMode)
                _uiState.update { current ->
                    current.copy(isInPictureInPicture = transition.isInPictureInPicture)
                }
                pictureInPictureController.onPictureInPictureModeChanged(transition)
            }
        }
        windowChromeController.applyRememberedBrightnessIfNeeded(uiState.value.runtimeSettings)
        windowChromeController.applySystemBarsForOrientation(activity.resources.configuration.orientation)
        sessionHost.ensureControllerReady(initialIntent)
    }

    fun onNewIntent(intent: Intent) {
        updateRuntimeSettings(dependencies.playbackRuntimeSettingsSource.current())
        sessionHost.onNewIntent(intent)
    }

    fun onStart() {
        activityBehavior.onStart()
        windowChromeController.applySystemBarsForOrientation(activity.resources.configuration.orientation)
        sessionHost.ensureControllerReady(activity.intent)
        pictureInPictureController.updatePictureInPictureParamsIfSupported()
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        windowChromeController.applySystemBarsForOrientation(newConfig.orientation)
    }

    fun onStop() {
        windowChromeController.saveRememberedBrightnessIfNeeded(uiState.value.runtimeSettings)
        sessionHost.onStop(activityBehavior.shouldRetainSessionOnStop())
    }

    fun onDestroy() {
        pictureInPictureController.release()
        sessionHost.releaseAll()
    }

    fun onUserLeaveHint() {
        if (activityBehavior.shouldAutoEnterPictureInPictureOnUserLeave()) {
            enterPictureInPictureMode()
        }
    }

    fun retryCurrentIntent() {
        sessionHost.ensureControllerReady(activity.intent)
    }

    fun updateVideoBounds(bounds: Rect) {
        pictureInPictureController.updateVideoBounds(bounds)
    }

    fun enterPictureInPictureMode() {
        pictureInPictureController.enterPictureInPictureMode(
            beforeEnter = { activityBehavior.onEnterPictureInPictureRequested() },
        )
    }

    fun requestBackgroundPlayback() {
        activityBehavior.onBackgroundPlaybackRequested()
    }

    fun toggleOrientation(
        requestedOrientation: Int,
        currentOrientation: Int,
    ): Int {
        return windowChromeController.toggleOrientation(
            requestedOrientation = requestedOrientation,
            currentOrientation = currentOrientation,
        )
    }

    private fun updateRuntimeSettings(settings: PlaybackRuntimeSettings) {
        _uiState.update { current -> current.copy(runtimeSettings = settings) }
        activityBehavior.onRuntimeSettingsChanged(settings)
        pictureInPictureController.updateRuntimeSettings(settings)
        pictureInPictureController.updatePictureInPictureParamsIfSupported()
    }
}

package com.asuka.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.asuka.player.ui.components.OverlayType
import com.asuka.player.ui.controller.GestureConfig
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.OverlayActions
import com.asuka.player.ui.controller.OverlayTrackActions
import com.asuka.player.ui.controller.PointerGestureDetector
import com.asuka.player.ui.controller.QueueActions
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.LongPressSpeedState
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.TapFeedbackState
import com.asuka.player.ui.state.PlayerUiState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.StateFlow

@Composable
fun PlayerScreen(
    model: PlaybackScreenModel,
    uiStateFlow: StateFlow<PlayerUiState>,
    dependencies: PlaybackScreenDependencies,
    onVideoBoundsChanged: ((android.graphics.Rect) -> Unit)? = null,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onBackground: () -> Unit,
    onRotate: () -> Unit = {},
) {
    val uiState by uiStateFlow.collectAsState()
    val trackUiState = model.trackUiState
    val settings = model.settings
    val controller = dependencies.controller
    val playbackPersistence = dependencies.playbackPersistence
    val deviceController = dependencies.deviceController
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val controlsState = remember(settings.controllerTimeoutSec) {
        ControlsState(scope = scope, autoHideDelay = settings.controllerTimeoutSec.coerceIn(1, 60).seconds)
    }
    val volumeBrightnessState = remember { VolumeBrightnessState() }
    val seekState = remember { SeekState() }
    val zoomState = remember { ZoomState() }
    val scaleState = remember { ScaleState() }
    val tapFeedbackState = remember { TapFeedbackState() }
    val longPressSpeedState = remember { LongPressSpeedState() }
    var overlayType by remember { mutableStateOf<OverlayType?>(null) }
    var dismissedErrorMessage by remember { mutableStateOf<String?>(null) }
    var lockedOverlayVisible by remember { mutableStateOf(false) }

    val positionMsState = rememberUpdatedState(uiState.positionMs)
    val durationMsState = rememberUpdatedState(uiState.durationMs)
    val mediaIdState = rememberUpdatedState(trackUiState.currentMediaId)
    val mediaUriState = rememberUpdatedState(trackUiState.currentMediaUri)
    val playbackSpeedState = rememberUpdatedState(trackUiState.currentSpeed)

    val overlayActions = remember(controller) { OverlayActions(controller = controller) }
    val trackActions = remember(dependencies.trackSelectionController) {
        dependencies.trackSelectionController?.let(::OverlayTrackActions)
    }
    val queueActions = remember(controller) { QueueActions(controller) }
    val pointerDetector = remember(settings.seekSensitivity, deviceController) {
        PointerGestureDetector(
            seekSensitivity = settings.seekSensitivity.coerceIn(0.1f, 2.0f),
            positionProvider = { positionMsState.value },
            durationProvider = { durationMsState.value },
            volumeProvider = deviceController::currentVolumePercent,
            brightnessProvider = deviceController::currentBrightnessPercent,
        )
    }
    val gestureCoordinator = remember(
        controller,
        controlsState,
        volumeBrightnessState,
        seekState,
        zoomState,
        settings,
        playbackPersistence,
        deviceController,
    ) {
        GestureCoordinator(
            controller = controller,
            controlsState = controlsState,
            volumeBrightnessState = volumeBrightnessState,
            seekState = seekState,
            zoomState = zoomState,
            onZoomEnd = createPersistZoomHandler(
                mediaIdState = mediaIdState,
                playbackPersistence = playbackPersistence,
                scope = scope,
            ),
            playbackSpeedProvider = { playbackSpeedState.value },
            onDoubleTapFeedback = { delta -> tapFeedbackState.show(delta) },
            onLongPressFeedback = createLongPressFeedbackHandler(
                haptic = haptic,
                longPressSpeedState = longPressSpeedState,
            ),
            onVolumeChanged = deviceController::setVolumePercent,
            onBrightnessChanged = deviceController::setBrightnessPercent,
            config = GestureConfig(
                enableSeekGesture = settings.seekGestureEnabled,
                enableBrightnessGesture = settings.brightnessGestureEnabled,
                enableVolumeGesture = settings.volumeGestureEnabled,
                enableZoomGesture = settings.zoomGestureEnabled,
                enablePanGesture = settings.panGestureEnabled,
                enableDoubleTapGesture = settings.doubleTapGestureEnabled,
                doubleTapAction = settings.doubleTapAction,
                enableLongPressGesture = settings.longPressGestureEnabled,
                doubleTapSeekDeltaMs = settings.seekIncrementSec.coerceIn(1, 60) * 1000L,
                longPressSpeed = settings.longPressSpeed.coerceIn(0.2f, 4.0f),
            ),
        )
    }

    PlayerScreenEffects(
        mediaIdState = mediaIdState,
        uiErrorMessage = uiState.errorMessage,
        uiIsPlaying = uiState.isPlaying,
        controlsState = controlsState,
        overlayType = overlayType,
        dismissedErrorMessage = dismissedErrorMessage,
        onDismissedErrorReset = { dismissedErrorMessage = null },
        seekState = seekState,
        tapFeedbackState = tapFeedbackState,
        longPressSpeedState = longPressSpeedState,
        zoomState = zoomState,
        playbackPersistence = playbackPersistence,
        scope = scope,
        lockedOverlayVisible = lockedOverlayVisible,
        onLockedOverlayVisibleChange = { lockedOverlayVisible = it },
    )

    val openOverlay: (OverlayType) -> Unit = { type ->
        overlayType = type
        controlsState.hide()
    }
    val displayedPositionMs = if (seekState.seeking) seekState.previewPositionMs else uiState.positionMs
    val controlsVisible = controlsState.visible && !controlsState.locked
    val visibleError = uiState.errorMessage?.takeIf { it != dismissedErrorMessage }
    val landscapeCutoutPadding = rememberLandscapeCutoutPadding()
    val videoBoundsModifier = Modifier.onGloballyPositioned { coords ->
        if (onVideoBoundsChanged == null) return@onGloballyPositioned
        val bounds = coords.boundsInWindow()
        onVideoBoundsChanged(
            android.graphics.Rect(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                bounds.bottom.toInt(),
            ),
        )
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PlayerScreenGestureShell(
            surfaceState = model.surfaceState,
            surfaceRenderer = dependencies.surfaceRenderer,
            zoomState = zoomState,
            scaleState = scaleState,
            videoBoundsModifier = videoBoundsModifier,
            controlsState = controlsState,
            overlayType = overlayType,
            visibleError = visibleError,
            isInPip = model.isInPip,
            gestureCoordinator = gestureCoordinator,
            pointerDetector = pointerDetector,
            landscapeCutoutPadding = landscapeCutoutPadding,
            controlsVisible = controlsVisible,
            lockedOverlayVisible = lockedOverlayVisible,
            onLockedOverlayTap = { lockedOverlayVisible = !lockedOverlayVisible },
            onUnlock = {
                controlsState.unlock()
                lockedOverlayVisible = false
            },
            seekState = seekState,
            mediaUri = mediaUriState.value,
            durationMs = uiState.durationMs,
            previewFrameProvider = dependencies.previewFrameProvider,
            tapFeedbackState = tapFeedbackState,
            volumeBrightnessState = volumeBrightnessState,
            longPressSpeedState = longPressSpeedState,
        )
        PlayerScreenLayoutShell(
            controlsVisible = controlsVisible,
            isInPip = model.isInPip,
            settings = settings,
            uiState = uiState,
            controller = controller,
            landscapeCutoutPadding = landscapeCutoutPadding,
            displayedPositionMs = displayedPositionMs,
            queueActions = queueActions,
            onBack = onBack,
            onPip = onPip,
            onBackground = onBackground,
            onOpenOverlay = openOverlay,
            onRotate = onRotate,
            controlsState = controlsState,
        )
        PlayerScreenOverlayShell(
            overlayType = overlayType,
            overlayActions = overlayActions,
            scaleState = scaleState,
            trackActions = trackActions,
            trackUiState = trackUiState,
            currentRepeatMode = uiState.repeatMode,
            shuffleEnabled = uiState.shuffleEnabled,
            onOpenType = { overlayType = it },
            onDismissOverlay = { overlayType = null },
            visibleError = visibleError,
            onRetry = {
                dismissedErrorMessage = null
                controller.prepare()
                controller.play()
            },
            onNext = { queueActions.next() },
            onDismissError = { dismissedErrorMessage = uiState.errorMessage },
        )
        if (!model.isControllerConnected && !model.isInPip) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    Text(
                        text = "Reconnecting...",
                        color = Color.White,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

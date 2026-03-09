package com.asuka.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.components.BottomBar
import com.asuka.player.ui.components.DoubleTapIndicator
import com.asuka.player.ui.components.ErrorOverlay
import com.asuka.player.ui.components.GestureLayer
import com.asuka.player.ui.components.LockedControlsOverlay
import com.asuka.player.ui.components.LongPressSpeedIndicator
import com.asuka.player.ui.components.MiddleControls
import com.asuka.player.ui.components.OverlayPanel
import com.asuka.player.ui.components.OverlayType
import com.asuka.player.ui.components.SeekIndicator
import com.asuka.player.ui.components.TopBar
import com.asuka.player.ui.components.VerticalAdjustIndicator
import com.asuka.player.ui.components.VideoSurfaceWithTransform
import com.asuka.player.ui.components.ZoomIndicator
import com.asuka.player.ui.controller.GestureConfig
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.OverlayActions
import com.asuka.player.ui.controller.OverlayTrackActions
import com.asuka.player.ui.controller.PointerGestureDetector
import com.asuka.player.ui.controller.QueueActions
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.controller.UiActions
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.LongPressSpeedState
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.state.TapFeedbackState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import com.asuka.player.ui.theme.PlayerUiTokens
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * M1 minimal UI: title + controls + gesture layer.
 */
@Composable
fun PlayerScreen(
    model: PlaybackScreenModel,
    dependencies: PlaybackScreenDependencies,
    onVideoBoundsChanged: ((android.graphics.Rect) -> Unit)? = null,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onBackground: () -> Unit,
    onRotate: () -> Unit = {},
) {
    val uiState = model.uiState
    val surfacePlayer = model.surfacePlayer
    val trackUiState = model.trackUiState
    val settings = model.settings
    val isInPip = model.isInPip
    val controller = dependencies.controller
    val playbackPersistence = dependencies.playbackPersistence
    val deviceController = dependencies.deviceController
    val scope = rememberCoroutineScope()
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
    val openOverlay: (OverlayType) -> Unit = { type ->
        overlayType = type
        controlsState.hide()
    }
    val positionMsState = rememberUpdatedState(uiState.positionMs)
    val durationMsState = rememberUpdatedState(uiState.durationMs)
    val mediaIdState = rememberUpdatedState(trackUiState.currentMediaId)
    val playbackSpeedState = rememberUpdatedState(trackUiState.currentSpeed)
    val overlayActions = remember(controller, playbackPersistence) {
        OverlayActions(
            controller = controller,
            playbackPersistence = playbackPersistence,
            mediaIdProvider = { mediaIdState.value },
        )
    }
    val trackActions = remember(dependencies.trackSelectionController, playbackPersistence) {
        dependencies.trackSelectionController?.let {
            OverlayTrackActions(
                trackSelectionController = it,
                playbackPersistence = playbackPersistence,
                mediaIdProvider = { mediaIdState.value },
            )
        }
    }
    val queueActions = remember(controller) { QueueActions(controller) }
    LaunchedEffect(mediaIdState.value) {
        val mediaId = mediaIdState.value ?: return@LaunchedEffect
        val zoom = withContext(Dispatchers.IO) {
            playbackPersistence.readZoom(mediaId)
        } ?: 1f
        zoomState.setTransform(zoom, Offset.Zero, pinching = false)
    }
    val pointerDetector = remember(settings.seekSensitivity, deviceController) {
        PointerGestureDetector(
            seekSensitivity = settings.seekSensitivity.coerceIn(0.1f, 2.0f),
            positionProvider = { positionMsState.value },
            durationProvider = { durationMsState.value },
            volumeProvider = deviceController::currentVolumePercent,
            brightnessProvider = deviceController::currentBrightnessPercent,
        )
    }
    val uiActions = remember(controller) { UiActions(controller) }
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
            onZoomEnd = { zoom ->
                mediaIdState.value?.let { id -> playbackPersistence.saveZoom(id, zoom) }
            },
            playbackSpeedProvider = { playbackSpeedState.value },
            onDoubleTapFeedback = { delta -> tapFeedbackState.show(delta) },
            onLongPressFeedback = { active, speed ->
                if (active) longPressSpeedState.start(speed) else longPressSpeedState.end()
            },
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
    LaunchedEffect(uiState.isPlaying) {
        if (uiState.isPlaying && !controlsState.locked) {
            controlsState.show()
        }
    }
    LaunchedEffect(overlayType) {
        if (overlayType == null && !controlsState.locked) {
            controlsState.show()
        }
    }
    LaunchedEffect(tapFeedbackState.eventId) {
        if (tapFeedbackState.visible) {
            delay(PlayerUiTokens.Motion.feedbackMs)
            tapFeedbackState.hide()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != dismissedErrorMessage) {
            dismissedErrorMessage = null
        }
    }
    LaunchedEffect(controlsState.locked) {
        lockedOverlayVisible = controlsState.locked
    }
    LaunchedEffect(controlsState.locked, lockedOverlayVisible) {
        if (controlsState.locked && lockedOverlayVisible) {
            delay(3.seconds)
            lockedOverlayVisible = false
        }
    }
    val controlsVisible = controlsState.visible && !controlsState.locked
    val visibleError = uiState.errorMessage?.takeIf { it != dismissedErrorMessage }
    val videoBoundsModifier = remember(onVideoBoundsChanged) {
        if (onVideoBoundsChanged != null) {
            Modifier.onGloballyPositioned { coords ->
                val b = coords.boundsInWindow()
                onVideoBoundsChanged(
                    android.graphics.Rect(
                        b.left.toInt(),
                        b.top.toInt(),
                        b.right.toInt(),
                        b.bottom.toInt(),
                    ),
                )
            }
        } else {
            Modifier
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        surfacePlayer?.let { player ->
            VideoSurfaceWithTransform(
                modifier = videoBoundsModifier,
                player = player,
                zoomState = zoomState,
                scaleState = scaleState,
            )
        }
        if (!controlsState.locked && overlayType == null && visibleError == null && !isInPip) {
            GestureLayer(coordinator = gestureCoordinator, pointerDetector = pointerDetector)
        }
        if (!isInPip) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
                exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
            ) {
                TopBar(
                    showBackground = !settings.hideButtonsBackground,
                    title = uiState.title,
                    onBack = onBack,
                    onAudio = { openOverlay(OverlayType.AUDIO) },
                    onSubtitle = { openOverlay(OverlayType.SUBTITLE) },
                    onSpeed = { openOverlay(OverlayType.SPEED) },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
                    exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
                ) {
                    MiddleControls(
                        controller = controller,
                        isPlaying = uiState.isPlaying,
                        onNext = { queueActions.next() },
                        onPrevious = { queueActions.previous() },
                    )
                }
            }
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
                exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
            ) {
                BottomBar(
                    showBackground = !settings.hideButtonsBackground,
                    controller = controller,
                    positionMs = uiState.positionMs,
                    durationMs = uiState.durationMs,
                    onLockToggle = { if (controlsState.locked) controlsState.unlock() else controlsState.lock() },
                    onScale = { openOverlay(OverlayType.SCALE) },
                    onRotate = onRotate,
                    onPip = onPip,
                    onBackground = onBackground,
                    onLoop = { uiActions.onLoop() },
                    onShuffle = { uiActions.onShuffle() },
                )
            }
        } // end Column
        } // end if (!isInPip)
        LockedControlsOverlay(
            visible = controlsState.locked,
            unlockHintVisible = lockedOverlayVisible,
            onTap = { lockedOverlayVisible = !lockedOverlayVisible },
            onUnlock = {
                controlsState.unlock()
                lockedOverlayVisible = false
            },
        )
        if (uiState.isBuffering) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        SeekIndicator(modifier = Modifier.align(Alignment.Center), seekState = seekState)
        DoubleTapIndicator(modifier = Modifier.align(Alignment.Center), state = tapFeedbackState)
        VerticalAdjustIndicator(modifier = Modifier.align(Alignment.Center), state = volumeBrightnessState)
        ZoomIndicator(modifier = Modifier.align(Alignment.Center), zoomState = zoomState)
        LongPressSpeedIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp), state = longPressSpeedState)
        OverlayPanel(
            type = overlayType,
            overlayActions = overlayActions,
            scaleState = scaleState,
            trackActions = trackActions,
            selectedAudio = trackUiState.selectedAudio,
            selectedSubtitle = trackUiState.selectedSubtitle,
            currentSpeed = trackUiState.currentSpeed,
            currentScaleMode = scaleState.mode,
            audioTracks = trackUiState.audioTracks,
            subtitleTracks = trackUiState.subtitleTracks,
            onDismiss = { overlayType = null },
        )
        ErrorOverlay(
            message = visibleError,
            onRetry = {
                dismissedErrorMessage = null
                controller.prepare()
                controller.play()
            },
            onNext = { queueActions.next() },
            onDismiss = { dismissedErrorMessage = uiState.errorMessage },
        )
    }
}

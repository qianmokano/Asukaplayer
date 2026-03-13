package com.asuka.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.asuka.player.render.api.PlaybackSurfaceTransform
import com.asuka.player.ui.components.BottomBar
import com.asuka.player.ui.components.DoubleTapIndicator
import com.asuka.player.ui.components.ErrorOverlay
import com.asuka.player.ui.components.GestureLayer
import com.asuka.player.ui.components.LockToggleAnchor
import com.asuka.player.ui.components.LockedControlsOverlay
import com.asuka.player.ui.components.LongPressSpeedIndicator
import com.asuka.player.ui.components.OverlayPanel
import com.asuka.player.ui.components.OverlayType
import com.asuka.player.ui.components.SeekIndicator
import com.asuka.player.ui.components.TopBar
import com.asuka.player.ui.components.VerticalAdjustIndicator
import com.asuka.player.ui.components.ZoomIndicator
import com.asuka.player.ui.controller.GestureConfig
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.OverlayActions
import com.asuka.player.ui.controller.OverlayTrackActions
import com.asuka.player.ui.controller.PointerGestureDetector
import com.asuka.player.ui.controller.QueueActions
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.LongPressSpeedState
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.state.TapFeedbackState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import com.asuka.player.ui.theme.PlayerUiTokens
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val surfaceState = model.surfaceState
    val trackUiState = model.trackUiState
    val settings = model.settings
    val isInPip = model.isInPip
    val controller = dependencies.controller
    val playbackPersistence = dependencies.playbackPersistence
    val deviceController = dependencies.deviceController
    val surfaceRenderer = dependencies.surfaceRenderer
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val landscapeCutoutPadding = rememberLandscapeCutoutPadding()
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
    val overlayActions = remember(controller) {
        OverlayActions(
            controller = controller,
        )
    }
    val trackActions = remember(dependencies.trackSelectionController) {
        dependencies.trackSelectionController?.let {
            OverlayTrackActions(
                trackSelectionController = it,
            )
        }
    }
    val queueActions = remember(controller) { QueueActions(controller) }
    LaunchedEffect(mediaIdState.value) {
        val mediaId = mediaIdState.value ?: return@LaunchedEffect
        val zoom = playbackPersistence.readZoom(mediaId) ?: 1f
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
                mediaIdState.value?.let { id ->
                    scope.launch {
                        playbackPersistence.saveZoom(id, zoom)
                    }
                }
            },
            playbackSpeedProvider = { playbackSpeedState.value },
            onDoubleTapFeedback = { delta -> tapFeedbackState.show(delta) },
            onLongPressFeedback = { active, speed ->
                if (active) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    longPressSpeedState.start(speed)
                } else {
                    longPressSpeedState.end()
                }
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
    LaunchedEffect(uiState.isPlaying) { if (uiState.isPlaying && !controlsState.locked) controlsState.show() }
    LaunchedEffect(seekState.seeking) { controlsState.setInteractionVisibilityHold(seekState.seeking) }
    LaunchedEffect(overlayType) { if (overlayType == null && !controlsState.locked) controlsState.show() }
    LaunchedEffect(tapFeedbackState.eventId) {
        if (tapFeedbackState.visible) {
            delay(PlayerUiTokens.Motion.feedbackMs)
            tapFeedbackState.hide()
        }
    }
    LaunchedEffect(uiState.errorMessage) { if (uiState.errorMessage != dismissedErrorMessage) dismissedErrorMessage = null }
    LaunchedEffect(controlsState.locked) {
        lockedOverlayVisible = controlsState.locked
    }
    LaunchedEffect(controlsState.locked, lockedOverlayVisible) {
        if (controlsState.locked && lockedOverlayVisible) {
            delay(3.seconds)
            lockedOverlayVisible = false
        }
    }
    val displayedPositionMs = if (seekState.seeking) seekState.previewPositionMs else uiState.positionMs
    val controlsVisible = controlsState.visible && !controlsState.locked
    val visibleError = uiState.errorMessage?.takeIf { it != dismissedErrorMessage }
    val videoBoundsModifier = Modifier.onGloballyPositioned { coords ->
        if (onVideoBoundsChanged != null) {
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
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        surfaceState?.let { playbackSurface ->
            surfaceRenderer?.Render(
                modifier = videoBoundsModifier,
                surfaceState = playbackSurface,
                transform = PlaybackSurfaceTransform(
                    zoomScale = zoomState.scale,
                    panOffset = zoomState.panOffset,
                    videoScaleMode = scaleState.mode,
                ),
            )
        }
        if (!controlsState.locked && overlayType == null && visibleError == null && !isInPip) {
            GestureLayer(coordinator = gestureCoordinator, pointerDetector = pointerDetector)
        }
        if (!isInPip) { Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
                exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
            ) {
                TopBar(
                    showBackground = !settings.hideButtonsBackground,
                    title = uiState.title,
                    landscapeCutoutPadding = landscapeCutoutPadding,
                    onBack = onBack,
                    onPip = onPip,
                    onBackground = onBackground,
                    onSettings = { openOverlay(OverlayType.SETTINGS) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
                exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
            ) {
                BottomBar(
                    showBackground = !settings.hideButtonsBackground,
                    controller = controller,
                    landscapeCutoutPadding = landscapeCutoutPadding,
                    positionMs = displayedPositionMs,
                    durationMs = uiState.durationMs,
                    isPlaying = uiState.isPlaying,
                    isBuffering = uiState.isBuffering,
                    onSeekBarDragChange = controlsState::setInteractionVisibilityHold,
                    onNext = { queueActions.next() },
                    onSpeed = { openOverlay(OverlayType.SPEED) },
                    onSubtitle = { openOverlay(OverlayType.SUBTITLE) },
                    onRotate = onRotate,
                )
            }
        } }
        LockToggleAnchor(
            visible = controlsVisible && !isInPip && !controlsState.locked,
            labelResId = R.string.lock,
            icon = Icons.Rounded.LockOpen,
            landscapeCutoutPadding = landscapeCutoutPadding,
            onClick = controlsState::lock,
            tag = "btn_lock",
        )
        LockedControlsOverlay(
            visible = controlsState.locked,
            unlockHintVisible = lockedOverlayVisible,
            landscapeCutoutPadding = landscapeCutoutPadding,
            onTap = { lockedOverlayVisible = !lockedOverlayVisible },
            onUnlock = { controlsState.unlock(); lockedOverlayVisible = false },
        )
        SeekIndicator(modifier = Modifier.align(Alignment.Center), seekState = seekState)
        DoubleTapIndicator(modifier = Modifier.align(Alignment.Center), state = tapFeedbackState)
        VerticalAdjustIndicator(
            modifier = Modifier.align(Alignment.Center),
            state = volumeBrightnessState,
        )
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
            currentRepeatMode = uiState.repeatMode,
            shuffleEnabled = uiState.shuffleEnabled,
            audioTracks = trackUiState.audioTracks,
            subtitleTracks = trackUiState.subtitleTracks,
            onOpenType = { overlayType = it },
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

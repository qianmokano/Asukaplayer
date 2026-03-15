package com.asuka.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.asuka.player.contract.PlaybackController
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.render.api.PlaybackSurfaceRenderer
import com.asuka.player.render.api.PlaybackSurfaceState
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
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.OverlayActions
import com.asuka.player.ui.controller.OverlayTrackActions
import com.asuka.player.ui.controller.PlaybackTrackUiState
import com.asuka.player.ui.controller.PointerGestureDetector
import com.asuka.player.ui.controller.QueueActions
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.LongPressSpeedState
import com.asuka.player.ui.state.PlayerUiState
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.TapFeedbackState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import com.asuka.player.ui.theme.PlayerUiTokens

@Composable
internal fun BoxScope.PlayerScreenGestureShell(
    surfaceState: PlaybackSurfaceState?,
    surfaceRenderer: PlaybackSurfaceRenderer?,
    zoomState: ZoomState,
    scaleState: ScaleState,
    videoBoundsModifier: Modifier,
    controlsState: ControlsState,
    overlayType: OverlayType?,
    visibleError: String?,
    isInPip: Boolean,
    gestureCoordinator: GestureCoordinator,
    pointerDetector: PointerGestureDetector,
    landscapeCutoutPadding: LandscapeCutoutPadding,
    controlsVisible: Boolean,
    lockedOverlayVisible: Boolean,
    onLockedOverlayTap: () -> Unit,
    onUnlock: () -> Unit,
    seekState: SeekState,
    mediaUri: String?,
    durationMs: Long,
    previewFrameProvider: PlaybackPreviewFrameProvider?,
    tapFeedbackState: TapFeedbackState,
    volumeBrightnessState: VolumeBrightnessState,
    longPressSpeedState: LongPressSpeedState,
) {
    surfaceState?.let { playbackSurface ->
        surfaceRenderer?.Render(
            modifier = videoBoundsModifier,
            surfaceState = playbackSurface,
            transform = PlaybackSurfaceTransform(
                zoomScale = zoomState.scale,
                panOffsetX = zoomState.panOffset.x,
                panOffsetY = zoomState.panOffset.y,
                videoScaleMode = scaleState.mode,
            ),
        )
    }
    if (!controlsState.locked && overlayType == null && visibleError == null && !isInPip) {
        GestureLayer(coordinator = gestureCoordinator, pointerDetector = pointerDetector)
    }
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
        onTap = onLockedOverlayTap,
        onUnlock = onUnlock,
    )
    SeekIndicator(
        modifier = Modifier.align(Alignment.Center),
        seekState = seekState,
        mediaId = mediaUri,
        durationMs = durationMs,
        previewFrameProvider = previewFrameProvider,
    )
    DoubleTapIndicator(modifier = Modifier.align(Alignment.Center), state = tapFeedbackState)
    VerticalAdjustIndicator(
        modifier = Modifier.align(Alignment.Center),
        state = volumeBrightnessState,
    )
    ZoomIndicator(modifier = Modifier.align(Alignment.Center), zoomState = zoomState)
    LongPressSpeedIndicator(modifier = Modifier.align(Alignment.Center), state = longPressSpeedState)
}

@Composable
internal fun PlayerScreenLayoutShell(
    controlsVisible: Boolean,
    isInPip: Boolean,
    settings: com.asuka.player.contract.PlayerSettings,
    uiState: PlayerUiState,
    controller: PlaybackController,
    landscapeCutoutPadding: LandscapeCutoutPadding,
    displayedPositionMs: Long,
    queueActions: QueueActions,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onBackground: () -> Unit,
    onOpenOverlay: (OverlayType) -> Unit,
    onRotate: () -> Unit,
    controlsState: ControlsState,
) {
    if (isInPip) return
    Column(modifier = Modifier.fillMaxSize()) {
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
                onSettings = { onOpenOverlay(OverlayType.SETTINGS) },
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
                onSpeed = { onOpenOverlay(OverlayType.SPEED) },
                onSubtitle = { onOpenOverlay(OverlayType.SUBTITLE) },
                onRotate = onRotate,
            )
        }
    }
}

@Composable
internal fun PlayerScreenOverlayShell(
    overlayType: OverlayType?,
    overlayActions: OverlayActions,
    scaleState: ScaleState,
    trackActions: OverlayTrackActions?,
    trackUiState: PlaybackTrackUiState,
    currentRepeatMode: com.asuka.player.contract.LoopMode,
    shuffleEnabled: Boolean,
    onOpenType: (OverlayType) -> Unit,
    onDismissOverlay: () -> Unit,
    visibleError: String?,
    onRetry: () -> Unit,
    onNext: () -> Unit,
    onDismissError: () -> Unit,
) {
    OverlayPanel(
        type = overlayType,
        overlayActions = overlayActions,
        scaleState = scaleState,
        trackActions = trackActions,
        selectedAudio = trackUiState.selectedAudio,
        selectedSubtitle = trackUiState.selectedSubtitle,
        currentSpeed = trackUiState.currentSpeed,
        currentScaleMode = scaleState.mode,
        currentRepeatMode = currentRepeatMode,
        shuffleEnabled = shuffleEnabled,
        audioTracks = trackUiState.audioTracks,
        subtitleTracks = trackUiState.subtitleTracks,
        onOpenType = onOpenType,
        onDismiss = onDismissOverlay,
    )
    ErrorOverlay(
        message = visibleError,
        onRetry = onRetry,
        onNext = onNext,
        onDismiss = onDismissError,
    )
}

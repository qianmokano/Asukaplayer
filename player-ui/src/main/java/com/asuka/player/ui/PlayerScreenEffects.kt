package com.asuka.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.ui.components.OverlayType
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.LongPressSpeedState
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.state.TapFeedbackState
import com.asuka.player.ui.state.ZoomState
import com.asuka.player.ui.theme.PlayerUiTokens
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun PlayerScreenEffects(
    mediaIdState: State<String?>,
    uiErrorMessage: String?,
    uiIsPlaying: Boolean,
    controlsState: ControlsState,
    overlayType: OverlayType?,
    dismissedErrorMessage: String?,
    onDismissedErrorReset: () -> Unit,
    seekState: SeekState,
    tapFeedbackState: TapFeedbackState,
    longPressSpeedState: LongPressSpeedState,
    zoomState: ZoomState,
    playbackPersistence: PlaybackUiPersistence,
    scope: CoroutineScope,
    lockedOverlayVisible: Boolean,
    onLockedOverlayVisibleChange: (Boolean) -> Unit,
) {
    LaunchedEffect(mediaIdState.value) {
        val mediaId = mediaIdState.value ?: return@LaunchedEffect
        val zoom = playbackPersistence.readZoom(mediaId) ?: 1f
        zoomState.setTransform(zoom, Offset.Zero, pinching = false)
    }
    LaunchedEffect(uiIsPlaying) {
        if (uiIsPlaying && !controlsState.locked) {
            controlsState.show()
        }
    }
    LaunchedEffect(seekState.seeking) {
        controlsState.setInteractionVisibilityHold(seekState.seeking)
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
    LaunchedEffect(uiErrorMessage) {
        if (uiErrorMessage != dismissedErrorMessage) {
            onDismissedErrorReset()
        }
    }
    LaunchedEffect(controlsState.locked) {
        onLockedOverlayVisibleChange(controlsState.locked)
    }
    LaunchedEffect(controlsState.locked, lockedOverlayVisible) {
        if (controlsState.locked && lockedOverlayVisible) {
            delay(3.seconds)
            onLockedOverlayVisibleChange(false)
        }
    }
}

internal fun createLongPressFeedbackHandler(
    haptic: HapticFeedback,
    longPressSpeedState: LongPressSpeedState,
): (Boolean, Float) -> Unit {
    return { active, speed ->
        if (active) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            longPressSpeedState.start(speed)
        } else {
            longPressSpeedState.end()
        }
    }
}

internal fun createPersistZoomHandler(
    mediaIdState: State<String?>,
    playbackPersistence: PlaybackUiPersistence,
    scope: CoroutineScope,
): (Float) -> Unit {
    return { zoom ->
        mediaIdState.value?.let { mediaId ->
            scope.launch {
                playbackPersistence.saveZoom(mediaId, zoom)
            }
        }
    }
}

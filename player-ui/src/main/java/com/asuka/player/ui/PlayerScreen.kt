package com.asuka.player.ui

import android.app.Activity
import android.media.AudioManager
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.asuka.player.core.PlaybackController
import com.asuka.player.core.PlaybackStateRestorer
import com.asuka.player.data.PlaybackStore
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
import com.asuka.player.ui.controller.GestureCoordinator
import com.asuka.player.ui.controller.OverlayActions
import com.asuka.player.ui.controller.OverlayTrackActions
import com.asuka.player.ui.controller.PointerGestureDetector
import com.asuka.player.ui.controller.QueueActions
import com.asuka.player.ui.state.SeekState
import com.asuka.player.ui.controller.SelectionState
import com.asuka.player.ui.controller.TrackUiStateHolder
import com.asuka.player.ui.controller.UiActions
import com.asuka.player.ui.state.ControlsState
import com.asuka.player.ui.state.LongPressSpeedState
import com.asuka.player.ui.state.PlayerUiState
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.state.TapFeedbackState
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.state.ZoomState
import com.asuka.player.ui.theme.PlayerUiTokens
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

/**
 * M1 minimal UI: title + controls + gesture layer.
 */
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    player: Player?,
    controller: PlaybackController,
    bindings: com.asuka.player.ui.controller.ControllerBindings?,
    store: PlaybackStore,
    settings: PlayerRuntimeSettings = PlayerRuntimeSettings(),
    isInPip: Boolean = false,
    onVideoBoundsChanged: ((android.graphics.Rect) -> Unit)? = null,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onBackground: () -> Unit,
    onRotate: () -> Unit = {},
) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(AudioManager::class.java)
    }
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

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
    val mediaIdState = rememberUpdatedState(player?.currentMediaItem?.mediaId)
    val playbackSpeedState = rememberUpdatedState(player?.playbackParameters?.speed ?: 1.0f)
    val overlayActions = remember(controller, store) {
        OverlayActions(
            controller = controller,
            store = store,
            mediaIdProvider = { mediaIdState.value },
        )
    }
    val trackActions = remember(bindings) {
        bindings?.let {
            OverlayTrackActions(
                trackSelection = it.trackSelection,
                store = store,
                mediaIdProvider = { mediaIdState.value },
            )
        }
    }
    val queueActions = remember(controller) { QueueActions(controller) }
    val trackStateHolder = remember(player) {
        player?.let { TrackUiStateHolder(it) }
    }
    val selectionState = remember(player) {
        player?.let { SelectionState(it) }
    }
    DisposableEffect(trackStateHolder) {
        trackStateHolder?.attach()
        onDispose { trackStateHolder?.detach() }
    }
    DisposableEffect(selectionState) {
        selectionState?.attach()
        onDispose { selectionState?.detach() }
    }
    LaunchedEffect(mediaIdState.value) {
        val mediaId = mediaIdState.value ?: return@LaunchedEffect
        val resume = PlaybackStateRestorer(store).read(mediaId)
        val zoom = resume.zoom ?: 1f
        zoomState.setTransform(zoom, androidx.compose.ui.geometry.Offset.Zero, pinching = false)
    }
    val pointerDetector = remember(settings.seekSensitivity, audioManager, activity) {
        PointerGestureDetector(
            seekSensitivity = settings.seekSensitivity.coerceIn(0.1f, 2.0f),
            positionProvider = { positionMsState.value },
            durationProvider = { durationMsState.value },
            volumeProvider = {
                val am = audioManager
                if (am != null) {
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                    ((cur.toFloat() / max) * 100f).toInt().coerceIn(0, 100)
                } else {
                    volumeBrightnessState.volumePercent
                }
            },
            brightnessProvider = {
                val act = activity
                if (act != null) {
                    val current = act.window.attributes.screenBrightness
                    if (current >= 0f) {
                        (current * 100f).toInt().coerceIn(0, 100)
                    } else {
                        50
                    }
                } else {
                    volumeBrightnessState.brightnessPercent
                }
            },
        )
    }
    val uiActions = remember(controller) { UiActions(controller) }
    val gestureCoordinator = remember(controller, controlsState, volumeBrightnessState, seekState, zoomState, settings) {
        GestureCoordinator(
            controller = controller,
            controlsState = controlsState,
            volumeBrightnessState = volumeBrightnessState,
            seekState = seekState,
            zoomState = zoomState,
            onZoomEnd = { zoom ->
                mediaIdState.value?.let { id -> store.saveZoom(id, zoom) }
            },
            playbackSpeedProvider = { playbackSpeedState.value },
            onDoubleTapFeedback = { delta -> tapFeedbackState.show(delta) },
            onLongPressFeedback = { active, speed ->
                if (active) longPressSpeedState.start(speed) else longPressSpeedState.end()
            },
            onVolumeChanged = { percent ->
                val am = audioManager
                if (am != null) {
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    val level = ((percent / 100f) * max).toInt().coerceIn(0, max)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0)
                }
            },
            onBrightnessChanged = { percent ->
                val act = activity
                if (act != null) {
                    val lp = act.window.attributes
                    lp.screenBrightness = (percent / 100f).coerceIn(0f, 1f)
                    act.window.attributes = lp
                }
            },
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
    val visibleError = uiState.errorMessage?.takeIf { it != dismissedErrorMessage }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .then(
                if (onVideoBoundsChanged != null) {
                    Modifier.onGloballyPositioned { coords ->
                        val b = coords.boundsInWindow()
                        onVideoBoundsChanged(
                            android.graphics.Rect(
                                b.left.toInt(), b.top.toInt(),
                                b.right.toInt(), b.bottom.toInt(),
                            )
                        )
                    }
                } else Modifier
            ),
    ) {
        player?.let { p ->
            VideoSurfaceWithTransform(
                player = p,
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
                visible = controlsState.visible && !controlsState.locked,
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
                    visible = controlsState.visible && !controlsState.locked,
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
                visible = controlsState.visible && !controlsState.locked,
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
        val audioTracks = trackStateHolder?.audioTracks?.collectAsState(initial = emptyList())?.value ?: emptyList()
        val subtitleTracks = trackStateHolder?.subtitleTracks?.collectAsState(initial = emptyList())?.value ?: emptyList()
        val selectedAudio = selectionState?.selectedAudio?.collectAsState(initial = null)?.value
        val selectedSubtitle = selectionState?.selectedSubtitle?.collectAsState(initial = null)?.value
        val currentSpeed = player?.playbackParameters?.speed ?: 1.0f
        OverlayPanel(
            type = overlayType,
            controller = controller,
            overlayActions = overlayActions,
            scaleState = scaleState,
            trackActions = trackActions,
            selectedAudio = selectedAudio,
            selectedSubtitle = selectedSubtitle,
            currentSpeed = currentSpeed,
            currentScaleMode = scaleState.mode,
            audioTracks = audioTracks,
            subtitleTracks = subtitleTracks,
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

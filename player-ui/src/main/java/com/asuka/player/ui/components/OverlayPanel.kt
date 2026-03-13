package com.asuka.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.CenterEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asuka.player.contract.LoopMode
import com.asuka.player.ui.controller.TrackOption
import com.asuka.player.ui.controller.OverlayActions
import com.asuka.player.ui.controller.OverlayTrackActions
import com.asuka.player.ui.R
import com.asuka.player.ui.state.ScaleState
import com.asuka.player.ui.theme.PlayerUiTokens
import kotlinx.coroutines.yield

enum class OverlayType {
    SETTINGS,
    AUDIO,
    SUBTITLE,
    SPEED,
    SCALE,
    LOOP_MODE,
    SHUFFLE_MODE,
}

@Composable
fun OverlayPanel(
    modifier: Modifier = Modifier,
    type: OverlayType?,
    overlayActions: OverlayActions,
    scaleState: ScaleState,
    trackActions: OverlayTrackActions?,
    selectedAudio: Int?,
    selectedSubtitle: Int?,
    currentSpeed: Float,
    currentScaleMode: com.asuka.player.contract.VideoScaleMode,
    currentRepeatMode: LoopMode,
    shuffleEnabled: Boolean,
    audioTracks: List<TrackOption>,
    subtitleTracks: List<TrackOption>,
    onOpenType: (OverlayType) -> Unit,
    onDismiss: () -> Unit,
) {
    // Keep the last non-null type so panel content stays rendered during the exit animation.
    var lastType by remember { mutableStateOf<OverlayType?>(null) }
    if (type != null) lastType = type

    val showing = type != null
    var canDismiss by remember(type) { mutableStateOf(false) }
    LaunchedEffect(type) {
        canDismiss = false
        yield()
        canDismiss = true
    }
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim — fades in when the panel opens, fades out when it closes.
        AnimatedVisibility(
            visible = showing,
            enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
            exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = PlayerUiTokens.Alpha.overlayBackdrop))
                    .clickable(
                        enabled = canDismiss,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                    .testTag("overlay_root"),
            )
        }

        // Panel sheet — slides in from the bottom (portrait) or from the right (landscape).
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isPortrait) BottomCenter else CenterEnd,
        ) {
            AnimatedVisibility(
                visible = showing,
                enter = if (isPortrait) {
                    fadeIn(tween(PlayerUiTokens.Motion.normalMs)) + slideInVertically { it }
                } else {
                    fadeIn(tween(PlayerUiTokens.Motion.normalMs)) + slideInHorizontally { it }
                },
                exit = if (isPortrait) {
                    fadeOut(tween(PlayerUiTokens.Motion.fastMs)) + slideOutVertically { it }
                } else {
                    fadeOut(tween(PlayerUiTokens.Motion.fastMs)) + slideOutHorizontally { it }
                },
            ) {
                Surface(
                    modifier = if (isPortrait) {
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.46f)
                    } else {
                        Modifier
                            .fillMaxWidth(0.48f)
                            .fillMaxHeight()
                    },
                    shape = MaterialTheme.shapes.large,
                    color = PlayerUiTokens.panelBackground,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            )
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 16.dp),
                    ) {
                        val displayType = lastType ?: return@Column
                        val title = when (displayType) {
                            OverlayType.SETTINGS -> stringResource(id = R.string.settings)
                            OverlayType.AUDIO -> stringResource(id = R.string.audio_track)
                            OverlayType.SUBTITLE -> stringResource(id = R.string.subtitle)
                            OverlayType.SPEED -> stringResource(id = R.string.playback_speed)
                            OverlayType.SCALE -> stringResource(id = R.string.content_scale)
                            OverlayType.LOOP_MODE -> stringResource(id = R.string.loop)
                            OverlayType.SHUFFLE_MODE -> stringResource(id = R.string.shuffle)
                        }
                        Text(
                            text = title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // AnimatedContent crossfades between panel types when the user
                        // switches from one overlay to another without closing first.
                        Box(modifier = Modifier.fillMaxSize()) {
                            AnimatedContent(
                                targetState = displayType,
                                transitionSpec = {
                                    fadeIn(tween(PlayerUiTokens.Motion.fastMs)) togetherWith
                                        fadeOut(tween(PlayerUiTokens.Motion.fastMs))
                                },
                                label = "panel_content",
                            ) { panel ->
                                when (panel) {
                                    OverlayType.SETTINGS -> SettingsMenuPanel(
                                        audioSummary = audioTracks
                                            .firstOrNull {
                                                com.asuka.player.contract.TrackIndexCodec.encode(it.groupIndex, it.trackIndex) == selectedAudio
                                            }
                                            ?.label
                                            ?: stringResource(id = R.string.no_audio_track),
                                        scaleSummary = currentScaleMode.toOverlayLabel(),
                                        loopSummary = currentRepeatMode.toLoopModeLabel(),
                                        shuffleSummary = if (shuffleEnabled) {
                                            stringResource(id = R.string.playback_mode_shuffle_on)
                                        } else {
                                            stringResource(id = R.string.playback_mode_shuffle_off)
                                        },
                                        onAudio = { onOpenType(OverlayType.AUDIO) },
                                        onScale = { onOpenType(OverlayType.SCALE) },
                                        onLoopMode = { onOpenType(OverlayType.LOOP_MODE) },
                                        onShuffleMode = { onOpenType(OverlayType.SHUFFLE_MODE) },
                                    )
                                    OverlayType.AUDIO -> AudioSelectorPanel(
                                        audioTracks,
                                        selectedAudio,
                                    ) { track ->
                                        trackActions?.setAudioTrack(track)
                                    }
                                    OverlayType.SUBTITLE -> SubtitleSelectorPanel(
                                        subtitleTracks,
                                        selectedSubtitle,
                                        onDisable = { trackActions?.disableSubtitles() },
                                    ) { track ->
                                        trackActions?.setSubtitleTrack(track)
                                    }
                                    OverlayType.SPEED -> SpeedSelectorPanel(
                                        selectedSpeed = currentSpeed,
                                    ) { overlayActions.setSpeed(it) }
                                    OverlayType.SCALE -> ScaleSelectorPanel(
                                        selectedMode = currentScaleMode,
                                    ) {
                                        overlayActions.setScale(it)
                                        scaleState.updateMode(it)
                                    }
                                    OverlayType.LOOP_MODE -> LoopModePanel(
                                        currentRepeatMode = currentRepeatMode,
                                        onLoopMode = overlayActions::setLoopMode,
                                    )
                                    OverlayType.SHUFFLE_MODE -> ShuffleModePanel(
                                        shuffleEnabled = shuffleEnabled,
                                        onShuffleEnabled = overlayActions::setShuffleEnabled,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

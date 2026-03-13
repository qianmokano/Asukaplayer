package com.asuka.player.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.R
import com.asuka.player.ui.state.VolumeBrightnessState
import com.asuka.player.ui.theme.PlayerUiTokens

private val indicatorHeight = 52.dp
private val portraitIndicatorGapFromPlaybackButton = 96.dp
private val landscapeIndicatorGapFromPlaybackButton = 22.dp

private data class VerticalAdjustSpec(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun VerticalAdjustIndicator(
    modifier: Modifier = Modifier,
    state: VolumeBrightnessState,
) {
    val mode = state.activeMode ?: return
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val indicatorGapFromPlaybackButton = if (isLandscape) {
        landscapeIndicatorGapFromPlaybackButton
    } else {
        portraitIndicatorGapFromPlaybackButton
    }
    val indicatorVerticalOffset =
        -((PlayerUiTokens.ButtonSize.playbackPrimary / 2) + indicatorGapFromPlaybackButton + (indicatorHeight / 2))
    val value = when (mode) {
        VolumeBrightnessState.Mode.VOLUME -> state.volumePercent
        VolumeBrightnessState.Mode.BRIGHTNESS -> state.brightnessPercent
    }
    val spec = when (mode) {
        VolumeBrightnessState.Mode.VOLUME -> VerticalAdjustSpec(
            label = stringResource(R.string.vertical_adjust_volume),
            icon = Icons.AutoMirrored.Rounded.VolumeUp,
        )
        VolumeBrightnessState.Mode.BRIGHTNESS -> VerticalAdjustSpec(
            label = stringResource(R.string.vertical_adjust_brightness),
            icon = Icons.Rounded.Brightness6,
        )
    }
    val progress = value / 100f
    val colorScheme = MaterialTheme.colorScheme
    val surfaceColor = colorScheme.primaryContainer.copy(alpha = 0.40f)
    val contentColor = colorScheme.onPrimaryContainer
    val meterTrackColor = PlayerUiTokens.buttonBackground()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = indicatorVerticalOffset),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.30f else 0.68f)
                .widthIn(
                    min = if (isLandscape) 120.dp else 220.dp,
                    max = if (isLandscape) 164.dp else 360.dp,
                )
                .padding(horizontal = 8.dp)
                .testTag("vertical_adjust_indicator")
                .semantics { stateDescription = "${spec.label} $value%" },
            shape = MaterialTheme.shapes.large,
            color = surfaceColor,
            contentColor = contentColor,
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(min = if (isLandscape) 40.dp else 96.dp)
                        .height(6.dp)
                        .testTag("vertical_adjust_meter")
                        .semantics {
                            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        },
                    color = contentColor,
                    trackColor = meterTrackColor,
                )
            }
        }
    }
}

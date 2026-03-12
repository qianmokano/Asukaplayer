package com.asuka.player.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.LandscapeCutoutPadding
import com.asuka.player.ui.R
import com.asuka.player.ui.theme.PlayerUiTokens

@Composable
internal fun TopBar(
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    title: String,
    landscapeCutoutPadding: LandscapeCutoutPadding = LandscapeCutoutPadding.None,
    onBack: () -> Unit,
    onAudio: () -> Unit,
    onSubtitle: () -> Unit,
    onSpeed: () -> Unit,
    onTitleLongPress: (() -> Unit)? = null,
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current
    val topInsetModifier = if (isLandscape) Modifier else Modifier.statusBarsPadding()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = if (showBackground) {
                        listOf(Color.Black.copy(alpha = PlayerUiTokens.Alpha.topGradientStart), Color.Transparent)
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    },
                ),
            )
            .then(topInsetModifier)
            .padding(
                start = PlayerUiTokens.Spacing.sm + landscapeCutoutPadding.start(layoutDirection),
                top = PlayerUiTokens.Spacing.xs,
                end = PlayerUiTokens.Spacing.sm + landscapeCutoutPadding.end(layoutDirection),
                bottom = PlayerUiTokens.Spacing.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SimpleButton(
            label = stringResource(id = R.string.back),
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            onClick = onBack,
            tag = "btn_back",
        )

        val titleModifier = if (onTitleLongPress == null) {
            Modifier.weight(1f)
        } else {
            Modifier
                .weight(1f)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                    onLongClick = onTitleLongPress,
                )
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = titleModifier,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SimpleButton(
                label = stringResource(id = R.string.speed),
                icon = Icons.Rounded.Speed,
                onClick = onSpeed,
                tag = "btn_speed",
            )
            SimpleButton(
                label = stringResource(id = R.string.audio),
                icon = Icons.Rounded.GraphicEq,
                onClick = onAudio,
                tag = "btn_audio",
            )
            SimpleButton(
                label = stringResource(id = R.string.subs),
                icon = Icons.Rounded.Subtitles,
                onClick = onSubtitle,
                tag = "btn_subs",
            )
        }
    }
}

package com.asuka.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.R
import com.asuka.player.ui.theme.PlayerUiTokens

private val lockToggleAnchorPadding = 20.dp

@Composable
fun BoxScope.LockToggleAnchor(
    visible: Boolean,
    labelResId: Int,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(PlayerUiTokens.Motion.normalMs)),
        exit = fadeOut(animationSpec = tween(PlayerUiTokens.Motion.fastMs)),
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = lockToggleAnchorPadding),
    ) {
        SimpleButton(
            label = stringResource(id = labelResId),
            icon = icon,
            onClick = onClick,
            tag = tag,
        )
    }
}

@Composable
fun LockedControlsOverlay(
    visible: Boolean,
    unlockHintVisible: Boolean,
    onTap: () -> Unit,
    onUnlock: () -> Unit,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(unlockHintVisible) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center,
    ) {
        LockToggleAnchor(
            visible = unlockHintVisible,
            labelResId = R.string.unlock,
            icon = Icons.Outlined.Lock,
            onClick = onUnlock,
            tag = "btn_unlock_controls",
        )
    }
}

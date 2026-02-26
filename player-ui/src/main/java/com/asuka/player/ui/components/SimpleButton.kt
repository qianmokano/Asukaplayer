package com.asuka.player.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asuka.player.ui.theme.PlayerUiTokens

@Composable
fun SimpleButton(
    label: String,
    onClick: () -> Unit,
    tag: String? = null,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    size: Dp = 44.dp,
    iconSize: Dp = 24.dp,
) {
    val baseMod = if (tag != null) Modifier.testTag(tag) else Modifier
    if (icon != null) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = baseMod.size(size),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = PlayerUiTokens.buttonBackground,
                contentColor = tint,
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    } else {
        TextButton(onClick = onClick, modifier = baseMod) {
            Text(text = label)
        }
    }
}

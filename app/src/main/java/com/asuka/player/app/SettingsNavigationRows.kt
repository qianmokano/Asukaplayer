package com.asuka.player.app

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow

@Composable
internal fun SettingsNavigationItem(
    icon: ImageVector,
    thumbnailUri: Uri? = null,
    thumbnailId: Long? = null,
    durationLabel: String? = null,
    progressFraction: Float? = null,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val alpha = when {
        !enabled -> 0.56f
        selected -> 1f
        else -> 0.92f
    }
    val useVideoSubtitleStyle = thumbnailUri != null || thumbnailId != null
    val isVideoRow = thumbnailUri != null || thumbnailId != null
    val rowHorizontalPadding =
        if (isVideoRow) VIDEO_ROW_HORIZONTAL_PADDING else DEFAULT_ROW_HORIZONTAL_PADDING
    val itemSpacing = if (isVideoRow) VIDEO_ROW_ITEM_SPACING else DEFAULT_ROW_ITEM_SPACING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isVideoRow) VIDEO_ITEM_ROW_HEIGHT else DEFAULT_ITEM_ROW_HEIGHT)
            .clickable(enabled = enabled) {
                if (enabled && hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                onClick()
            }
            .padding(horizontal = rowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumbOrIcon(
            icon = icon,
            thumbnailUri = thumbnailUri,
            thumbnailId = thumbnailId,
            durationLabel = durationLabel,
            progressFraction = progressFraction,
            selected = selected,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = if (isVideoRow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = if (useVideoSubtitleStyle) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = if (isVideoRow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!isVideoRow && enabled) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

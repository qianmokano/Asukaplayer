package com.asuka.player.app

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.asuka.player.R

internal val LocalHapticsEnabled = staticCompositionLocalOf { true }

internal val VIDEO_ITEM_THUMB_WIDTH = 116.dp
internal val VIDEO_ITEM_THUMB_HEIGHT = 74.dp
internal val VIDEO_ITEM_ROW_HEIGHT = 92.dp
internal val DEFAULT_ITEM_ROW_HEIGHT = 64.dp
internal val GROUP_OUTER_CORNER_RADIUS = 24.dp
internal val GROUP_SOFT_CORNER_RADIUS = 6.dp
internal val VIDEO_PAGE_CORNER_RADIUS = 8.dp
internal val GROUP_ITEM_SPACING_DEFAULT = 2.dp
internal val GROUP_HORIZONTAL_PADDING_DEFAULT = 16.dp
internal val VIDEO_GROUP_HORIZONTAL_PADDING = 16.dp
internal val DEFAULT_ROW_HORIZONTAL_PADDING = 16.dp
internal val VIDEO_ROW_HORIZONTAL_PADDING = 12.dp
internal val DEFAULT_ROW_ITEM_SPACING = 16.dp
internal val VIDEO_ROW_ITEM_SPACING = 12.dp

internal data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

internal class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(
        key: Any? = null,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

@Composable
internal fun SplicedColumnGroup(
    title: String,
    content: SplicedGroupScope.() -> Unit,
) {
    val allItems = remember(content) {
        SplicedGroupScope().apply(content).items.toList()
    }
    if (allItems.isEmpty()) return

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )

        allItems.forEachIndexed { index, itemData ->
            key(itemData.key) {
                if (itemData.visible) {
                    val isFirst = index == 0
                    val isLast = index == allItems.lastIndex
                    val shape = RoundedCornerShape(
                        topStart = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                        topEnd = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                        bottomStart = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                        bottomEnd = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                    )
                    val topPadding = if (index == 0) 0.dp else 2.dp
                    Column(
                        modifier = Modifier
                            .padding(top = topPadding)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceBright),
                    ) {
                        itemData.content()
                    }
                }
            }
        }
    }
}

@Composable
internal fun LoadingBlock() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
internal fun EmptyBlock(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 32.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun GroupedListRow(
    index: Int,
    totalCount: Int,
    itemSpacing: Dp = GROUP_ITEM_SPACING_DEFAULT,
    horizontalPadding: Dp = GROUP_HORIZONTAL_PADDING_DEFAULT,
    useSoftCornersOnly: Boolean = false,
    useLargeCornersOnly: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isFirst = index == 0
    val isLast = index == totalCount - 1
    val shape = if (useLargeCornersOnly) {
        RoundedCornerShape(VIDEO_PAGE_CORNER_RADIUS)
    } else if (useSoftCornersOnly) {
        RoundedCornerShape(GROUP_SOFT_CORNER_RADIUS)
    } else {
        RoundedCornerShape(
            topStart = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
            topEnd = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
            bottomStart = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
            bottomEnd = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = if (isFirst) 0.dp else itemSpacing),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = shape,
    ) {
        content()
    }
}

@Composable
internal fun SettingsNavigationItem(
    icon: ImageVector,
    thumbnailUri: Uri? = null,
    thumbnailId: Long? = null,
    durationLabel: String? = null,
    title: String,
    description: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val alpha = if (selected) 1f else 0.92f
    val useVideoSubtitleStyle = thumbnailUri != null || thumbnailId != null
    val isVideoRow = thumbnailUri != null || thumbnailId != null
    val rowHorizontalPadding = if (isVideoRow) VIDEO_ROW_HORIZONTAL_PADDING else DEFAULT_ROW_HORIZONTAL_PADDING
    val itemSpacing = if (isVideoRow) VIDEO_ROW_ITEM_SPACING else DEFAULT_ROW_ITEM_SPACING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isVideoRow) VIDEO_ITEM_ROW_HEIGHT else DEFAULT_ITEM_ROW_HEIGHT)
            .clickable {
                if (hapticsEnabled) {
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

        if (!isVideoRow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

@Composable
internal fun SettingsSliderItem(
    icon: ImageVector? = null,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    enabled: Boolean = true,
    onReset: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onReset != null) {
                    IconButton(
                        onClick = onReset,
                        enabled = enabled,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.content_desc_reset),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
            )
        }
    }
}

@Composable
internal fun SettingsToggleItem(
    icon: ImageVector? = null,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val onToggle: (Boolean) -> Unit = { checkedValue ->
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
        onCheckedChange(checkedValue)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
        )
    }
}

@Composable
internal fun SettingsToggleNavigationItem(
    icon: ImageVector? = null,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .clickable {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                onClick()
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerticalDivider(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
            )
        }
    }
}

@Composable
internal fun SettingsRadioItem(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .clickable {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
                onClick()
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun CustomThemeRow(
    theme: CustomThemeEntry,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(theme.seed),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (theme.monochrome) stringResource(R.string.theme_mode_monochrome) else stringResource(R.string.theme_mode_color),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = onDelete,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

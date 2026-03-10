package com.asuka.player.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun ThemeSwatch(
    label: String,
    scheme: ColorScheme,
    selected: Boolean,
    disabled: Boolean,
    shape: androidx.compose.foundation.shape.RoundedCornerShape,
    size: Dp,
    hapticsEnabled: Boolean,
    icon: ImageVector? = null,
    showDynamicIcon: Boolean = false,
    showDelete: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = shape,
        tonalElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .combinedClickable(
                enabled = !disabled,
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                    onClick()
                },
                onLongClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onLongPress?.invoke()
                },
            ),
        color = scheme.surfaceContainer,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    if (showDelete) {
                        DeleteSwatch(borderColor = scheme.primary)
                    } else {
                        ColorSwatch(
                            color = scheme.primary,
                            selected = selected,
                            icon = icon,
                            iconTint = if (icon != null) scheme.onPrimaryContainer else null,
                            borderColor = scheme.primary,
                        )
                        if (showDynamicIcon) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 3.dp, y = 3.dp),
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (disabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun ColorSwatch(
    color: Color,
    selected: Boolean = false,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    borderColor: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = CircleShape
    val borderWidth = if (selected) 2.dp else 1.dp
    val strokeColor = if (selected) borderColor else borderColor.copy(alpha = 0.45f)
    val innerPadding = if (selected) 2.dp else 0.dp
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(borderWidth, strokeColor, shape)
            .padding(innerPadding)
            .clip(shape),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.85f)))
            }
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.65f)))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.5f)))
            }
        }
        if (icon != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else if (selected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
internal fun DeleteSwatch(borderColor: Color) {
    val shape = CircleShape
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, borderColor, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun ThemeSectionBlock(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        content()
    }
}

@Composable
internal fun PreviewChip(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = background,
        tonalElevation = 1.dp,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = foreground,
            )
        }
    }
}

package com.asuka.player.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
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
internal fun ErrorBlock(
    title: String,
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(
                onClick = onAction,
            ) {
                Text(text = actionLabel)
            }
        }
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
        androidx.compose.foundation.shape.RoundedCornerShape(VIDEO_PAGE_CORNER_RADIUS)
    } else if (useSoftCornersOnly) {
        androidx.compose.foundation.shape.RoundedCornerShape(GROUP_SOFT_CORNER_RADIUS)
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(
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

package com.asuka.player.app

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlin.math.min
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun <T> rememberIncrementalItems(
    items: List<T>,
    listState: LazyListState,
    pageSize: Int = 60,
    loadMoreThreshold: Int = 20,
): List<T> {
    val safePageSize = pageSize.coerceAtLeast(1)
    val safeThreshold = loadMoreThreshold.coerceAtLeast(1)
    var visibleCount by remember(items) {
        mutableIntStateOf(min(items.size, safePageSize))
    }

    LaunchedEffect(items, safePageSize) {
        visibleCount = min(items.size, safePageSize)
    }
    LaunchedEffect(listState, items.size, safePageSize, safeThreshold) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex >= visibleCount - safeThreshold && visibleCount < items.size) {
                    visibleCount = min(items.size, visibleCount + safePageSize)
                }
            }
    }

    return remember(items, visibleCount) {
        items.take(visibleCount)
    }
}

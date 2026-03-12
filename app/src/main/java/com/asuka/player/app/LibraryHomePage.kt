package com.asuka.player.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
internal fun HomePageContent(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    hasLimitedMediaAccess: Boolean,
    foldersState: MediaCatalogState<LocalVideoFolder>,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenFolder: (Long) -> Unit,
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val folders = foldersState.items

    LaunchedEffect(listState, folders.size, foldersState.hasMore, foldersState.isAppending, foldersState.isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .map { lastVisibleIndex ->
                foldersState.hasMore &&
                    !foldersState.isAppending &&
                    !foldersState.isLoading &&
                    lastVisibleIndex >= folders.lastIndex - 8
            }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = foldersState.isLoading && foldersState.hasLoadedOnce,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = foldersState.isLoading && foldersState.hasLoadedOnce,
                state = pullToRefreshState,
            )
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 92.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            if (!permissionGranted && !hasLimitedMediaAccess) {
                item {
                    SplicedColumnGroup(title = stringResource(id = R.string.permission_required_title)) {
                        item {
                            SettingsNavigationItem(
                                icon = Icons.Rounded.Lock,
                                title = stringResource(id = R.string.grant_permission),
                                description = if (hasLimitedMediaAccess) {
                                    stringResource(id = R.string.permission_hint_limited)
                                } else {
                                    stringResource(id = R.string.permission_hint)
                                },
                                onClick = onRequestPermission,
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            if (!permissionGranted && hasLimitedMediaAccess) {
                item {
                    SplicedColumnGroup(title = stringResource(id = R.string.limited_access_title)) {
                        item {
                            SettingsNavigationItem(
                                icon = Icons.Rounded.Info,
                                title = stringResource(id = R.string.limited_access_banner_title),
                                description = stringResource(id = R.string.limited_access_banner_body),
                                onClick = onRequestPermission,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(4.dp)) }

            when {
                foldersState.isLoading && !foldersState.hasLoadedOnce -> {
                    item { LoadingSectionBlock() }
                }

                folders.isEmpty() && foldersState.errorMessage != null -> {
                    item {
                        AnimatedItemEntrance {
                            ErrorBlock(
                                title = stringResource(id = R.string.media_library_refresh_error_title),
                                text = foldersState.errorMessage.resolve(context),
                                actionLabel = stringResource(id = R.string.media_library_refresh_retry),
                                onAction = onRefresh,
                            )
                        }
                    }
                }

                folders.isEmpty() -> {
                    item {
                        AnimatedItemEntrance {
                            EmptyBlock(
                                text = if (!permissionGranted && hasLimitedMediaAccess) {
                                    stringResource(id = R.string.empty_video_list_limited)
                                } else {
                                    stringResource(id = R.string.empty_video_list)
                                },
                            )
                        }
                    }
                }

                else -> {
                    foldersState.errorMessage?.let { message ->
                        item {
                            AnimatedItemEntrance {
                                ErrorBlock(
                                    title = stringResource(id = R.string.media_library_refresh_error_title),
                                    text = message.resolve(context),
                                    actionLabel = stringResource(id = R.string.media_library_refresh_retry),
                                    onAction = onRefresh,
                                )
                            }
                        }
                    }
                    item {
                        AnimatedItemEntrance {
                            SectionTitle(
                                text = stringResource(id = R.string.folders_group_title, folders.size),
                            )
                        }
                    }
                    itemsIndexed(
                        items = folders,
                        key = { _, folder -> folder.id },
                    ) { index, folder ->
                        AnimatedItemEntrance {
                            GroupedListRow(
                                index = index,
                                totalCount = folders.size,
                            ) {
                                SettingsNavigationItem(
                                    icon = Icons.Outlined.FolderOpen,
                                    title = folder.name,
                                    description = stringResource(
                                        id = R.string.folder_meta_no_size,
                                        folder.videoCount,
                                        folder.totalDurationLabel,
                                    ),
                                    onClick = { onOpenFolder(folder.id) },
                                )
                            }
                        }
                    }
                    foldersState.appendErrorMessage?.let { message ->
                        item {
                            ErrorFooterBlock(
                                title = stringResource(id = R.string.media_library_append_error_title),
                                text = message.resolve(context),
                                actionLabel = stringResource(id = R.string.media_library_append_retry),
                                onAction = onLoadMore,
                            )
                        }
                    }
                    if (foldersState.isAppending) {
                        item { LoadingFooterBlock() }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

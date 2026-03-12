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
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
internal fun VideosPageContent(
    modifier: Modifier = Modifier,
    videosState: MediaCatalogState<LocalVideoItem>,
    onPlay: (PlaybackSelection) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val videos = videosState.items
    val queueEntries = remember(videos) { videos.map(LocalVideoItem::toPlaybackQueueEntry) }
    LibraryVideoListPage(
        modifier = modifier,
        state = videosState,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        videos = videos,
        emptyMessage = stringResource(id = R.string.empty_video_list),
        sectionTitle = stringResource(id = R.string.videos_group_title, videos.size),
        onPlay = { item ->
            onPlay(
                PlaybackSelection(
                    targetEntry = item.toPlaybackQueueEntry(),
                    queueEntries = queueEntries,
                ),
            )
        },
    )
}

@Composable
internal fun FolderPageContent(
    modifier: Modifier = Modifier,
    videosState: MediaCatalogState<LocalVideoItem>,
    onPlay: (PlaybackSelection) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val videos = videosState.items
    val queueEntries = remember(videos) { videos.map(LocalVideoItem::toPlaybackQueueEntry) }
    LibraryVideoListPage(
        modifier = modifier,
        state = videosState,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        videos = videos,
        emptyMessage = stringResource(id = R.string.empty_folder_video_list),
        sectionTitle = stringResource(id = R.string.selected_folder_group_title, videos.size),
        onPlay = { item ->
            onPlay(
                PlaybackSelection(
                    targetEntry = item.toPlaybackQueueEntry(),
                    queueEntries = queueEntries,
                ),
            )
        },
    )
}

@Composable
private fun LibraryVideoListPage(
    modifier: Modifier,
    state: MediaCatalogState<LocalVideoItem>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    videos: List<LocalVideoItem>,
    emptyMessage: String,
    sectionTitle: String,
    onPlay: (LocalVideoItem) -> Unit,
) {
    val context = LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState, videos.size, state.hasMore, state.isAppending, state.isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .map { lastVisibleIndex ->
                state.hasMore &&
                    !state.isAppending &&
                    !state.isLoading &&
                    lastVisibleIndex >= videos.lastIndex - 12
            }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = state.isLoading && state.hasLoadedOnce,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = state.isLoading && state.hasLoadedOnce,
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

            when {
                state.isLoading && !state.hasLoadedOnce -> {
                    item {
                        LoadingSectionBlock(
                            horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                        )
                    }
                }

                videos.isEmpty() && state.errorMessage != null -> {
                    item {
                        AnimatedItemEntrance {
                            ErrorBlock(
                                title = stringResource(id = R.string.media_library_refresh_error_title),
                                text = state.errorMessage.resolve(context),
                                actionLabel = stringResource(id = R.string.media_library_refresh_retry),
                                onAction = onRefresh,
                            )
                        }
                    }
                }

                videos.isEmpty() -> {
                    item {
                        AnimatedItemEntrance {
                            EmptyBlock(text = emptyMessage)
                        }
                    }
                }

                else -> {
                    state.errorMessage?.let { message ->
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
                            SectionTitle(text = sectionTitle)
                        }
                    }
                    itemsIndexed(
                        items = videos,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        AnimatedItemEntrance {
                            GroupedListRow(
                                index = index,
                                totalCount = videos.size,
                                horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                            ) {
                                SettingsNavigationItem(
                                icon = Icons.Outlined.VideoLibrary,
                                thumbnailUri = item.uri,
                                thumbnailId = item.id,
                                durationLabel = item.durationLabel,
                                progressFraction = item.resumeProgressFraction,
                                title = item.title,
                                description = item.folderPath,
                                onClick = { onPlay(item) },
                                )
                            }
                        }
                    }
                    state.appendErrorMessage?.let { message ->
                        item {
                            ErrorFooterBlock(
                                title = stringResource(id = R.string.media_library_append_error_title),
                                text = message.resolve(context),
                                actionLabel = stringResource(id = R.string.media_library_append_retry),
                                onAction = onLoadMore,
                            )
                        }
                    }
                    if (state.isAppending) {
                        item { LoadingFooterBlock() }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

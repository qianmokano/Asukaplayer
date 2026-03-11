package com.asuka.player.app

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R

@Composable
internal fun VideosPageContent(
    modifier: Modifier = Modifier,
    mediaLibraryState: MediaLibraryRefreshState,
    onPlay: (String, List<String>) -> Unit,
    onRefresh: () -> Unit,
) {
    val videos = mediaLibraryState.items
    val queueMediaIds = remember(videos) { videos.map { it.uri.toString() } }
    LibraryVideoListPage(
        modifier = modifier,
        isRefreshing = mediaLibraryState.isLoading && mediaLibraryState.hasLoadedOnce,
        onRefresh = onRefresh,
        videos = videos,
        errorMessage = mediaLibraryState.errorMessage,
        emptyMessage = stringResource(id = R.string.empty_video_list),
        sectionTitle = stringResource(id = R.string.videos_group_title, videos.size),
        onPlay = { mediaId -> onPlay(mediaId, queueMediaIds) },
    )
}

@Composable
internal fun FolderPageContent(
    modifier: Modifier = Modifier,
    mediaLibraryState: MediaLibraryRefreshState,
    folder: LocalVideoFolder?,
    onPlay: (String, List<String>) -> Unit,
    onRefresh: () -> Unit,
) {
    val videos = folder?.videos.orEmpty()
    val queueMediaIds = remember(folder) { videos.map { it.uri.toString() } }
    LibraryVideoListPage(
        modifier = modifier,
        isRefreshing = mediaLibraryState.isLoading && mediaLibraryState.hasLoadedOnce,
        onRefresh = onRefresh,
        videos = videos,
        errorMessage = mediaLibraryState.errorMessage,
        emptyMessage = stringResource(id = R.string.empty_video_list),
        sectionTitle = stringResource(id = R.string.selected_folder_group_title, videos.size),
        onPlay = { mediaId -> onPlay(mediaId, queueMediaIds) },
    )
}

@Composable
private fun LibraryVideoListPage(
    modifier: Modifier,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    videos: List<LocalVideoItem>,
    errorMessage: String?,
    emptyMessage: String,
    sectionTitle: String,
    onPlay: (String) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 92.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            when {
                isRefreshing && videos.isEmpty() -> {
                    item { LoadingBlock() }
                }

                videos.isEmpty() && errorMessage != null -> {
                    item {
                        ErrorBlock(
                            title = stringResource(id = R.string.media_library_refresh_error_title),
                            text = errorMessage,
                            actionLabel = stringResource(id = R.string.media_library_refresh_retry),
                            onAction = onRefresh,
                        )
                    }
                }

                videos.isEmpty() -> {
                    item { EmptyBlock(text = emptyMessage) }
                }

                else -> {
                    errorMessage?.let { message ->
                        item {
                            ErrorBlock(
                                title = stringResource(id = R.string.media_library_refresh_error_title),
                                text = message,
                                actionLabel = stringResource(id = R.string.media_library_refresh_retry),
                                onAction = onRefresh,
                            )
                        }
                    }
                    item {
                        SectionTitle(text = sectionTitle)
                    }
                    itemsIndexed(
                        items = videos,
                        key = { _, item -> item.id },
                    ) { index, item ->
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
                                title = item.title,
                                description = item.folderPath,
                                onClick = { onPlay(item.uri.toString()) },
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

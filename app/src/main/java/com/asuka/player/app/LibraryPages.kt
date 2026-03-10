package com.asuka.player.app

import android.net.Uri
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
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.asuka.player.R

@Composable
internal fun HomePageContent(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    hasLimitedMediaAccess: Boolean,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    folders: List<LocalVideoFolder>,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFolder: (Long) -> Unit,
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

            if (initialLoading) {
                item { LoadingBlock() }
            } else if (folders.isEmpty()) {
                item {
                    EmptyBlock(
                        text = if (!permissionGranted && hasLimitedMediaAccess) {
                            stringResource(id = R.string.empty_video_list_limited)
                        } else {
                            stringResource(id = R.string.empty_video_list)
                        },
                    )
                }
            } else {
                item {
                    SectionTitle(text = stringResource(id = R.string.folders_group_title, folders.size))
                }
                itemsIndexed(
                    items = folders,
                    key = { _, folder -> folder.id },
                ) { index, folder ->
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

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

@Composable
internal fun VideosPageContent(
    modifier: Modifier = Modifier,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    videos: List<LocalVideoItem>,
    onPlay: (String, List<String>) -> Unit,
    onRefresh: () -> Unit,
) {
    val queueMediaIds = remember(videos) { videos.map { it.uri.toString() } }
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

            if (initialLoading) {
                item { LoadingBlock() }
            } else if (videos.isEmpty()) {
                item { EmptyBlock(text = stringResource(id = R.string.empty_video_list)) }
            } else {
                item {
                    SectionTitle(text = stringResource(id = R.string.videos_group_title, videos.size))
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
                            onClick = { onPlay(item.uri.toString(), queueMediaIds) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

@Composable
internal fun FolderPageContent(
    modifier: Modifier = Modifier,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    folder: LocalVideoFolder?,
    onPlay: (String, List<String>) -> Unit,
    onRefresh: () -> Unit,
) {
    val queueMediaIds = remember(folder) { folder?.videos.orEmpty().map { it.uri.toString() } }
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

            if (initialLoading) {
                item { LoadingBlock() }
            } else {
                val videos = folder?.videos.orEmpty()
                if (videos.isEmpty()) {
                    item { EmptyBlock(text = stringResource(id = R.string.empty_video_list)) }
                } else {
                    item {
                        SectionTitle(text = stringResource(id = R.string.selected_folder_group_title, videos.size))
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
                                onClick = { onPlay(item.uri.toString(), queueMediaIds) },
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

@Composable
internal fun RecentPageContent(
    modifier: Modifier = Modifier,
    recentMediaIds: List<String>,
    knownVideos: Map<String, LocalVideoItem>,
    onPlay: (String, List<String>) -> Unit,
) {
    val context = LocalContext.current
    val queueMediaIds = remember(recentMediaIds) { recentMediaIds.distinct() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 92.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        if (recentMediaIds.isEmpty()) {
            item { EmptyBlock(text = stringResource(id = R.string.empty_recent_list)) }
        } else {
            item {
                SectionTitle(text = stringResource(id = R.string.recent_group_title, recentMediaIds.size))
            }
            itemsIndexed(
                items = recentMediaIds,
                key = { _, mediaId -> mediaId },
            ) { index, mediaId ->
                val known = knownVideos[mediaId]
                val uri = remember(mediaId) {
                    try {
                        Uri.parse(mediaId)
                    } catch (_: Throwable) {
                        null
                    }
                }
                val fallbackTitle = uri?.lastPathSegment ?: mediaId
                val title by produceState(initialValue = fallbackTitle, mediaId) {
                    if (uri == null) return@produceState
                    val resolved = runCatching { resolveDisplayName(context, uri) }.getOrNull()
                    value = resolved?.ifBlank { fallbackTitle } ?: fallbackTitle
                }

                GroupedListRow(
                    index = index,
                    totalCount = recentMediaIds.size,
                    horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                ) {
                    if (known != null) {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            thumbnailUri = known.uri,
                            thumbnailId = known.id,
                            durationLabel = known.durationLabel,
                            title = known.title,
                            description = known.folderPath,
                            onClick = { onPlay(known.uri.toString(), queueMediaIds) },
                        )
                    } else {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            title = title,
                            description = uri?.toString() ?: mediaId,
                            onClick = { onPlay(mediaId, queueMediaIds) },
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }
}

private fun resolveDisplayName(context: android.content.Context, uri: Uri): String? {
    val resolver = context.contentResolver
    val cursor = runCatching {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
    }.getOrNull() ?: return null
    cursor.use {
        if (!it.moveToFirst()) return null
        val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx < 0) return null
        return it.getString(idx)
    }
}

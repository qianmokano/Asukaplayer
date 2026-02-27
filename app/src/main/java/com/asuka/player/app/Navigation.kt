package com.asuka.player.app

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.asuka.player.R

internal const val ROUTE_HOME = "home"
internal const val ROUTE_ALL_VIDEOS = "all_videos"
internal const val ROUTE_SETTINGS = "settings"
internal const val ROUTE_SETTINGS_PLAYER = "settings/player"
internal const val ROUTE_SETTINGS_THEME = "settings/theme"
internal const val ROUTE_SETTINGS_MOTION = "settings/motion"
internal const val ARG_FOLDER_ID = "folderId"
internal const val ROUTE_FOLDER = "folder/{$ARG_FOLDER_ID}"

internal fun folderRoute(folderId: Long): String = "folder/$folderId"

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnterTransition(
    durationMs: Int,
): EnterTransition {
    val safeDuration = durationMs.coerceAtLeast(0)
    return fadeIn(animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing)) +
        slideInHorizontally(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            initialOffsetX = { (it * 0.08f).toInt() },
        ) +
        scaleIn(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            initialScale = 0.98f,
        )
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExitTransition(
    durationMs: Int,
): ExitTransition {
    val safeDuration = durationMs.coerceAtLeast(0)
    return fadeOut(animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            targetOffsetX = { (-it * 0.06f).toInt() },
        ) +
        scaleOut(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            targetScale = 0.985f,
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPageScaffold(
    title: String,
    showBack: Boolean,
    showSettingsAction: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
            ) {
                TopAppBar(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (showBack) {
                            Row {
                                IconButton(
                                    modifier = Modifier.size(36.dp),
                                    onClick = onBack,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.back_to_folders),
                                    )
                                }
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    actions = {
                        if (showSettingsAction) {
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    }
                                    onOpenSettings()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(id = R.string.settings_title),
                                )
                            }
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        content = content,
    )
}

@Composable
internal fun HomePageContent(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    folders: List<LocalVideoFolder>,
    onRequestPermission: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAllVideos: () -> Unit,
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
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            if (!permissionGranted) {
                item {
                    SplicedColumnGroup(title = stringResource(id = R.string.permission_required_title)) {
                        item {
                            SettingsNavigationItem(
                                icon = Icons.Rounded.Lock,
                                title = stringResource(id = R.string.grant_permission),
                                description = stringResource(id = R.string.permission_hint),
                                onClick = onRequestPermission,
                            )
                        }
                        item {
                            SettingsNavigationItem(
                                icon = Icons.Rounded.PlayCircle,
                                title = stringResource(id = R.string.open_local_video),
                                description = stringResource(id = R.string.open_video_without_permission_hint),
                                onClick = onOpenLocalVideo,
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                SplicedColumnGroup(title = stringResource(id = R.string.library_actions_title)) {
                    item {
                        SettingsNavigationItem(
                            icon = Icons.Rounded.PlayCircle,
                            title = stringResource(id = R.string.open_local_video),
                            description = stringResource(id = R.string.open_local_video_hint),
                            onClick = onOpenLocalVideo,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.size(4.dp))
            }

            if (initialLoading) {
                item { LoadingBlock() }
            } else if (folders.isEmpty()) {
                item { EmptyBlock(text = stringResource(id = R.string.empty_video_list)) }
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
    onPlay: (String) -> Unit,
    onRefresh: () -> Unit,
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
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
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
                            onClick = { onPlay(item.uri.toString()) },
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
    onPlay: (String) -> Unit,
    onRefresh: () -> Unit,
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
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
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

package com.asuka.player.app

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.asuka.player.R

internal const val ROUTE_HOME = "home"
internal const val ROUTE_ALL_VIDEOS = "all_videos"
internal const val ROUTE_RECENT = "recent"
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
    speedDialActions: List<SpeedDialAction> = emptyList(),
    content: @Composable (PaddingValues) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Box(modifier = Modifier.fillMaxSize()) {
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

        if (speedDialActions.isNotEmpty()) {
            LibrarySpeedDial(
                actions = speedDialActions,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LibrarySpeedDial(
    actions: List<SpeedDialAction>,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    val transition = updateTransition(targetState = expanded, label = "LibrarySpeedDial")
    val scrimAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = if (targetState) 170 else 90) },
        label = "LibrarySpeedDialScrim",
    ) { isExpanded -> if (isExpanded) 0.08f else 0f }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val endPadding = 18.dp
    val bottomPadding = 18.dp + bottomInset

    if (scrimAlpha > 0f) {
        Box(
            modifier = modifier
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { expanded = false } },
        )
    }

    Box(modifier = modifier) {
        FloatingActionButtonMenu(
            expanded = expanded,
            button = {
                val collapsedContainerColor = lerp(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surfaceContainerHighest,
                    0.30f,
                )
                ToggleFloatingActionButton(
                    checked = expanded,
                    onCheckedChange = { nextExpanded ->
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        expanded = nextExpanded
                    },
                    containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                        collapsedContainerColor,
                        MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    val progress = checkedProgress.coerceIn(0f, 1f)
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = lerp(
                            MaterialTheme.colorScheme.onPrimaryContainer,
                            MaterialTheme.colorScheme.onPrimary,
                            progress,
                        ),
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = endPadding, bottom = bottomPadding),
            horizontalAlignment = Alignment.End,
        ) {
            actions.forEach { action ->
                FloatingActionButtonMenuItem(
                    onClick = {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        expanded = false
                        action.onClick()
                    },
                    icon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text(text = action.label)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

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

            item {
                Spacer(modifier = Modifier.size(4.dp))
            }

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

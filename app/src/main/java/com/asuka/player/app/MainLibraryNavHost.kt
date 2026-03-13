package com.asuka.player.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.R
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.ThemeConfig
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

@Composable
internal fun MainLibraryNavHost(
    state: MainLibraryUiState,
    onPlay: (PlaybackSelection) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onEnsureFoldersLoaded: () -> Unit,
    onRefreshFolders: () -> Unit,
    onLoadMoreFolders: () -> Unit,
    onEnsureAllVideosLoaded: () -> Unit,
    onRefreshAllVideos: () -> Unit,
    onLoadMoreAllVideos: () -> Unit,
    onEnsureFolderLoaded: (Long) -> Unit,
    onRefreshFolder: (Long) -> Unit,
    onLoadMoreFolder: (Long) -> Unit,
    onRefreshRecent: () -> Unit,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit,
    onPlayerSettingsChange: (PlayerSettings) -> Unit,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onCustomThemesChange: (List<CustomThemeEntry>) -> Unit,
) {
    var backStack by rememberSaveable { mutableStateOf(listOf(ROUTE_HOME)) }
    var navigatingForward by rememberSaveable { mutableStateOf(true) }
    val currentRoute = backStack.lastOrNull() ?: ROUTE_HOME
    val currentFolderId = parseFolderId(currentRoute)
    val saveableStateHolder = rememberSaveableStateHolder()
    val slideDistance = rememberSlideDistance()
    val refreshCurrentPage = remember(currentRoute, currentFolderId) {
        when {
            currentRoute == ROUTE_HOME -> onRefreshFolders
            currentRoute == ROUTE_ALL_VIDEOS -> onRefreshAllVideos
            currentRoute.startsWith("folder/") && currentFolderId != null -> {
                { onRefreshFolder(currentFolderId) }
            }
            else -> onRefreshFolders
        }
    }
    val navigateTo: (String, Boolean) -> Unit = fun(route: String, launchSingleTop: Boolean) {
        if (launchSingleTop && currentRoute == route) return
        navigatingForward = true
        backStack = backStack + route
    }
    val speedDialActions = listOf(
        SpeedDialAction(
            icon = Icons.Rounded.PlayCircle,
            label = stringResource(id = R.string.open_local_video),
            onClick = onOpenLocalVideo,
        ),
        SpeedDialAction(
            icon = Icons.Rounded.Link,
            label = stringResource(id = R.string.open_network_stream),
            onClick = onOpenNetworkStream,
        ),
        SpeedDialAction(
            icon = Icons.Rounded.History,
            label = stringResource(id = R.string.recent_playback),
            onClick = { navigateTo(ROUTE_RECENT, true) },
        ),
        SpeedDialAction(
            icon = Icons.Rounded.Refresh,
            label = stringResource(id = R.string.refresh),
            onClick = refreshCurrentPage,
        ),
    )
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) {
            navigatingForward = false
            backStack = backStack.dropLast(1)
        }
    }

    BackHandler(enabled = backStack.size > 1, onBack = navigateBack)

    AnimatedContent(
        targetState = currentRoute,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = { materialSharedAxisX(forward = navigatingForward, slideDistance = slideDistance) },
        label = "MainLibraryScreenTransition",
    ) { route ->
        saveableStateHolder.SaveableStateProvider(route) {
            when {
                route == ROUTE_HOME -> {
                    LaunchedEffect(Unit) {
                        onEnsureFoldersLoaded()
                    }
                    LibraryPageScaffold(
                        title = stringResource(id = R.string.launcher_title),
                        showBack = false,
                        showSettingsAction = true,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                        speedDialActions = speedDialActions,
                    ) { innerPadding ->
                    HomePageContent(
                        modifier = Modifier.padding(innerPadding),
                        permissionGranted = state.permissionGranted,
                        hasLimitedMediaAccess = state.userSelectedPermissionGranted,
                        foldersState = state.foldersState,
                        onRequestPermission = onRequestPermission,
                        onRefresh = onRefreshFolders,
                        onLoadMore = onLoadMoreFolders,
                        onOpenFolder = { folderId ->
                            navigateTo(folderRoute(folderId), true)
                        },
                    )
                }
                }

                route == ROUTE_ALL_VIDEOS -> {
                    LaunchedEffect(Unit) {
                        onEnsureAllVideosLoaded()
                    }
                    LibraryPageScaffold(
                        title = stringResource(id = R.string.tab_all_videos),
                        showBack = true,
                        showSettingsAction = true,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                        speedDialActions = speedDialActions,
                    ) { innerPadding ->
                        VideosPageContent(
                            modifier = Modifier.padding(innerPadding),
                            videosState = state.allVideosState,
                            onPlay = onPlay,
                            onRefresh = onRefreshAllVideos,
                            onLoadMore = onLoadMoreAllVideos,
                        )
                    }
                }

                route == ROUTE_RECENT -> {
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                onRefreshRecent()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        observer.onStateChanged(lifecycleOwner, Lifecycle.Event.ON_RESUME)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    LibraryPageScaffold(
                        title = stringResource(id = R.string.recent_playback),
                        showBack = true,
                        showSettingsAction = true,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                        speedDialActions = speedDialActions,
                    ) { innerPadding ->
                        RecentPageContent(
                            modifier = Modifier.padding(innerPadding),
                            recentMediaIds = state.recentMediaIds,
                            knownVideos = state.recentKnownVideos,
                            onPlay = onPlay,
                        )
                    }
                }

                route.startsWith("folder/") -> {
                    val folderId = parseFolderId(route)
                    LaunchedEffect(folderId) {
                        if (folderId != null) {
                            onEnsureFolderLoaded(folderId)
                        }
                    }
                    val folder = remember(state.foldersState.items, folderId) {
                        state.foldersState.items.firstOrNull { it.id == folderId }
                    }
                    LibraryPageScaffold(
                        title = folder?.name ?: stringResource(id = R.string.launcher_title),
                        showBack = true,
                        showSettingsAction = true,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                        speedDialActions = speedDialActions,
                    ) { innerPadding ->
                        FolderPageContent(
                            modifier = Modifier.padding(innerPadding),
                            videosState = state.currentFolderVideosState,
                            onPlay = onPlay,
                            onRefresh = { if (folderId != null) onRefreshFolder(folderId) },
                            onLoadMore = { if (folderId != null) onLoadMoreFolder(folderId) },
                        )
                    }
                }

                route == ROUTE_SETTINGS -> {
                    LibraryPageScaffold(
                        title = stringResource(id = R.string.settings_title),
                        showBack = true,
                        showSettingsAction = false,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                    ) { innerPadding ->
                        SettingsPageContent(
                            modifier = Modifier.padding(innerPadding),
                            appVersion = state.appVersion,
                            hapticFeedbackEnabled = state.uiSettings.hapticFeedbackEnabled,
                            onHapticFeedbackEnabledChange = onHapticFeedbackEnabledChange,
                            onOpenPlayer = { navigateTo(ROUTE_SETTINGS_PLAYER, false) },
                            onOpenTheme = { navigateTo(ROUTE_SETTINGS_THEME, false) },
                        )
                    }
                }

                route == ROUTE_SETTINGS_PLAYER -> {
                    LibraryPageScaffold(
                        title = stringResource(R.string.settings_player_title),
                        showBack = true,
                        showSettingsAction = false,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                    ) { innerPadding ->
                        PlayerSettingsPlaceholderPageContent(
                            modifier = Modifier.padding(innerPadding),
                            playerSettings = state.playerSettings,
                            onPlayerSettingsChange = onPlayerSettingsChange,
                        )
                    }
                }

                route == ROUTE_SETTINGS_THEME -> {
                    LibraryPageScaffold(
                        title = stringResource(R.string.settings_theme_title),
                        showBack = true,
                        showSettingsAction = false,
                        onBack = navigateBack,
                        onOpenSettings = { navigateTo(ROUTE_SETTINGS, true) },
                    ) { innerPadding ->
                        ThemeSettingsPageContent(
                            modifier = Modifier.padding(innerPadding),
                            themeConfig = state.uiSettings.themeConfig,
                            customThemes = state.uiSettings.customThemes,
                            hapticsEnabled = state.uiSettings.hapticFeedbackEnabled,
                            onThemeConfigChange = onThemeConfigChange,
                            onCustomThemesChange = onCustomThemesChange,
                        )
                    }
                }

                else -> Unit
            }
        }
    }
}

private fun parseFolderId(route: String): Long? {
    if (!route.startsWith("folder/")) return null
    return route.substringAfter("folder/").toLongOrNull()
}

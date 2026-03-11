package com.asuka.player.app

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.R
import com.asuka.player.runtime.CustomThemeEntry
import com.asuka.player.runtime.ThemeConfig

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
    onNavDurationChange: (Int) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val currentFolderId = backStackEntry?.arguments?.getLong(ARG_FOLDER_ID)
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
            onClick = {
                navController.navigate(ROUTE_RECENT) {
                    launchSingleTop = true
                }
            },
        ),
        SpeedDialAction(
            icon = Icons.Rounded.Refresh,
            label = stringResource(id = R.string.refresh),
            onClick = refreshCurrentPage,
        ),
    )

    NavHost(
        navController = navController,
        startDestination = ROUTE_HOME,
        modifier = Modifier.fillMaxSize(),
        enterTransition = { pageEnterTransition(state.uiSettings.navDurationMs) },
        exitTransition = { pageExitTransition(state.uiSettings.navDurationMs) },
        popEnterTransition = { pageEnterTransition(state.uiSettings.navDurationMs) },
        popExitTransition = { pageExitTransition(state.uiSettings.navDurationMs) },
    ) {
        composable(route = ROUTE_HOME) {
            LaunchedEffect(Unit) {
                onEnsureFoldersLoaded()
            }
            LibraryPageScaffold(
                title = stringResource(id = R.string.launcher_title),
                showBack = false,
                showSettingsAction = true,
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
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
                        navController.navigate(folderRoute(folderId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }

        composable(route = ROUTE_ALL_VIDEOS) {
            LaunchedEffect(Unit) {
                onEnsureAllVideosLoaded()
            }
            LibraryPageScaffold(
                title = stringResource(id = R.string.tab_all_videos),
                showBack = true,
                showSettingsAction = true,
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
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

        composable(route = ROUTE_RECENT) {
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
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
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

        composable(
            route = ROUTE_FOLDER,
            arguments = listOf(
                navArgument(ARG_FOLDER_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val folderId = entry.arguments?.getLong(ARG_FOLDER_ID)
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
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
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

        composable(route = ROUTE_SETTINGS) {
            LibraryPageScaffold(
                title = stringResource(id = R.string.settings_title),
                showBack = true,
                showSettingsAction = false,
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
            ) { innerPadding ->
                SettingsPageContent(
                    modifier = Modifier.padding(innerPadding),
                    appVersion = state.appVersion,
                    hapticFeedbackEnabled = state.uiSettings.hapticFeedbackEnabled,
                    onHapticFeedbackEnabledChange = onHapticFeedbackEnabledChange,
                    onOpenPlayer = { navController.navigate(ROUTE_SETTINGS_PLAYER) },
                    onOpenTheme = { navController.navigate(ROUTE_SETTINGS_THEME) },
                    onOpenMotion = { navController.navigate(ROUTE_SETTINGS_MOTION) },
                )
            }
        }

        composable(route = ROUTE_SETTINGS_PLAYER) {
            LibraryPageScaffold(
                title = stringResource(R.string.settings_player_title),
                showBack = true,
                showSettingsAction = false,
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
            ) { innerPadding ->
                PlayerSettingsPlaceholderPageContent(
                    modifier = Modifier.padding(innerPadding),
                    playerSettings = state.playerSettings,
                    onPlayerSettingsChange = onPlayerSettingsChange,
                )
            }
        }

        composable(route = ROUTE_SETTINGS_THEME) {
            LibraryPageScaffold(
                title = stringResource(R.string.settings_theme_title),
                showBack = true,
                showSettingsAction = false,
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
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

        composable(route = ROUTE_SETTINGS_MOTION) {
            LibraryPageScaffold(
                title = stringResource(R.string.settings_motion_title),
                showBack = true,
                showSettingsAction = false,
                onBack = { navController.navigateUp() },
                onOpenSettings = {
                    navController.navigate(ROUTE_SETTINGS) {
                        launchSingleTop = true
                    }
                },
            ) { innerPadding ->
                MotionSettingsPageContent(
                    modifier = Modifier.padding(innerPadding),
                    navDurationMs = state.uiSettings.navDurationMs,
                    onNavDurationChange = onNavDurationChange,
                )
            }
        }
    }
}

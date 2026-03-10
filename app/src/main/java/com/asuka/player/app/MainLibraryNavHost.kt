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
import com.asuka.player.R

internal data class MainLibraryUiState(
    val appVersion: String,
    val uiSettings: UiSettingsState,
    val playerSettings: PlayerSettingsConfig,
    val permissionGranted: Boolean,
    val userSelectedPermissionGranted: Boolean,
    val loading: Boolean,
    val hasLoadedOnce: Boolean,
    val items: List<LocalVideoItem>,
    val recentMediaIds: List<String>,
)

@Composable
internal fun MainLibraryNavHost(
    state: MainLibraryUiState,
    onPlay: (String, List<String>) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onOpenNetworkStream: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshRecent: () -> Unit,
    onPrefetchFolder: (LocalVideoFolder?) -> Unit,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit,
    onPlayerSettingsChange: (PlayerSettingsConfig) -> Unit,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onCustomThemesChange: (List<CustomThemeEntry>) -> Unit,
    onNavDurationChange: (Int) -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val currentFolderId = backStackEntry?.arguments?.getLong(ARG_FOLDER_ID)
    val folders = remember(state.items) { buildFolderGroups(state.items) }
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
            onClick = onRefresh,
        ),
    )
    val selectedFolderExists = remember(folders, currentFolderId) {
        currentFolderId?.let { id -> folders.any { it.id == id } } ?: false
    }

    LaunchedEffect(currentRoute, currentFolderId, folders) {
        if (currentRoute.startsWith("folder/") && currentFolderId != null && !selectedFolderExists) {
            navController.popBackStack(route = ROUTE_HOME, inclusive = false)
        }
    }

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
                    initialLoading = state.loading && !state.hasLoadedOnce,
                    isRefreshing = state.loading && state.hasLoadedOnce,
                    folders = folders,
                    onRequestPermission = onRequestPermission,
                    onRefresh = onRefresh,
                    onOpenFolder = { folderId ->
                        val targetFolder = folders.firstOrNull { it.id == folderId }
                        onPrefetchFolder(targetFolder)
                        navController.navigate(folderRoute(folderId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }
        }

        composable(route = ROUTE_ALL_VIDEOS) {
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
                    initialLoading = state.loading && !state.hasLoadedOnce,
                    isRefreshing = state.loading && state.hasLoadedOnce,
                    videos = state.items,
                    onPlay = onPlay,
                    onRefresh = onRefresh,
                )
            }
        }

        composable(route = ROUTE_RECENT) {
            val knownByUri = remember(state.items) { state.items.associateBy { it.uri.toString() } }
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
                    knownVideos = knownByUri,
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
            val folder = folders.firstOrNull { it.id == folderId }
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
                    initialLoading = state.loading && !state.hasLoadedOnce,
                    isRefreshing = state.loading && state.hasLoadedOnce,
                    folder = folder,
                    onPlay = onPlay,
                    onRefresh = onRefresh,
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

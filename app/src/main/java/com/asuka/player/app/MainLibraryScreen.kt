package com.asuka.player.app

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asuka.player.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainLibraryScreen(onPlay: (String, PlayerSettingsConfig) -> Unit) {
    val context = LocalContext.current
    val appGraph = remember(context) { (context.applicationContext as AsuraPlayerApp).graph }
    val vm: MainLibraryViewModel = viewModel()
    val uiScope = rememberCoroutineScope()
    val appVersion = remember(context) { readAppVersion(context) }

    val themeConfig by vm.themeConfig.collectAsState()
    val customThemes by vm.customThemes.collectAsState()
    val navDurationMs by vm.navDurationMs.collectAsState()
    val hapticFeedbackEnabled by vm.hapticFeedbackEnabled.collectAsState()
    val playerSettings by vm.playerSettings.collectAsState()
    val permissionGranted by vm.permissionGranted.collectAsState()
    val userSelectedPermissionGranted by vm.userSelectedPermissionGranted.collectAsState()
    val loading by vm.loading.collectAsState()
    val hasLoadedOnce by vm.hasLoadedOnce.collectAsState()
    val items by vm.items.collectAsState()

    val invalidNetworkStreamMessage = stringResource(id = R.string.open_network_stream_invalid)

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val currentFolderId = backStackEntry?.arguments?.getLong(ARG_FOLDER_ID)

    LaunchedEffect(themeConfig.appearance) {
        val mode = when (themeConfig.appearance) {
            ThemeAppearanceMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeAppearanceMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeAppearanceMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    LaunchedEffect(permissionGranted, userSelectedPermissionGranted) {
        if (permissionGranted || userSelectedPermissionGranted) vm.scanVideos()
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        vm.onPermissionResult(result)
    }

    var networkStreamUrl by rememberSaveable { mutableStateOf("") }
    var showOpenNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onPlay(uri.toString(), playerSettings)
        }
    }

    val openLocalLabel = stringResource(id = R.string.open_local_video)
    val openNetworkLabel = stringResource(id = R.string.open_network_stream)
    val recentLabel = stringResource(id = R.string.recent_playback)
    val refreshLabel = stringResource(id = R.string.refresh)
    val speedDialActions = listOf(
        SpeedDialAction(
            icon = Icons.Rounded.PlayCircle,
            label = openLocalLabel,
            onClick = { picker.launch(arrayOf("video/*")) },
        ),
        SpeedDialAction(
            icon = Icons.Rounded.Link,
            label = openNetworkLabel,
            onClick = { showOpenNetworkStreamDialog = true },
        ),
        SpeedDialAction(
            icon = Icons.Rounded.History,
            label = recentLabel,
            onClick = {
                navController.navigate(ROUTE_RECENT) {
                    launchSingleTop = true
                }
            },
        ),
        SpeedDialAction(
            icon = Icons.Rounded.Refresh,
            label = refreshLabel,
            onClick = { vm.refresh() },
        ),
    )

    val folders = remember(items) { buildFolderGroups(items) }
    val selectedFolderExists = remember(folders, currentFolderId) {
        currentFolderId?.let { id -> folders.any { it.id == id } } ?: false
    }

    LaunchedEffect(currentRoute, currentFolderId, folders) {
        if (currentRoute.startsWith("folder/") && currentFolderId != null && !selectedFolderExists) {
            navController.popBackStack(route = ROUTE_HOME, inclusive = false)
        }
    }

    AsukaTheme(themeConfig = themeConfig) {
        CompositionLocalProvider(LocalHapticsEnabled provides hapticFeedbackEnabled) {
            if (showOpenNetworkStreamDialog) {
                OpenNetworkStreamDialog(
                    url = networkStreamUrl,
                    onUrlChange = { networkStreamUrl = it },
                    onDismiss = { showOpenNetworkStreamDialog = false },
                    onPlay = { url ->
                        val trimmed = url.trim()
                        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
                        if (trimmed.isBlank() || parsed?.scheme.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                invalidNetworkStreamMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                            return@OpenNetworkStreamDialog
                        }
                        showOpenNetworkStreamDialog = false
                        onPlay(trimmed, playerSettings)
                    },
                )
            }
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { pageEnterTransition(navDurationMs) },
                exitTransition = { pageExitTransition(navDurationMs) },
                popEnterTransition = { pageEnterTransition(navDurationMs) },
                popExitTransition = { pageExitTransition(navDurationMs) },
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
                            permissionGranted = permissionGranted,
                            hasLimitedMediaAccess = userSelectedPermissionGranted,
                            initialLoading = loading && !hasLoadedOnce,
                            isRefreshing = loading && hasLoadedOnce,
                            folders = folders,
                            onRequestPermission = { permissionLauncher.launch(videoPermissionsForRuntime()) },
                            onRefresh = { vm.refresh() },
                            onOpenAllVideos = {
                                navController.navigate(ROUTE_ALL_VIDEOS) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenFolder = { folderId ->
                                val targetFolder = folders.firstOrNull { it.id == folderId }
                                uiScope.launch(Dispatchers.IO) {
                                    prefetchFolderThumbnails(context, targetFolder, limit = 12)
                                }
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
                            initialLoading = loading && !hasLoadedOnce,
                            isRefreshing = loading && hasLoadedOnce,
                            videos = items,
                            onPlay = { mediaId -> onPlay(mediaId, playerSettings) },
                            onRefresh = { vm.refresh() },
                        )
                    }
                }

                composable(route = ROUTE_RECENT) {
                    val knownByUri = remember(items) { items.associateBy { it.uri.toString() } }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    var recentMediaIds by remember { mutableStateOf(emptyList<String>()) }
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                recentMediaIds = appGraph.playbackStateRepository.recentMediaIds(limit = 100)
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
                            recentMediaIds = recentMediaIds,
                            knownVideos = knownByUri,
                            onPlay = { mediaId -> onPlay(mediaId, playerSettings) },
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
                            initialLoading = loading && !hasLoadedOnce,
                            isRefreshing = loading && hasLoadedOnce,
                            folder = folder,
                            onPlay = { mediaId -> onPlay(mediaId, playerSettings) },
                            onRefresh = { vm.refresh() },
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
                            appVersion = appVersion,
                            hapticFeedbackEnabled = hapticFeedbackEnabled,
                            onHapticFeedbackEnabledChange = { vm.hapticFeedbackEnabled.value = it },
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
                            playerSettings = playerSettings,
                            onPlayerSettingsChange = { vm.playerSettings.value = it },
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
                            themeConfig = themeConfig,
                            customThemes = customThemes,
                            hapticsEnabled = hapticFeedbackEnabled,
                            onThemeConfigChange = { vm.themeConfig.value = it },
                            onCustomThemesChange = { vm.customThemes.value = it },
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
                            navDurationMs = navDurationMs,
                            onNavDurationChange = { vm.navDurationMs.value = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenNetworkStreamDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPlay: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.open_network_stream)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                singleLine = true,
                placeholder = { Text(text = stringResource(id = R.string.open_network_stream_hint)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onPlay(url) },
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onPlay(url) },
                enabled = url.trim().isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                Text(text = stringResource(id = R.string.dialog_play))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(text = stringResource(id = R.string.dialog_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    )
}

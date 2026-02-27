package com.asuka.player.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.asuka.player.R
import com.asuka.player.core.SeekFallbackCopier
import com.asuka.player.ui.activity.PlaybackActivity
import com.asuka.player.ui.PlayerRuntimeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var launchedForDirectPlayback = false
    private val seekFallbackCopier by lazy { SeekFallbackCopier(contentResolver, cacheDir) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val incomingData = try { intent?.data } catch (_: Throwable) { null }
        if (incomingData != null) {
            launchedForDirectPlayback = true
            setContent {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
            requestPlayback(incomingData.toString(), AppSettingsStore(this).playerSettings)
            return
        }

        setContent {
            MainLibraryScreen(
                onPlay = { mediaId, playerSettings -> requestPlayback(mediaId, playerSettings) },
            )
        }
    }

    private fun requestPlayback(mediaId: String, playerSettings: PlayerSettingsConfig) {
        lifecycleScope.launch {
            val resolvedUri = withContext(Dispatchers.IO) {
                resolveUriForPlayback(Uri.parse(mediaId))
            }
            startPlayback(resolvedUri, playerSettings)
        }
    }

    private fun startPlayback(mediaUri: Uri, playerSettings: PlayerSettingsConfig) {
        val runtimeSettings = PlayerRuntimeSettings(
            seekGestureEnabled = playerSettings.seekGestureEnabled,
            brightnessGestureEnabled = playerSettings.brightnessGestureEnabled,
            volumeGestureEnabled = playerSettings.volumeGestureEnabled,
            zoomGestureEnabled = playerSettings.zoomGestureEnabled,
            panGestureEnabled = playerSettings.panGestureEnabled,
            doubleTapGestureEnabled = playerSettings.doubleTapGestureEnabled,
            doubleTapAction = when (playerSettings.doubleTapAction) {
                DoubleTapActionSetting.Seek -> PlayerRuntimeSettings.DoubleTapAction.Seek
                DoubleTapActionSetting.TogglePlayPause -> PlayerRuntimeSettings.DoubleTapAction.TogglePlayPause
                DoubleTapActionSetting.Both -> PlayerRuntimeSettings.DoubleTapAction.Both
            },
            longPressGestureEnabled = playerSettings.longPressGestureEnabled,
            seekIncrementSec = playerSettings.seekIncrementSec,
            seekSensitivity = playerSettings.seekSensitivity,
            longPressSpeed = playerSettings.longPressSpeed,
            controllerTimeoutSec = playerSettings.controllerTimeoutSec,
            hideButtonsBackground = playerSettings.hideButtonsBackground,
            resumePlayback = playerSettings.resumePlayback,
            defaultPlaybackSpeed = playerSettings.defaultPlaybackSpeed,
            autoplay = playerSettings.autoplay,
            autoPip = playerSettings.autoPip,
            autoBackgroundPlay = playerSettings.autoBackgroundPlay,
            rememberBrightness = playerSettings.rememberBrightness,
            rememberSelections = playerSettings.rememberSelections,
        )
        val playbackIntent = Intent(this, PlaybackActivity::class.java).apply {
            data = mediaUri
            if (mediaUri.scheme == "content") {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            putExtra(PlayerRuntimeSettings.EXTRA_KEY, runtimeSettings)
        }
        startActivity(playbackIntent)
        if (launchedForDirectPlayback) {
            finish()
        }
    }

    private fun resolveUriForPlayback(sourceUri: Uri): Uri {
        if (sourceUri.scheme != "content") return sourceUri
        if (sourceUri.authority == MediaStore.AUTHORITY) return sourceUri
        if (isContentUriSeekable(sourceUri)) return sourceUri
        val copiedUri = seekFallbackCopier.copy(sourceUri)
        return copiedUri ?: sourceUri
    }

    private fun isContentUriSeekable(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                try {
                    Os.lseek(pfd.fileDescriptor, 0L, OsConstants.SEEK_CUR)
                    true
                } catch (_: ErrnoException) {
                    false
                }
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainLibraryScreen(onPlay: (String, PlayerSettingsConfig) -> Unit) {
    val context = LocalContext.current
    val vm: MainLibraryViewModel = viewModel()
    val uiScope = rememberCoroutineScope()
    val appVersion = remember(context) { readAppVersion(context) }

    val themeConfig by vm.themeConfig.collectAsState()
    val customThemes by vm.customThemes.collectAsState()
    val navDurationMs by vm.navDurationMs.collectAsState()
    val hapticFeedbackEnabled by vm.hapticFeedbackEnabled.collectAsState()
    val playerSettings by vm.playerSettings.collectAsState()
    val permissionGranted by vm.permissionGranted.collectAsState()
    val loading by vm.loading.collectAsState()
    val hasLoadedOnce by vm.hasLoadedOnce.collectAsState()
    val items by vm.items.collectAsState()

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

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) vm.scanVideos()
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        vm.onPermissionResult(result.values.any { it })
    }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onPlay(uri.toString(), playerSettings)
    }

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
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    pageEnterTransition(navDurationMs)
                },
                exitTransition = {
                    pageExitTransition(navDurationMs)
                },
                popEnterTransition = {
                    pageEnterTransition(navDurationMs)
                },
                popExitTransition = {
                    pageExitTransition(navDurationMs)
                },
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
                ) { innerPadding ->
                    HomePageContent(
                        modifier = Modifier.padding(innerPadding),
                        permissionGranted = permissionGranted,
                        initialLoading = loading && !hasLoadedOnce,
                        isRefreshing = loading && hasLoadedOnce,
                        folders = folders,
                        onRequestPermission = { permissionLauncher.launch(videoPermissionsForRuntime()) },
                        onOpenLocalVideo = { picker.launch(arrayOf("video/*")) },
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

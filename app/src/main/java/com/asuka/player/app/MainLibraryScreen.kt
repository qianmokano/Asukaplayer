package com.asuka.player.app

import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.asuka.player.runtime.ThemeAppearanceMode

@Composable
internal fun MainLibraryScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onPlay: (PlaybackSelection) -> Unit,
) {
    val context = LocalContext.current
    val vm: MainLibraryViewModel = viewModel(factory = viewModelFactory)
    val appVersion = remember(context) { readAppVersion(context) }

    val uiSettings by vm.uiSettings.collectAsState()
    val playerSettings by vm.playerSettings.collectAsState()
    val permissionGranted by vm.permissionGranted.collectAsState()
    val userSelectedPermissionGranted by vm.userSelectedPermissionGranted.collectAsState()
    val foldersState by vm.foldersState.collectAsState()
    val allVideosState by vm.allVideosState.collectAsState()
    val currentFolderId by vm.currentFolderId.collectAsState()
    val currentFolderVideosState by vm.currentFolderVideosState.collectAsState()
    val recentMediaIds by vm.recentMediaIds.collectAsState()
    val recentKnownVideos by vm.recentKnownVideos.collectAsState()

    LaunchedEffect(uiSettings.themeConfig.appearance) {
        val mode = when (uiSettings.themeConfig.appearance) {
            ThemeAppearanceMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeAppearanceMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeAppearanceMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    LaunchedEffect(permissionGranted, userSelectedPermissionGranted) {
        if (permissionGranted || userSelectedPermissionGranted) vm.ensureFoldersLoaded()
    }

    LaunchedEffect(vm, context) {
        vm.events.collect { event ->
            when (event) {
                is MainLibraryEvent.ShowMessage ->
                    Toast.makeText(context, event.message.resolve(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        vm.onPermissionResult(result)
    }
    val documentPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            onPlay(singlePlaybackSelection(uri.toString()))
        }
    }

    var networkStreamUrl by rememberSaveable { mutableStateOf("") }
    var showOpenNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }

    val screenState = rememberMainLibraryUiState(
        appVersion,
        uiSettings,
        playerSettings,
        permissionGranted,
        userSelectedPermissionGranted,
        foldersState,
        allVideosState,
        currentFolderId,
        currentFolderVideosState,
        recentMediaIds,
        recentKnownVideos,
    )

    AsukaTheme(themeConfig = uiSettings.themeConfig) {
        CompositionLocalProvider(LocalHapticsEnabled provides uiSettings.hapticFeedbackEnabled) {
            if (showOpenNetworkStreamDialog) {
                OpenNetworkStreamDialog(
                    url = networkStreamUrl,
                    onUrlChange = { networkStreamUrl = it },
                    onDismiss = { showOpenNetworkStreamDialog = false },
                    onPlay = { url ->
                        val trimmed = vm.validateNetworkStreamUrl(url) ?: return@OpenNetworkStreamDialog
                        showOpenNetworkStreamDialog = false
                        onPlay(singlePlaybackSelection(trimmed))
                    },
                )
            }

            MainLibraryNavHost(
                state = screenState,
                onPlay = onPlay,
                onRequestPermission = { permissionLauncher.launch(videoPermissionsForRuntime()) },
                onOpenLocalVideo = { documentPicker.launch(arrayOf("video/*")) },
                onOpenNetworkStream = { showOpenNetworkStreamDialog = true },
                onEnsureFoldersLoaded = vm::ensureFoldersLoaded,
                onRefreshFolders = vm::refreshFolders,
                onLoadMoreFolders = vm::loadMoreFolders,
                onEnsureAllVideosLoaded = vm::ensureAllVideosLoaded,
                onRefreshAllVideos = vm::refreshAllVideos,
                onLoadMoreAllVideos = vm::loadMoreAllVideos,
                onEnsureFolderLoaded = vm::ensureFolderLoaded,
                onRefreshFolder = vm::refreshFolder,
                onLoadMoreFolder = vm::loadMoreFolder,
                onRefreshRecent = { vm.refreshRecentMediaIds() },
                onHapticFeedbackEnabledChange = { vm.setHapticFeedbackEnabled(it) },
                onPlayerSettingsChange = { vm.setPlayerSettings(it) },
                onThemeConfigChange = { vm.setThemeConfig(it) },
                onCustomThemesChange = { vm.setCustomThemes(it) },
            )
        }
    }
}

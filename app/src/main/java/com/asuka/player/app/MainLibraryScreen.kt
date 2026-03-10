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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun MainLibraryScreen(onPlay: (String, List<String>) -> Unit) {
    val context = LocalContext.current
    val vm: MainLibraryViewModel = viewModel()
    val uiScope = rememberCoroutineScope()
    val appVersion = remember(context) { readAppVersion(context) }

    val uiSettings by vm.uiSettings.collectAsState()
    val playerSettings by vm.playerSettings.collectAsState()
    val permissionGranted by vm.permissionGranted.collectAsState()
    val userSelectedPermissionGranted by vm.userSelectedPermissionGranted.collectAsState()
    val loading by vm.loading.collectAsState()
    val hasLoadedOnce by vm.hasLoadedOnce.collectAsState()
    val items by vm.items.collectAsState()
    val recentMediaIds by vm.recentMediaIds.collectAsState()

    LaunchedEffect(uiSettings.themeConfig.appearance) {
        val mode = when (uiSettings.themeConfig.appearance) {
            ThemeAppearanceMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeAppearanceMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeAppearanceMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    LaunchedEffect(permissionGranted, userSelectedPermissionGranted) {
        if (permissionGranted || userSelectedPermissionGranted) vm.scanVideos()
    }

    LaunchedEffect(vm, context) {
        vm.events.collect { event ->
            when (event) {
                is MainLibraryEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
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
            onPlay(uri.toString(), emptyList())
        }
    }

    var networkStreamUrl by rememberSaveable { mutableStateOf("") }
    var showOpenNetworkStreamDialog by rememberSaveable { mutableStateOf(false) }

    val screenState = remember(
        appVersion,
        uiSettings,
        playerSettings,
        permissionGranted,
        userSelectedPermissionGranted,
        loading,
        hasLoadedOnce,
        items,
        recentMediaIds,
    ) {
        MainLibraryUiState(
            appVersion = appVersion,
            uiSettings = uiSettings,
            playerSettings = playerSettings,
            permissionGranted = permissionGranted,
            userSelectedPermissionGranted = userSelectedPermissionGranted,
            loading = loading,
            hasLoadedOnce = hasLoadedOnce,
            items = items,
            recentMediaIds = recentMediaIds,
        )
    }

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
                        onPlay(trimmed, emptyList())
                    },
                )
            }

            MainLibraryNavHost(
                state = screenState,
                onPlay = onPlay,
                onRequestPermission = { permissionLauncher.launch(videoPermissionsForRuntime()) },
                onOpenLocalVideo = { documentPicker.launch(arrayOf("video/*")) },
                onOpenNetworkStream = { showOpenNetworkStreamDialog = true },
                onRefresh = { vm.refresh() },
                onRefreshRecent = { vm.refreshRecentMediaIds() },
                onPrefetchFolder = { folder ->
                    uiScope.launch(Dispatchers.IO) {
                        prefetchFolderThumbnails(context, folder, limit = 12)
                    }
                },
                onHapticFeedbackEnabledChange = { vm.setHapticFeedbackEnabled(it) },
                onPlayerSettingsChange = { vm.setPlayerSettings(it) },
                onThemeConfigChange = { vm.setThemeConfig(it) },
                onCustomThemesChange = { vm.setCustomThemes(it) },
                onNavDurationChange = { vm.setNavDurationMs(it) },
            )
        }
    }
}

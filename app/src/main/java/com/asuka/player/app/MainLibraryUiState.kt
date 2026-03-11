package com.asuka.player.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.runtime.UiSettingsState

internal data class MainLibraryUiState(
    val appVersion: String,
    val uiSettings: UiSettingsState,
    val playerSettings: PlayerSettings,
    val permissionGranted: Boolean,
    val userSelectedPermissionGranted: Boolean,
    val mediaLibraryState: MediaLibraryRefreshState,
    val recentMediaIds: List<String>,
)

@Composable
internal fun rememberMainLibraryUiState(
    appVersion: String,
    uiSettings: UiSettingsState,
    playerSettings: PlayerSettings,
    permissionGranted: Boolean,
    userSelectedPermissionGranted: Boolean,
    mediaLibraryState: MediaLibraryRefreshState,
    recentMediaIds: List<String>,
): MainLibraryUiState {
    return remember(
        appVersion,
        uiSettings,
        playerSettings,
        permissionGranted,
        userSelectedPermissionGranted,
        mediaLibraryState,
        recentMediaIds,
    ) {
        MainLibraryUiState(
            appVersion = appVersion,
            uiSettings = uiSettings,
            playerSettings = playerSettings,
            permissionGranted = permissionGranted,
            userSelectedPermissionGranted = userSelectedPermissionGranted,
            mediaLibraryState = mediaLibraryState,
            recentMediaIds = recentMediaIds,
        )
    }
}

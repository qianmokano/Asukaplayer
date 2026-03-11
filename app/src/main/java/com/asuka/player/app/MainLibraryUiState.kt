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
    val foldersState: MediaCatalogState<LocalVideoFolder>,
    val allVideosState: MediaCatalogState<LocalVideoItem>,
    val currentFolderId: Long?,
    val currentFolderVideosState: MediaCatalogState<LocalVideoItem>,
    val recentMediaIds: List<String>,
    val recentKnownVideos: Map<String, LocalVideoItem>,
)

@Composable
internal fun rememberMainLibraryUiState(
    appVersion: String,
    uiSettings: UiSettingsState,
    playerSettings: PlayerSettings,
    permissionGranted: Boolean,
    userSelectedPermissionGranted: Boolean,
    foldersState: MediaCatalogState<LocalVideoFolder>,
    allVideosState: MediaCatalogState<LocalVideoItem>,
    currentFolderId: Long?,
    currentFolderVideosState: MediaCatalogState<LocalVideoItem>,
    recentMediaIds: List<String>,
    recentKnownVideos: Map<String, LocalVideoItem>,
): MainLibraryUiState {
    return remember(
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
    ) {
        MainLibraryUiState(
            appVersion = appVersion,
            uiSettings = uiSettings,
            playerSettings = playerSettings,
            permissionGranted = permissionGranted,
            userSelectedPermissionGranted = userSelectedPermissionGranted,
            foldersState = foldersState,
            allVideosState = allVideosState,
            currentFolderId = currentFolderId,
            currentFolderVideosState = currentFolderVideosState,
            recentMediaIds = recentMediaIds,
            recentKnownVideos = recentKnownVideos,
        )
    }
}

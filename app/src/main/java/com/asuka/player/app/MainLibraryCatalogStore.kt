package com.asuka.player.app

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MainLibraryCatalogStore(
    resolveVideoAccessUseCase: ResolveVideoAccessUseCase,
    loadFolderPageUseCase: LoadFolderPageUseCase,
    private val loadVideoPageUseCase: LoadVideoPageUseCase,
    loadRecentMediaIdsUseCase: LoadRecentMediaIdsUseCase,
    resolveRecentMediaItemsUseCase: ResolveRecentMediaItemsUseCase,
    observeMediaLibraryChangesUseCase: ObserveMediaLibraryChangesUseCase,
    private val scope: CoroutineScope,
    private val publishMessage: (MainLibraryText) -> Unit,
) {
    private val initialVideoAccessState = resolveVideoAccessUseCase()
    private val resolveVideoAccess = resolveVideoAccessUseCase

    private val _permissionGranted = MutableStateFlow(initialVideoAccessState.permissionGranted)
    val permissionGranted = _permissionGranted.asStateFlow()

    private val _userSelectedPermissionGranted =
        MutableStateFlow(initialVideoAccessState.userSelectedPermissionGranted)
    val userSelectedPermissionGranted = _userSelectedPermissionGranted.asStateFlow()

    private val foldersSlice = MainLibraryFoldersSlice(
        loadFolderPageUseCase = loadFolderPageUseCase,
        scope = scope,
        canReadLibrary = ::canReadLibrary,
        handlePermissionDenied = ::updateVideoAccessState,
        publishMessage = publishMessage,
    )
    private val allVideosSlice = MainLibraryAllVideosSlice(
        loadVideoPageUseCase = loadVideoPageUseCase,
        scope = scope,
        canReadLibrary = ::canReadLibrary,
        handlePermissionDenied = ::updateVideoAccessState,
        publishMessage = publishMessage,
    )
    private val currentFolderSlice = MainLibraryCurrentFolderSlice(
        loadVideoPageUseCase = loadVideoPageUseCase,
        scope = scope,
        canReadLibrary = ::canReadLibrary,
        handlePermissionDenied = ::updateVideoAccessState,
        publishMessage = publishMessage,
    )
    private val recentSlice = MainLibraryRecentSlice(
        loadRecentMediaIdsUseCase = loadRecentMediaIdsUseCase,
        resolveRecentMediaItemsUseCase = resolveRecentMediaItemsUseCase,
        scope = scope,
    )

    val foldersState = foldersSlice.state
    val allVideosState = allVideosSlice.state
    val currentFolderId = currentFolderSlice.currentFolderId
    val currentFolderVideosState = currentFolderSlice.state
    val recentMediaIds = recentSlice.recentMediaIds
    val recentKnownVideos = recentSlice.recentKnownVideos

    init {
        scope.launch {
            observeMediaLibraryChangesUseCase().collect {
                refreshLoadedCatalogsFromIndex()
            }
        }
    }

    fun onPermissionResult() {
        updateVideoAccessAndReloadCatalogs(forceReload = true)
    }

    fun refreshVideoAccessState() {
        val wasLimited = _userSelectedPermissionGranted.value
        val accessChanged = updateVideoAccessState()
        // Android 14 can change the selected-media set without changing either
        // permission boolean. Rebuild the index whenever limited access is active.
        if (canReadLibrary() && (accessChanged || wasLimited || _userSelectedPermissionGranted.value)) {
            reloadCatalogsForAccessChange()
        }
    }

    fun ensureFoldersLoaded() = foldersSlice.ensureLoaded()

    fun refreshFolders() = foldersSlice.refresh()

    fun loadMoreFolders() = foldersSlice.loadMore()

    fun ensureAllVideosLoaded() = allVideosSlice.ensureLoaded()

    fun refreshAllVideos() = allVideosSlice.refresh()

    fun loadMoreAllVideos() = allVideosSlice.loadMore()

    fun ensureFolderLoaded(folderId: Long) {
        currentFolderSlice.ensureLoaded(folderId)
    }

    fun refreshFolder(folderId: Long) {
        currentFolderSlice.refresh(folderId)
    }

    fun loadMoreFolder(folderId: Long) = currentFolderSlice.loadMore(folderId)

    fun refreshRecentMediaIds() = recentSlice.refresh()

    fun validateNetworkStreamUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val parsed = runCatching { trimmed.toUri() }.getOrNull()
        if (trimmed.isBlank() || !parsed.isSupportedNetworkStreamUri()) {
            publishMessage(MainLibraryText.OpenNetworkStreamInvalid)
            return null
        }
        return trimmed
    }

    private fun refreshLoadedCatalogsFromIndex() {
        if (!canReadLibrary()) return
        foldersSlice.refreshLoadedFromIndex()
        allVideosSlice.refreshLoadedFromIndex()
        recentSlice.refreshIfLoaded()
        currentFolderSlice.refreshLoadedFromIndex()
    }

    private fun updateVideoAccessAndReloadCatalogs(forceReload: Boolean) {
        val accessChanged = updateVideoAccessState()
        if (canReadLibrary() && (forceReload || accessChanged)) {
            reloadCatalogsForAccessChange()
        }
    }

    private fun reloadCatalogsForAccessChange() {
        val reloadFolders = foldersSlice.state.value.hasLoadedOnce
        val reloadAllVideos = allVideosSlice.state.value.hasLoadedOnce
        val reloadFolderId = currentFolderSlice.currentFolderId.value
            ?.takeIf { currentFolderSlice.state.value.hasLoadedOnce }
        if (reloadFolders) {
            foldersSlice.resetForAccessChange()
            ensureFoldersLoaded()
        }
        if (reloadAllVideos) {
            allVideosSlice.resetForAccessChange()
            ensureAllVideosLoaded()
        }
        if (reloadFolderId != null) {
            currentFolderSlice.resetForAccessChange()
            refreshFolder(reloadFolderId)
        }
        recentSlice.refreshIfLoaded()
    }

    private fun updateVideoAccessState(): Boolean {
        val previous = VideoAccessState(
            permissionGranted = _permissionGranted.value,
            userSelectedPermissionGranted = _userSelectedPermissionGranted.value,
        )
        val accessState = resolveVideoAccess()
        _permissionGranted.value = accessState.permissionGranted
        _userSelectedPermissionGranted.value = accessState.userSelectedPermissionGranted
        if (!canReadLibrary()) {
            foldersSlice.resetForPermissionLoss()
            allVideosSlice.resetForPermissionLoss()
            currentFolderSlice.resetForPermissionLoss()
            recentSlice.resetForPermissionLoss()
            return previous != accessState
        }
        return previous != accessState
    }

    private fun canReadLibrary(): Boolean {
        return _permissionGranted.value || _userSelectedPermissionGranted.value
    }
}

private fun Uri?.isSupportedNetworkStreamUri(): Boolean {
    val uri = this ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme !in SUPPORTED_NETWORK_STREAM_SCHEMES) return false
    if (uri.host.isNullOrBlank()) return false
    return when (scheme) {
        "rtsp" -> true
        else -> !uri.path.isNullOrBlank() && uri.path != "/"
    }
}

private val SUPPORTED_NETWORK_STREAM_SCHEMES = setOf("http", "https", "rtsp")

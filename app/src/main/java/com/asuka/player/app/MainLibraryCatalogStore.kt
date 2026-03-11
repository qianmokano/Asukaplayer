package com.asuka.player.app

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal class MainLibraryCatalogStore(
    resolveVideoAccessUseCase: ResolveVideoAccessUseCase,
    private val loadFolderPageUseCase: LoadFolderPageUseCase,
    private val loadVideoPageUseCase: LoadVideoPageUseCase,
    private val loadRecentMediaIdsUseCase: LoadRecentMediaIdsUseCase,
    private val resolveRecentMediaItemsUseCase: ResolveRecentMediaItemsUseCase,
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

    private val _foldersState = MutableStateFlow(MediaCatalogState<LocalVideoFolder>())
    val foldersState = _foldersState.asStateFlow()

    private val _allVideosState = MutableStateFlow(MediaCatalogState<LocalVideoItem>())
    val allVideosState = _allVideosState.asStateFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId = _currentFolderId.asStateFlow()

    private val _currentFolderVideosState = MutableStateFlow(MediaCatalogState<LocalVideoItem>())
    val currentFolderVideosState = _currentFolderVideosState.asStateFlow()

    private val _recentMediaIds = MutableStateFlow(emptyList<String>())
    val recentMediaIds = _recentMediaIds.asStateFlow()

    private val _recentKnownVideos = MutableStateFlow<Map<String, LocalVideoItem>>(emptyMap())
    val recentKnownVideos = _recentKnownVideos.asStateFlow()

    init {
        scope.launch {
            observeMediaLibraryChangesUseCase().collect {
                refreshLoadedCatalogsFromIndex()
            }
        }
    }

    fun onPermissionResult() {
        syncVideoAccessState()
    }

    fun ensureFoldersLoaded() {
        if (!canReadLibrary()) return
        if (_foldersState.value.hasLoadedOnce || _foldersState.value.isLoading) return
        loadFolders(offset = 0, syncIndex = true, publishFeedback = false)
    }

    fun refreshFolders() {
        if (!canReadLibrary() || _foldersState.value.isLoading) return
        loadFolders(offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMoreFolders() {
        val state = _foldersState.value
        if (!canReadLibrary() || state.isLoading || state.isAppending || !state.hasMore) return
        loadFolders(offset = state.nextOffset, syncIndex = false, publishFeedback = false)
    }

    fun ensureAllVideosLoaded() {
        if (!canReadLibrary()) return
        if (_allVideosState.value.hasLoadedOnce || _allVideosState.value.isLoading) return
        loadAllVideos(offset = 0, syncIndex = true, publishFeedback = false)
    }

    fun refreshAllVideos() {
        if (!canReadLibrary() || _allVideosState.value.isLoading) return
        loadAllVideos(offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMoreAllVideos() {
        val state = _allVideosState.value
        if (!canReadLibrary() || state.isLoading || state.isAppending || !state.hasMore) return
        loadAllVideos(offset = state.nextOffset, syncIndex = false, publishFeedback = false)
    }

    fun ensureFolderLoaded(folderId: Long) {
        if (!canReadLibrary()) return
        if (_currentFolderId.value != folderId) {
            _currentFolderId.value = folderId
            _currentFolderVideosState.value = MediaCatalogState()
        }
        val state = _currentFolderVideosState.value
        if (state.hasLoadedOnce || state.isLoading) return
        loadFolderVideos(folderId = folderId, offset = 0, syncIndex = true, publishFeedback = false)
    }

    fun refreshFolder(folderId: Long) {
        if (!canReadLibrary()) return
        if (_currentFolderId.value != folderId) {
            _currentFolderId.value = folderId
        }
        if (_currentFolderVideosState.value.isLoading) return
        loadFolderVideos(folderId = folderId, offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMoreFolder(folderId: Long) {
        val state = _currentFolderVideosState.value
        if (!canReadLibrary() || _currentFolderId.value != folderId) return
        if (state.isLoading || state.isAppending || !state.hasMore) return
        loadFolderVideos(
            folderId = folderId,
            offset = state.nextOffset,
            syncIndex = false,
            publishFeedback = false,
        )
    }

    fun refreshRecentMediaIds() {
        scope.launch(Dispatchers.IO) {
            val ids = loadRecentMediaIdsUseCase(limit = 100)
            val knownVideos = runCatching { resolveRecentMediaItemsUseCase(ids) }
                .getOrDefault(emptyMap())
            _recentMediaIds.value = ids
            _recentKnownVideos.value = knownVideos
        }
    }

    fun validateNetworkStreamUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
        if (trimmed.isBlank() || parsed?.scheme.isNullOrBlank()) {
            publishMessage(MainLibraryText.OpenNetworkStreamInvalid)
            return null
        }
        return trimmed
    }

    private fun loadFolders(
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        scope.launch {
            val previous = _foldersState.value
            _foldersState.value = previous.beginLoad(offset)

            when (
                val outcome = loadFolderPageUseCase(
                    offset = offset,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    _foldersState.value = previous.applyPage(page = outcome.page, append = offset > 0)
                    publishRefreshMessage(previous.hasLoadedOnce, offset, outcome.page.items.size, publishFeedback)
                }
                is MediaCatalogOutcome.Failure -> {
                    handleFolderFailure(outcome.reason, previous, offset) { _foldersState.value = it }
                }
            }
        }
    }

    private fun loadAllVideos(
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        scope.launch {
            val previous = _allVideosState.value
            _allVideosState.value = previous.beginLoad(offset)

            when (
                val outcome = loadVideoPageUseCase(
                    offset = offset,
                    folderId = null,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    _allVideosState.value = previous.applyPage(page = outcome.page, append = offset > 0)
                    warmupThumbnails(outcome.warmupVideos)
                    publishRefreshMessage(previous.hasLoadedOnce, offset, outcome.page.items.size, publishFeedback)
                }
                is MediaCatalogOutcome.Failure -> {
                    handleVideoFailure(outcome.reason, previous, offset) { _allVideosState.value = it }
                }
            }
        }
    }

    private fun loadFolderVideos(
        folderId: Long,
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        scope.launch {
            val previous = _currentFolderVideosState.value
            _currentFolderVideosState.value = previous.beginLoad(offset)

            when (
                val outcome = loadVideoPageUseCase(
                    offset = offset,
                    folderId = folderId,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    _currentFolderVideosState.value = previous.applyPage(page = outcome.page, append = offset > 0)
                    warmupThumbnails(outcome.warmupVideos)
                    publishRefreshMessage(previous.hasLoadedOnce, offset, outcome.page.items.size, publishFeedback)
                }
                is MediaCatalogOutcome.Failure -> {
                    handleVideoFailure(outcome.reason, previous, offset) { _currentFolderVideosState.value = it }
                }
            }
        }
    }

    private fun publishRefreshMessage(
        hasLoadedOnce: Boolean,
        offset: Int,
        itemCount: Int,
        publishFeedback: Boolean,
    ) {
        if (publishFeedback && offset == 0 && hasLoadedOnce) {
            publishMessage(MainLibraryText.RefreshDone(itemCount))
        }
    }

    private fun refreshLoadedCatalogsFromIndex() {
        if (!canReadLibrary()) return
        if (_foldersState.value.hasLoadedOnce && !_foldersState.value.isLoading && !_foldersState.value.isAppending) {
            loadFolders(offset = 0, syncIndex = false, publishFeedback = false)
        }
        if (_allVideosState.value.hasLoadedOnce && !_allVideosState.value.isLoading && !_allVideosState.value.isAppending) {
            loadAllVideos(offset = 0, syncIndex = false, publishFeedback = false)
        }
        val folderId = _currentFolderId.value
        if (
            folderId != null &&
            _currentFolderVideosState.value.hasLoadedOnce &&
            !_currentFolderVideosState.value.isLoading &&
            !_currentFolderVideosState.value.isAppending
        ) {
            loadFolderVideos(
                folderId = folderId,
                offset = 0,
                syncIndex = false,
                publishFeedback = false,
            )
        }
        if (_recentMediaIds.value.isNotEmpty()) {
            refreshRecentMediaIds()
        }
    }

    private fun warmupThumbnails(videos: List<LocalVideoItem>) {
        if (videos.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            runCatching {
                loadVideoPageUseCase.warmupInitialThumbnails(videos)
            }
        }
    }

    private fun handleFolderFailure(
        failure: MediaCatalogFailure,
        currentState: MediaCatalogState<LocalVideoFolder>,
        offset: Int,
        setState: (MediaCatalogState<LocalVideoFolder>) -> Unit,
    ) {
        if (failure == MediaCatalogFailure.PermissionDenied && resetStatesIfPermissionLost(setState)) {
            return
        }
        val message = failure.toText()
        setState(currentState.applyFailure(offset = offset, message = message))
    }

    private fun handleVideoFailure(
        failure: MediaCatalogFailure,
        currentState: MediaCatalogState<LocalVideoItem>,
        offset: Int,
        setState: (MediaCatalogState<LocalVideoItem>) -> Unit,
    ) {
        if (failure == MediaCatalogFailure.PermissionDenied && resetStatesIfPermissionLost(setState)) {
            return
        }
        val message = failure.toText()
        setState(currentState.applyFailure(offset = offset, message = message))
    }

    private fun <T> resetStatesIfPermissionLost(
        setState: (MediaCatalogState<T>) -> Unit,
    ): Boolean {
        syncVideoAccessState()
        if (!canReadLibrary()) {
            setState(MediaCatalogState(hasMore = false))
            return true
        }
        return false
    }

    private fun syncVideoAccessState() {
        val accessState = resolveVideoAccess()
        _permissionGranted.value = accessState.permissionGranted
        _userSelectedPermissionGranted.value = accessState.userSelectedPermissionGranted
        if (!canReadLibrary()) {
            _foldersState.value = MediaCatalogState(hasMore = false)
            _allVideosState.value = MediaCatalogState(hasMore = false)
            _currentFolderId.value = null
            _currentFolderVideosState.value = MediaCatalogState(hasMore = false)
        }
    }

    private fun canReadLibrary(): Boolean {
        return _permissionGranted.value || _userSelectedPermissionGranted.value
    }
}

internal fun <T> MediaCatalogState<T>.beginLoad(offset: Int): MediaCatalogState<T> {
    return copy(
        status = if (offset == 0) MediaCatalogStatus.Loading else MediaCatalogStatus.Appending,
        errorMessage = if (offset == 0) null else errorMessage,
        appendErrorMessage = if (offset == 0) null else null,
    )
}

internal fun <T> MediaCatalogState<T>.applyPage(
    page: MediaLibraryPage<T>,
    append: Boolean,
): MediaCatalogState<T> {
    val mergedItems = if (append) items + page.items else page.items
    return copy(
        items = mergedItems,
        status = MediaCatalogStatus.Idle,
        hasLoadedOnce = true,
        hasMore = page.nextOffset != null,
        nextOffset = page.nextOffset ?: mergedItems.size,
        errorMessage = null,
        appendErrorMessage = null,
    )
}

internal fun <T> MediaCatalogState<T>.applyFailure(
    offset: Int,
    message: MainLibraryText,
): MediaCatalogState<T> {
    return copy(
        status = MediaCatalogStatus.Idle,
        errorMessage = if (offset == 0) message else errorMessage,
        appendErrorMessage = if (offset > 0) message else null,
    )
}

internal fun MediaCatalogFailure.toText(): MainLibraryText {
    return when (this) {
        MediaCatalogFailure.PermissionDenied -> MainLibraryText.MediaLibraryPermissionDenied
        MediaCatalogFailure.ProviderUnavailable -> MainLibraryText.MediaLibraryProviderUnavailable
        MediaCatalogFailure.Unknown -> MainLibraryText.MediaLibraryUnknown
    }
}

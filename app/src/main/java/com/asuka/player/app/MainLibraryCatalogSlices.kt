package com.asuka.player.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MainLibraryFoldersSlice(
    private val loadFolderPageUseCase: LoadFolderPageUseCase,
    private val scope: CoroutineScope,
    private val canReadLibrary: () -> Boolean,
    private val handlePermissionDenied: () -> Boolean,
    private val publishMessage: (MainLibraryText) -> Unit,
) {
    private val requestTracker = RequestTracker()
    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoFolder>())
    val state: StateFlow<MediaCatalogState<LocalVideoFolder>> = _state.asStateFlow()

    fun ensureLoaded() {
        if (!canReadLibrary() || state.value.hasLoadedOnce || state.value.isLoading) return
        loadInitial()
    }

    fun refresh() {
        val current = state.value
        if (!canReadLibrary() || current.isLoading || current.isAppending) return
        load(offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMore() {
        val current = state.value
        if (!canReadLibrary() || current.isLoading || current.isAppending || !current.hasMore) return
        load(offset = current.nextOffset, syncIndex = false, publishFeedback = false)
    }

    fun refreshLoadedFromIndex() {
        val current = state.value
        if (current.hasLoadedOnce && !current.isLoading && !current.isAppending) {
            load(offset = 0, syncIndex = false, publishFeedback = false)
        }
    }

    fun resetForPermissionLoss() {
        requestTracker.invalidate()
        _state.value = MediaCatalogState(hasMore = false)
    }

    fun resetForAccessChange() {
        requestTracker.invalidate()
        _state.value = MediaCatalogState()
    }

    private fun loadInitial() {
        val requestToken = requestTracker.next()
        val previous = state.value
        _state.value = previous.beginLoad(offset = 0)
        scope.launch {
            when (
                val cachedOutcome = loadFolderPageUseCase(
                    offset = 0,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = false,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    val hasCachedResults = cachedOutcome.page.items.isNotEmpty() ||
                        (cachedOutcome.page.totalCount ?: 0) > 0
                    if (hasCachedResults) {
                        _state.value = previous.applyPage(page = cachedOutcome.page, append = false)
                    }
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    load(
                        offset = 0,
                        syncIndex = true,
                        publishFeedback = false,
                        forceFullRescan = true,
                    )
                }

                is MediaCatalogOutcome.Failure -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    handleFailure(cachedOutcome.reason, previous, 0)
                }
            }
        }
    }

    private fun load(
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
        forceFullRescan: Boolean? = null,
    ) {
        val requestToken = requestTracker.next()
        val previous = state.value
        _state.value = previous.beginLoad(offset)
        scope.launch {
            when (
                val outcome = loadFolderPageUseCase(
                    offset = offset,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                    forceFullRescan = forceFullRescan ?: !previous.hasLoadedOnce,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    _state.value = previous.applyPage(page = outcome.page, append = offset > 0)
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    publishRefreshMessage(
                        hasLoadedOnce = previous.hasLoadedOnce,
                        offset = offset,
                        totalCount = outcome.page.totalCount,
                        fallbackItemCount = outcome.page.items.size,
                        publishFeedback = publishFeedback,
                        publishMessage = publishMessage,
                    )
                }

                is MediaCatalogOutcome.Failure -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    handleFailure(outcome.reason, previous, offset)
                }
            }
        }
    }

    private fun handleFailure(
        failure: MediaCatalogFailure,
        currentState: MediaCatalogState<LocalVideoFolder>,
        offset: Int,
    ) {
        if (failure == MediaCatalogFailure.PermissionDenied && handlePermissionDenied()) {
            return
        }
        _state.value = currentState.applyFailure(offset = offset, message = failure.toText())
    }
}

internal class MainLibraryAllVideosSlice(
    private val loadVideoPageUseCase: LoadVideoPageUseCase,
    private val scope: CoroutineScope,
    private val canReadLibrary: () -> Boolean,
    private val handlePermissionDenied: () -> Boolean,
    private val publishMessage: (MainLibraryText) -> Unit,
) {
    private val requestTracker = RequestTracker()
    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoItem>())
    val state: StateFlow<MediaCatalogState<LocalVideoItem>> = _state.asStateFlow()

    fun ensureLoaded() {
        if (!canReadLibrary() || state.value.hasLoadedOnce || state.value.isLoading) return
        load(offset = 0, syncIndex = true, publishFeedback = false)
    }

    fun refresh() {
        val current = state.value
        if (!canReadLibrary() || current.isLoading || current.isAppending) return
        load(offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMore() {
        val current = state.value
        if (!canReadLibrary() || current.isLoading || current.isAppending || !current.hasMore) return
        load(offset = current.nextOffset, syncIndex = false, publishFeedback = false)
    }

    fun refreshLoadedFromIndex() {
        val current = state.value
        if (current.hasLoadedOnce && !current.isLoading && !current.isAppending) {
            load(offset = 0, syncIndex = false, publishFeedback = false)
        }
    }

    fun resetForPermissionLoss() {
        requestTracker.invalidate()
        _state.value = MediaCatalogState(hasMore = false)
    }

    fun resetForAccessChange() {
        requestTracker.invalidate()
        _state.value = MediaCatalogState()
    }

    private fun load(
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        val requestToken = requestTracker.next()
        val previous = state.value
        _state.value = previous.beginLoad(offset)
        scope.launch {
            when (
                val outcome = loadVideoPageUseCase(
                    offset = offset,
                    folderId = null,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    _state.value = previous.applyPage(page = outcome.page, append = offset > 0)
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    warmupInitialThumbnails(scope, loadVideoPageUseCase, outcome.warmupVideos)
                    publishRefreshMessage(
                        hasLoadedOnce = previous.hasLoadedOnce,
                        offset = offset,
                        totalCount = outcome.page.totalCount,
                        fallbackItemCount = outcome.page.items.size,
                        publishFeedback = publishFeedback,
                        publishMessage = publishMessage,
                    )
                }

                is MediaCatalogOutcome.Failure -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    handleFailure(outcome.reason, previous, offset)
                }
            }
        }
    }

    private fun handleFailure(
        failure: MediaCatalogFailure,
        currentState: MediaCatalogState<LocalVideoItem>,
        offset: Int,
    ) {
        if (failure == MediaCatalogFailure.PermissionDenied && handlePermissionDenied()) {
            return
        }
        _state.value = currentState.applyFailure(offset = offset, message = failure.toText())
    }
}

internal class MainLibraryCurrentFolderSlice(
    private val loadVideoPageUseCase: LoadVideoPageUseCase,
    private val scope: CoroutineScope,
    private val canReadLibrary: () -> Boolean,
    private val handlePermissionDenied: () -> Boolean,
    private val publishMessage: (MainLibraryText) -> Unit,
) {
    private val requestTracker = RequestTracker()
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoItem>())
    val state: StateFlow<MediaCatalogState<LocalVideoItem>> = _state.asStateFlow()

    fun ensureLoaded(folderId: Long) {
        if (!canReadLibrary()) return
        val switchingFolders = currentFolderId.value != folderId
        if (switchingFolders) {
            _currentFolderId.value = folderId
            _state.value = MediaCatalogState()
        }
        val current = state.value
        if (current.hasLoadedOnce || current.isLoading) return
        load(folderId = folderId, offset = 0, syncIndex = false, publishFeedback = false)
    }

    fun refresh(folderId: Long) {
        val current = state.value
        if (!canReadLibrary() || current.isLoading || current.isAppending) return
        val switchingFolders = currentFolderId.value != folderId
        _currentFolderId.value = folderId
        if (switchingFolders) {
            _state.value = MediaCatalogState()
        }
        load(folderId = folderId, offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMore(folderId: Long) {
        val current = state.value
        if (!canReadLibrary() ||
            currentFolderId.value != folderId ||
            current.isLoading ||
            current.isAppending ||
            !current.hasMore
        ) {
            return
        }
        load(folderId = folderId, offset = current.nextOffset, syncIndex = false, publishFeedback = false)
    }

    fun refreshLoadedFromIndex() {
        val folderId = currentFolderId.value ?: return
        val current = state.value
        if (current.hasLoadedOnce && !current.isLoading && !current.isAppending) {
            load(folderId = folderId, offset = 0, syncIndex = false, publishFeedback = false)
        }
    }

    fun resetForPermissionLoss() {
        requestTracker.invalidate()
        resetCurrentFolder(MediaCatalogState(hasMore = false))
    }

    fun resetForAccessChange() {
        requestTracker.invalidate()
        resetCurrentFolder(MediaCatalogState())
    }

    private fun resetCurrentFolder(nextState: MediaCatalogState<LocalVideoItem>) {
        _currentFolderId.value = null
        _state.value = nextState
    }

    private fun load(
        folderId: Long,
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        val requestToken = requestTracker.next()
        val previous = state.value
        _state.value = previous.beginLoad(offset)
        scope.launch {
            when (
                val outcome = loadVideoPageUseCase(
                    offset = offset,
                    folderId = folderId,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    if (currentFolderId.value != folderId) return@launch
                    _state.value = previous.applyPage(page = outcome.page, append = offset > 0)
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    warmupInitialThumbnails(scope, loadVideoPageUseCase, outcome.warmupVideos)
                    publishRefreshMessage(
                        hasLoadedOnce = previous.hasLoadedOnce,
                        offset = offset,
                        totalCount = outcome.page.totalCount,
                        fallbackItemCount = outcome.page.items.size,
                        publishFeedback = publishFeedback,
                        publishMessage = publishMessage,
                    )
                }

                is MediaCatalogOutcome.Failure -> {
                    if (!requestTracker.isCurrent(requestToken)) return@launch
                    if (currentFolderId.value != folderId) return@launch
                    handleFailure(outcome.reason, previous, offset)
                }
            }
        }
    }

    private fun handleFailure(
        failure: MediaCatalogFailure,
        currentState: MediaCatalogState<LocalVideoItem>,
        offset: Int,
    ) {
        if (failure == MediaCatalogFailure.PermissionDenied && handlePermissionDenied()) {
            return
        }
        _state.value = currentState.applyFailure(offset = offset, message = failure.toText())
    }
}

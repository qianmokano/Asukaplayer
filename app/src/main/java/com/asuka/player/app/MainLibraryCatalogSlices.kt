package com.asuka.player.app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoFolder>())
    val state: StateFlow<MediaCatalogState<LocalVideoFolder>> = _state.asStateFlow()

    fun ensureLoaded() {
        if (!canReadLibrary() || state.value.hasLoadedOnce || state.value.isLoading) return
        loadInitial()
    }

    fun refresh() {
        if (!canReadLibrary() || state.value.isLoading) return
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
        _state.value = MediaCatalogState(hasMore = false)
    }

    private fun loadInitial() {
        scope.launch {
            val previous = state.value
            _state.value = previous.beginLoad(offset = 0)

            when (
                val cachedOutcome = loadFolderPageUseCase(
                    offset = 0,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = false,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    val hasCachedResults = cachedOutcome.page.items.isNotEmpty() ||
                        (cachedOutcome.page.totalCount ?: 0) > 0
                    if (hasCachedResults) {
                        _state.value = previous.applyPage(page = cachedOutcome.page, append = false)
                    }
                    load(
                        offset = 0,
                        syncIndex = true,
                        publishFeedback = false,
                        forceFullRescan = true,
                    )
                }

                is MediaCatalogOutcome.Failure -> {
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
        scope.launch {
            val previous = state.value
            _state.value = previous.beginLoad(offset)

            when (
                val outcome = loadFolderPageUseCase(
                    offset = offset,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                    forceFullRescan = forceFullRescan ?: !previous.hasLoadedOnce,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    _state.value = previous.applyPage(page = outcome.page, append = offset > 0)
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
    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoItem>())
    val state: StateFlow<MediaCatalogState<LocalVideoItem>> = _state.asStateFlow()

    fun ensureLoaded() {
        if (!canReadLibrary() || state.value.hasLoadedOnce || state.value.isLoading) return
        load(offset = 0, syncIndex = true, publishFeedback = false)
    }

    fun refresh() {
        if (!canReadLibrary() || state.value.isLoading) return
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
        _state.value = MediaCatalogState(hasMore = false)
    }

    private fun load(
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        scope.launch {
            val previous = state.value
            _state.value = previous.beginLoad(offset)

            when (
                val outcome = loadVideoPageUseCase(
                    offset = offset,
                    folderId = null,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    _state.value = previous.applyPage(page = outcome.page, append = offset > 0)
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
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoItem>())
    val state: StateFlow<MediaCatalogState<LocalVideoItem>> = _state.asStateFlow()

    private var currentFolderLoadJob: Job? = null
    private var currentFolderRequestToken: Long = 0L

    fun ensureLoaded(folderId: Long) {
        if (!canReadLibrary()) return
        selectFolder(folderId)
        val current = state.value
        if (current.hasLoadedOnce || current.isLoading) return
        load(folderId = folderId, offset = 0, syncIndex = true, publishFeedback = false)
    }

    fun refresh(folderId: Long) {
        if (!canReadLibrary()) return
        selectFolder(folderId)
        if (state.value.isLoading) return
        load(folderId = folderId, offset = 0, syncIndex = true, publishFeedback = true)
    }

    fun loadMore(folderId: Long) {
        val current = state.value
        if (!canReadLibrary() || currentFolderId.value != folderId) return
        if (current.isLoading || current.isAppending || !current.hasMore) return
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
        currentFolderLoadJob?.cancel()
        currentFolderLoadJob = null
        _currentFolderId.value = null
        _state.value = MediaCatalogState(hasMore = false)
    }

    private fun selectFolder(folderId: Long) {
        if (currentFolderId.value != folderId) {
            currentFolderLoadJob?.cancel()
            currentFolderLoadJob = null
            _currentFolderId.value = folderId
            _state.value = MediaCatalogState()
        }
    }

    private fun load(
        folderId: Long,
        offset: Int,
        syncIndex: Boolean,
        publishFeedback: Boolean,
    ) {
        val requestToken = nextRequestToken()
        currentFolderLoadJob?.cancel()
        val job = scope.launch {
            val previous = state.value
            if (!isCurrentRequest(folderId, requestToken)) return@launch
            _state.value = previous.beginLoad(offset)

            when (
                val outcome = loadVideoPageUseCase(
                    offset = offset,
                    folderId = folderId,
                    hasLoadedOnce = previous.hasLoadedOnce,
                    syncIndex = syncIndex,
                )
            ) {
                is MediaCatalogOutcome.Success -> {
                    if (!isCurrentRequest(folderId, requestToken)) return@launch
                    _state.value = previous.applyPage(page = outcome.page, append = offset > 0)
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
                    if (!isCurrentRequest(folderId, requestToken)) return@launch
                    if (outcome.reason == MediaCatalogFailure.PermissionDenied && handlePermissionDenied()) {
                        return@launch
                    }
                    _state.value = previous.applyFailure(offset = offset, message = outcome.reason.toText())
                }
            }
        }
        currentFolderLoadJob = job
        job.invokeOnCompletion {
            if (currentFolderLoadJob === job) {
                currentFolderLoadJob = null
            }
        }
    }

    private fun nextRequestToken(): Long {
        currentFolderRequestToken += 1L
        return currentFolderRequestToken
    }

    private fun isCurrentRequest(folderId: Long, requestToken: Long): Boolean {
        return currentFolderId.value == folderId && currentFolderRequestToken == requestToken
    }
}

internal class MainLibraryRecentSlice(
    private val loadRecentMediaIdsUseCase: LoadRecentMediaIdsUseCase,
    private val resolveRecentMediaItemsUseCase: ResolveRecentMediaItemsUseCase,
    private val scope: CoroutineScope,
) {
    private val _recentMediaIds = MutableStateFlow(emptyList<String>())
    val recentMediaIds: StateFlow<List<String>> = _recentMediaIds.asStateFlow()

    private val _recentKnownVideos = MutableStateFlow<Map<String, LocalVideoItem>>(emptyMap())
    val recentKnownVideos: StateFlow<Map<String, LocalVideoItem>> = _recentKnownVideos.asStateFlow()

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            val ids = loadRecentMediaIdsUseCase(limit = 100)
            val knownVideos = runCatching { resolveRecentMediaItemsUseCase(ids) }
                .getOrDefault(emptyMap())
            _recentMediaIds.value = ids
            _recentKnownVideos.value = knownVideos
        }
    }

    fun refreshIfLoaded() {
        if (recentMediaIds.value.isNotEmpty()) {
            refresh()
        }
    }

    fun resetForPermissionLoss() {
        _recentMediaIds.value = emptyList()
        _recentKnownVideos.value = emptyMap()
    }
}

private fun publishRefreshMessage(
    hasLoadedOnce: Boolean,
    offset: Int,
    totalCount: Int?,
    fallbackItemCount: Int,
    publishFeedback: Boolean,
    publishMessage: (MainLibraryText) -> Unit,
) {
    if (publishFeedback && offset == 0 && hasLoadedOnce) {
        publishMessage(MainLibraryText.RefreshDone(totalCount ?: fallbackItemCount))
    }
}

private fun warmupInitialThumbnails(
    scope: CoroutineScope,
    loadVideoPageUseCase: LoadVideoPageUseCase,
    videos: List<LocalVideoItem>,
) {
    if (videos.isEmpty()) return
    scope.launch(Dispatchers.IO) {
        try {
            loadVideoPageUseCase.warmupInitialThumbnails(videos)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Warmup is best-effort and should never interfere with the page result.
        }
    }
}

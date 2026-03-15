package com.asuka.player.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class MainLibraryPreloadedVideosSlice(
    private val loadVideoPageUseCase: LoadVideoPageUseCase,
    private val scope: CoroutineScope,
    private val canReadLibrary: () -> Boolean,
    private val handlePermissionDenied: () -> Boolean,
    private val publishMessage: (MainLibraryText) -> Unit,
) {
    private val _state = MutableStateFlow(MediaCatalogState<LocalVideoItem>(hasMore = false))
    val state: StateFlow<MediaCatalogState<LocalVideoItem>> = _state.asStateFlow()

    private var preloadJob: Job? = null
    private var preloadRequestToken: Long = 0L

    fun ensureLoaded() {
        if (!canReadLibrary()) return
        val current = state.value
        if (current.hasLoadedOnce || current.isLoading) return
        preload(syncIndexOnFirstPage = false, publishFeedback = false)
    }

    fun refresh(
        feedbackItemCount: ((List<LocalVideoItem>) -> Int)? = null,
    ) {
        if (!canReadLibrary() || state.value.isLoading) return
        preload(
            syncIndexOnFirstPage = true,
            publishFeedback = true,
            feedbackItemCount = feedbackItemCount,
        )
    }

    fun refreshLoadedFromIndex() {
        val current = state.value
        if (current.hasLoadedOnce && !current.isLoading) {
            preload(syncIndexOnFirstPage = false, publishFeedback = false)
        }
    }

    fun resetForPermissionLoss() {
        preloadRequestToken += 1L
        preloadJob?.cancel()
        preloadJob = null
        _state.value = MediaCatalogState(hasMore = false)
    }

    private fun preload(
        syncIndexOnFirstPage: Boolean,
        publishFeedback: Boolean,
        feedbackItemCount: ((List<LocalVideoItem>) -> Int)? = null,
    ) {
        val previous = state.value
        val requestToken = nextRequestToken()
        preloadJob?.cancel()
        _state.value = previous.beginLoad(offset = 0).copy(
            hasMore = false,
            nextOffset = previous.items.size,
        )

        val job = scope.launch(Dispatchers.IO) {
            val allItems = mutableListOf<LocalVideoItem>()
            var offset = 0
            var warmupVideos = emptyList<LocalVideoItem>()

            while (true) {
                when (
                    val outcome = loadVideoPageUseCase(
                        offset = offset,
                        folderId = null,
                        hasLoadedOnce = previous.hasLoadedOnce && offset == 0,
                        syncIndex = syncIndexOnFirstPage && offset == 0,
                    )
                ) {
                    is MediaCatalogOutcome.Success -> {
                        if (!isActive || !isCurrentRequest(requestToken)) return@launch
                        if (offset == 0) {
                            warmupVideos = outcome.warmupVideos
                        }
                        allItems.addAll(outcome.page.items)
                        offset = outcome.page.nextOffset ?: break
                    }

                    is MediaCatalogOutcome.Failure -> {
                        if (!isActive || !isCurrentRequest(requestToken)) return@launch
                        if (outcome.reason == MediaCatalogFailure.PermissionDenied && handlePermissionDenied()) {
                            return@launch
                        }
                        _state.value = previous.applyFailure(
                            offset = 0,
                            message = outcome.reason.toText(),
                        ).copy(
                            hasMore = false,
                            nextOffset = previous.items.size,
                        )
                        return@launch
                    }
                }
            }

            if (!isActive || !isCurrentRequest(requestToken)) return@launch
            _state.value = MediaCatalogState(
                items = allItems,
                hasLoadedOnce = true,
                hasMore = false,
                nextOffset = allItems.size,
            )
            warmupInitialThumbnails(scope, loadVideoPageUseCase, warmupVideos)
            if (publishFeedback && previous.hasLoadedOnce) {
                publishMessage(
                    MainLibraryText.RefreshDone(
                        feedbackItemCount?.invoke(allItems) ?: allItems.size,
                    ),
                )
            }
        }

        preloadJob = job
        job.invokeOnCompletion {
            if (preloadJob === job) {
                preloadJob = null
            }
        }
    }

    private fun nextRequestToken(): Long {
        preloadRequestToken += 1L
        return preloadRequestToken
    }

    private fun isCurrentRequest(requestToken: Long): Boolean {
        return preloadRequestToken == requestToken
    }
}

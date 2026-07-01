package com.asuka.player.app

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class MainLibraryRecentSlice(
    private val loadRecentMediaIdsUseCase: LoadRecentMediaIdsUseCase,
    private val resolveRecentMediaItemsUseCase: ResolveRecentMediaItemsUseCase,
    private val scope: CoroutineScope,
) {
    private val requestTracker = RequestTracker()
    private val _recentMediaIds = MutableStateFlow(emptyList<String>())
    val recentMediaIds: StateFlow<List<String>> = _recentMediaIds.asStateFlow()

    private val _recentKnownVideos = MutableStateFlow<Map<String, LocalVideoItem>>(emptyMap())
    val recentKnownVideos: StateFlow<Map<String, LocalVideoItem>> = _recentKnownVideos.asStateFlow()

    fun refresh() {
        val requestToken = requestTracker.next()
        scope.launch(Dispatchers.IO) {
            val ids = try {
                loadRecentMediaIdsUseCase(limit = 100)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                return@launch
            }
            if (!requestTracker.isCurrent(requestToken)) return@launch
            val knownVideos = runCatching { resolveRecentMediaItemsUseCase(ids) }
                .getOrDefault(emptyMap())
            if (!requestTracker.isCurrent(requestToken)) return@launch
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
        requestTracker.invalidate()
        _recentMediaIds.value = emptyList()
        _recentKnownVideos.value = emptyMap()
    }
}

internal fun publishRefreshMessage(
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

internal class RequestTracker {
    private val token = AtomicLong(0L)

    fun next(): Long = token.incrementAndGet()

    fun invalidate() {
        token.incrementAndGet()
    }

    fun isCurrent(value: Long): Boolean = token.get() == value
}

internal fun warmupInitialThumbnails(
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

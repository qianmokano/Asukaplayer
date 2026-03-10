package com.asuka.player.app

internal enum class MediaLibraryRefreshStatus {
    Idle,
    Loading,
}

internal data class MediaLibraryRefreshState(
    val items: List<LocalVideoItem> = emptyList(),
    val status: MediaLibraryRefreshStatus = MediaLibraryRefreshStatus.Idle,
    val hasLoadedOnce: Boolean = false,
    val errorMessage: String? = null,
) {
    val isLoading: Boolean
        get() = status == MediaLibraryRefreshStatus.Loading
}

internal enum class MediaLibraryRefreshFailure {
    PermissionDenied,
    ProviderUnavailable,
    Unknown,
}

internal sealed interface MediaLibraryRefreshOutcome {
    data class Success(
        val items: List<LocalVideoItem>,
        val warmupVideos: List<LocalVideoItem>,
    ) : MediaLibraryRefreshOutcome

    data class Failure(
        val reason: MediaLibraryRefreshFailure,
    ) : MediaLibraryRefreshOutcome
}

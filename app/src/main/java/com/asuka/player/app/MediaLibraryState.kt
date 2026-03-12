package com.asuka.player.app

internal enum class MediaCatalogStatus {
    Idle,
    Loading,
    Appending,
}

internal data class MediaCatalogState<T>(
    val items: List<T> = emptyList(),
    val status: MediaCatalogStatus = MediaCatalogStatus.Idle,
    val hasLoadedOnce: Boolean = false,
    val hasMore: Boolean = true,
    val nextOffset: Int = 0,
    val errorMessage: MainLibraryText? = null,
    val appendErrorMessage: MainLibraryText? = null,
) {
    val isLoading: Boolean
        get() = status == MediaCatalogStatus.Loading

    val isAppending: Boolean
        get() = status == MediaCatalogStatus.Appending
}

internal data class MediaLibraryPage<T>(
    val items: List<T>,
    val nextOffset: Int?,
    val totalCount: Int? = null,
)

internal enum class MediaCatalogFailure {
    PermissionDenied,
    ProviderUnavailable,
    Unknown,
}

internal sealed interface MediaCatalogOutcome<out T> {
    data class Success<T>(
        val page: MediaLibraryPage<T>,
        val warmupVideos: List<LocalVideoItem> = emptyList(),
    ) : MediaCatalogOutcome<T>

    data class Failure(
        val reason: MediaCatalogFailure,
    ) : MediaCatalogOutcome<Nothing>
}

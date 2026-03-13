package com.asuka.player.app

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

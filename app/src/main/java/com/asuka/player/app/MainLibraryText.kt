package com.asuka.player.app

import android.content.Context
import com.asuka.player.R

internal sealed interface MainLibraryText {
    data object OpenNetworkStreamInvalid : MainLibraryText
    data class RefreshDone(val itemCount: Int) : MainLibraryText
    data object MediaLibraryPermissionDenied : MainLibraryText
    data object MediaLibraryProviderUnavailable : MainLibraryText
    data object MediaLibraryUnknown : MainLibraryText
}

internal fun MainLibraryText.resolve(context: Context): String {
    return when (this) {
        MainLibraryText.OpenNetworkStreamInvalid ->
            context.getString(R.string.open_network_stream_invalid)
        is MainLibraryText.RefreshDone ->
            context.getString(R.string.refresh_done, itemCount)
        MainLibraryText.MediaLibraryPermissionDenied ->
            context.getString(R.string.media_library_refresh_error_permission)
        MainLibraryText.MediaLibraryProviderUnavailable ->
            context.getString(R.string.media_library_refresh_error_provider)
        MainLibraryText.MediaLibraryUnknown ->
            context.getString(R.string.media_library_refresh_error_unknown)
    }
}

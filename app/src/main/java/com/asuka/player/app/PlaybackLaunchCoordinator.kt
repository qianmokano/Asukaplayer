package com.asuka.player.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.provider.MediaStore
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.core.SeekFallbackCopier
import com.asuka.player.core.remapClipDataUri
import com.asuka.player.ui.activity.PlaybackActivity
import java.io.File

internal interface PlaybackUriResolver {
    fun resolveForPlayback(sourceUri: Uri): Uri
}

internal data class PlaybackLaunchRequest(
    val mediaUri: Uri,
    val clipData: ClipData?,
    val runtimeSettings: PlaybackRuntimeSettings,
)

internal class PlaybackLaunchCoordinator(
    private val uriResolver: PlaybackUriResolver,
) {
    fun createLaunchRequest(
        mediaId: String,
        playerSettings: PlayerSettingsConfig,
        keepConnectionInBackground: Boolean,
        sourceIntent: Intent? = null,
    ): PlaybackLaunchRequest {
        val sourceUri = Uri.parse(mediaId)
        val resolvedUri = uriResolver.resolveForPlayback(sourceUri)
        return PlaybackLaunchRequest(
            mediaUri = resolvedUri,
            clipData = remapClipDataUri(
                clipData = sourceIntent?.clipData,
                originalUri = sourceUri,
                replacementUri = resolvedUri,
            ),
            runtimeSettings = playerSettings.toRuntimeSettings(
                keepConnectionInBackground = keepConnectionInBackground,
            ),
        )
    }

    fun createPlaybackIntent(context: Context, request: PlaybackLaunchRequest): Intent {
        return Intent(context, PlaybackActivity::class.java).apply {
            data = request.mediaUri
            clipData = request.clipData
            if (request.mediaUri.scheme == "content" || request.clipData != null) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            putExtra(PlaybackRuntimeSettings.EXTRA_KEY, request.runtimeSettings)
        }
    }
}

internal class SeekAwarePlaybackUriResolver(
    private val contentResolver: android.content.ContentResolver,
    cacheDir: File,
) : PlaybackUriResolver {
    private val seekFallbackCopier = SeekFallbackCopier(contentResolver, cacheDir)

    override fun resolveForPlayback(sourceUri: Uri): Uri {
        if (sourceUri.scheme != "content") return sourceUri
        if (sourceUri.authority == MediaStore.AUTHORITY) return sourceUri
        if (isContentUriSeekable(sourceUri)) return sourceUri
        return seekFallbackCopier.copy(sourceUri) ?: sourceUri
    }

    private fun isContentUriSeekable(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                try {
                    Os.lseek(pfd.fileDescriptor, 0L, OsConstants.SEEK_CUR)
                    true
                } catch (_: ErrnoException) {
                    false
                }
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }
}

internal fun PlayerSettingsConfig.toRuntimeSettings(
    keepConnectionInBackground: Boolean,
): PlaybackRuntimeSettings {
    return PlaybackRuntimeSettings(
        seekGestureEnabled = seekGestureEnabled,
        brightnessGestureEnabled = brightnessGestureEnabled,
        volumeGestureEnabled = volumeGestureEnabled,
        zoomGestureEnabled = zoomGestureEnabled,
        panGestureEnabled = panGestureEnabled,
        doubleTapGestureEnabled = doubleTapGestureEnabled,
        doubleTapAction = when (doubleTapAction) {
            DoubleTapActionSetting.Seek -> PlaybackRuntimeSettings.DoubleTapAction.Seek
            DoubleTapActionSetting.TogglePlayPause -> PlaybackRuntimeSettings.DoubleTapAction.TogglePlayPause
            DoubleTapActionSetting.Both -> PlaybackRuntimeSettings.DoubleTapAction.Both
        },
        longPressGestureEnabled = longPressGestureEnabled,
        seekIncrementSec = seekIncrementSec,
        seekSensitivity = seekSensitivity,
        longPressSpeed = longPressSpeed,
        controllerTimeoutSec = controllerTimeoutSec,
        hideButtonsBackground = hideButtonsBackground,
        resumePlayback = resumePlayback,
        defaultPlaybackSpeed = defaultPlaybackSpeed,
        autoplay = autoplay,
        autoPip = autoPip,
        autoBackgroundPlay = autoBackgroundPlay,
        rememberBrightness = rememberBrightness,
        rememberSelections = rememberSelections,
        keepSessionConnectionInBackground = keepConnectionInBackground,
    )
}

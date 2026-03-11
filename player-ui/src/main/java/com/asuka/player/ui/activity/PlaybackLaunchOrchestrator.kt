package com.asuka.player.ui.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.PlaybackException
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlan
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.platform.SeekFallbackCopier
import com.asuka.player.platform.copyIntentWithRemappedUri
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class PlaybackLaunchOrchestrator(
    contentResolver: ContentResolver,
    cacheDir: File,
    private val scope: CoroutineScope,
    private val runtimeSettingsSource: PlaybackRuntimeSettingsSource,
    private val copyForSeekFallback: suspend (Uri) -> Uri? = { uri ->
        SeekFallbackCopier(contentResolver, cacheDir).copy(uri, checkSize = true)
    },
) {
    private var currentIntent: Intent? = null
    private var seekFallbackJob: Job? = null
    private val seekFallbackAttemptedUris = mutableSetOf<String>()

    fun updateIntent(
        intent: Intent?,
        clearSeekFallbackAttempts: Boolean = false,
    ) {
        currentIntent = intent
        if (clearSeekFallbackAttempts) {
            seekFallbackAttemptedUris.clear()
        }
    }

    fun currentIntentData(): Uri? = currentIntent?.data

    fun cancelPending() {
        seekFallbackJob?.cancel()
        seekFallbackJob = null
    }

    suspend fun startPlayback(
        targetUri: Uri?,
        sessionStarter: suspend (
            targetUri: Uri?,
            launchIntent: Intent?,
            autoplay: Boolean,
            policy: PlaybackStartupPolicy,
        ) -> PlaybackSessionPlan?,
        applyArtwork: (mediaId: String, startIndex: Int, uri: Uri) -> Unit = { _, _, _ -> },
    ): PlaybackSessionPlan? {
        val runtimeSettings = runtimeSettingsSource.current()
        val plan = sessionStarter(
            targetUri,
            currentIntent,
            runtimeSettings.autoplay,
            PlaybackStartupPolicy(
                resumePlayback = runtimeSettings.resumePlayback,
                defaultPlaybackSpeed = runtimeSettings.defaultPlaybackSpeed,
                rememberTrackSelections = runtimeSettings.rememberSelections,
            ),
        ) ?: return null
        val uri = targetUri ?: return plan
        applyArtwork(uri.toString(), plan.queue.startIndex, uri)
        return plan
    }

    fun handlePlaybackReady(
        currentUri: Uri?,
        isSeekable: Boolean,
        onFallbackResolved: suspend (Uri) -> Unit,
    ) {
        if (currentUri == null || isSeekable) return
        trySeekFallback(currentUri, "not_seekable_ready", onFallbackResolved)
    }

    fun handlePlaybackError(
        currentUri: Uri?,
        error: PlaybackException,
        onFallbackResolved: suspend (Uri) -> Unit,
    ) {
        if (currentUri == null) return
        trySeekFallback(currentUri, "player_error_${error.errorCode}", onFallbackResolved)
    }

    private fun trySeekFallback(
        currentUri: Uri,
        reason: String,
        onFallbackResolved: suspend (Uri) -> Unit,
    ) {
        if (currentUri.scheme != "content") return
        val key = currentUri.toString()
        if (!seekFallbackAttemptedUris.add(key)) return
        if (seekFallbackJob?.isActive == true) return
        seekFallbackJob = scope.launch {
            val copiedUri = copyForSeekFallback(currentUri) ?: return@launch
            Log.i(TAG, "fallback[$reason] src=${currentUri.authority} dst=${copiedUri.lastPathSegment}")
            currentIntent = copyIntentWithRemappedUri(
                intent = currentIntent,
                originalUri = currentUri,
                replacementUri = copiedUri,
            )
            onFallbackResolved(copiedUri)
        }
    }

    private companion object {
        private const val TAG = "AsukaPlayback"
    }
}

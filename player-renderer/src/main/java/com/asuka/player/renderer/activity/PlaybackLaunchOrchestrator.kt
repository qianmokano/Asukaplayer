package com.asuka.player.renderer.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.PlaybackException
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
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
    private var currentRequestId: Long = 0L

    fun updateIntent(
        intent: Intent?,
        supersedeRequest: Boolean = false,
        clearSeekFallbackAttempts: Boolean = false,
    ): Long {
        currentIntent = intent
        if (supersedeRequest || (currentRequestId == 0L && intent != null)) {
            currentRequestId += 1L
            if (supersedeRequest) {
                cancelPending()
            }
        }
        if (clearSeekFallbackAttempts) {
            seekFallbackAttemptedUris.clear()
        }
        return currentRequestId
    }

    fun currentIntentData(): Uri? = currentIntent?.data

    fun currentRequestId(): Long = currentRequestId

    fun cancelPending() {
        seekFallbackJob?.cancel()
        seekFallbackJob = null
    }

    suspend fun startPlayback(
        requestId: Long,
        targetUri: Uri?,
        prepareLaunch: suspend (
            targetUri: Uri?,
            launchIntent: Intent?,
            policy: PlaybackStartupPolicy,
        ) -> PlaybackLaunchResult?,
        applyLaunch: (PlaybackLaunchResult, Boolean) -> Unit,
        applyArtwork: (targetEntry: PlaybackQueueEntry, startIndex: Int, uri: Uri) -> Unit = { _, _, _ -> },
    ): PlaybackLaunchResult? {
        if (requestId != currentRequestId) return null
        val runtimeSettings = runtimeSettingsSource.current()
        val result = prepareLaunch(
            targetUri,
            currentIntent,
            PlaybackStartupPolicy(
                resumePlayback = runtimeSettings.resumePlayback,
                defaultPlaybackSpeed = runtimeSettings.defaultPlaybackSpeed,
                rememberTrackSelections = runtimeSettings.rememberSelections,
            ),
        ) ?: return null
        if (requestId != currentRequestId) return null
        applyLaunch(result, runtimeSettings.autoplay)
        if (requestId != currentRequestId) return null
        val uri = targetUri ?: return result
        applyArtwork(result.targetEntry, result.plan.queue.startIndex, uri)
        return result
    }

    fun handlePlaybackReady(
        requestId: Long,
        currentUri: Uri?,
        isSeekable: Boolean,
        onFallbackResolved: suspend (Uri) -> Unit,
    ) {
        if (currentUri == null || isSeekable) return
        trySeekFallback(requestId, currentUri, "not_seekable_ready", onFallbackResolved)
    }

    fun handlePlaybackError(
        requestId: Long,
        currentUri: Uri?,
        error: PlaybackException,
        onFallbackResolved: suspend (Uri) -> Unit,
    ) {
        if (currentUri == null) return
        trySeekFallback(requestId, currentUri, "player_error_${error.errorCode}", onFallbackResolved)
    }

    private fun trySeekFallback(
        requestId: Long,
        currentUri: Uri,
        reason: String,
        onFallbackResolved: suspend (Uri) -> Unit,
    ) {
        if (requestId != currentRequestId) return
        if (currentUri.scheme != "content") return
        val key = currentUri.toString()
        if (!seekFallbackAttemptedUris.add(key)) return
        if (seekFallbackJob?.isActive == true) return
        val job = scope.launch {
            val copiedUri = copyForSeekFallback(currentUri) ?: return@launch
            if (requestId != currentRequestId) return@launch
            Log.i(TAG, "fallback[$reason] src=${currentUri.authority} dst=${copiedUri.lastPathSegment}")
            currentIntent = copyIntentWithRemappedUri(
                intent = currentIntent,
                originalUri = currentUri,
                replacementUri = copiedUri,
            )
            onFallbackResolved(copiedUri)
        }
        seekFallbackJob = job
        job.invokeOnCompletion {
            if (seekFallbackJob === job) {
                seekFallbackJob = null
            }
        }
    }

    private companion object {
        private const val TAG = "AsukaPlayback"
    }
}

internal data class PlaybackLaunchResult(
    val targetEntry: PlaybackQueueEntry,
    val plan: com.asuka.player.contract.PlaybackSessionPlan,
)

package com.asuka.player.renderer.activity

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.PlaybackException
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlan
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.platform.PlaybackSessionRequestCodec
import com.asuka.player.platform.SeekFallbackCopier
import java.io.File
import java.util.Collections
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
    @Volatile
    private var currentRequest: PlaybackSessionRequest? = null
    private var seekFallbackJob: Job? = null
    private val seekFallbackAttemptedUris = Collections.synchronizedSet(mutableSetOf<String>())
    @Volatile
    private var latestRequestId: Long = 0L

    fun updateIntent(
        intent: Intent?,
        supersedeRequest: Boolean = false,
        clearSeekFallbackAttempts: Boolean = false,
    ): Long {
        val parsedRequest = PlaybackSessionRequestCodec.readPlaybackRequest(intent)
        if (supersedeRequest || (latestRequestId == 0L && parsedRequest != null)) {
            latestRequestId += 1L
            if (supersedeRequest) {
                cancelPending()
            }
        }
        currentRequest = parsedRequest?.withRequestId(latestRequestId)
        if (clearSeekFallbackAttempts) {
            seekFallbackAttemptedUris.clear()
        }
        return currentRequest?.requestId ?: 0L
    }

    fun currentPlaybackUri(): Uri? = currentRequest?.playbackUri?.let(Uri::parse)

    fun currentRequestId(): Long = currentRequest?.requestId ?: 0L

    fun cancelPending() {
        seekFallbackJob?.cancel()
        seekFallbackJob = null
    }

    suspend fun startPlayback(
        requestId: Long,
        prepareLaunch: suspend (PlaybackSessionRequest) -> PlaybackLaunchResult?,
        applyLaunch: (PlaybackLaunchResult) -> Unit,
        applyArtwork: (request: PlaybackSessionRequest, startIndex: Int, uri: Uri) -> Unit = { _, _, _ -> },
    ): PlaybackLaunchResult? {
        val baseRequest = currentRequest ?: return null
        if (requestId != baseRequest.requestId) return null
        val runtimeSettings = runtimeSettingsSource.current()
        val request = baseRequest.withRuntimePolicy(
            policy = PlaybackStartupPolicy(
                resumePlayback = runtimeSettings.resumePlayback,
                defaultPlaybackSpeed = runtimeSettings.defaultPlaybackSpeed,
                rememberTrackSelections = runtimeSettings.rememberSelections,
            ),
            autoplay = runtimeSettings.autoplay,
        )
        val result = prepareLaunch(request) ?: return null
        if (requestId != currentRequestId()) return null
        applyLaunch(result)
        if (requestId != currentRequestId()) return null
        applyArtwork(result.request, result.plan.queue.startIndex, Uri.parse(result.request.playbackUri))
        return result
    }

    fun handlePlaybackReady(
        requestId: Long,
        currentUri: Uri?,
        isSeekable: Boolean,
        onFallbackResolved: suspend () -> Unit,
    ) {
        if (currentUri == null || isSeekable) return
        trySeekFallback(requestId, currentUri, "not_seekable_ready", onFallbackResolved)
    }

    fun handlePlaybackError(
        requestId: Long,
        currentUri: Uri?,
        error: PlaybackException,
        onFallbackResolved: suspend () -> Unit,
    ) {
        if (currentUri == null) return
        trySeekFallback(requestId, currentUri, "player_error_${error.errorCode}", onFallbackResolved)
    }

    private fun trySeekFallback(
        requestId: Long,
        currentUri: Uri,
        reason: String,
        onFallbackResolved: suspend () -> Unit,
    ) {
        if (requestId != currentRequestId()) return
        if (currentUri.scheme != "content") return
        val key = currentUri.toString()
        if (!seekFallbackAttemptedUris.add(key)) return
        if (seekFallbackJob?.isActive == true) return
        val job = scope.launch {
            val copiedUri = copyForSeekFallback(currentUri) ?: return@launch
            if (requestId != currentRequestId()) return@launch
            Log.i(TAG, "fallback[$reason] src=${currentUri.authority} dst=${copiedUri.lastPathSegment}")
            currentRequest = currentRequest?.withPlaybackUri(copiedUri.toString())
            onFallbackResolved()
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
    val request: PlaybackSessionRequest,
    val plan: PlaybackSessionPlan,
)

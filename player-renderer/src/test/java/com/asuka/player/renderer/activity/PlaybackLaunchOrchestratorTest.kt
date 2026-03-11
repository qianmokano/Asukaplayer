package com.asuka.player.renderer.activity

import android.content.Intent
import android.net.Uri
import androidx.media3.common.PlaybackException
import com.asuka.player.contract.PlaybackQueueEntry
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlan
import com.asuka.player.contract.PlaybackStartupPolicy
import com.asuka.player.contract.PlaybackQueue
import com.asuka.player.contract.PlayerSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackLaunchOrchestratorTest {

    @Test
    fun startPlayback_usesCurrentIntentAndRuntimePolicy() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val launchIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://videos/current.mp4")
        }
        val runtimeSettingsSource = fakeRuntimeSettingsSource(
            PlaybackRuntimeSettings(
                playerSettings = PlayerSettings(
                    autoplay = false,
                    resumePlayback = false,
                    defaultPlaybackSpeed = 1.25f,
                    rememberSelections = false,
                ),
            ),
        )
        val orchestrator = PlaybackLaunchOrchestrator(
            contentResolver = context.contentResolver,
            cacheDir = context.cacheDir,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            runtimeSettingsSource = runtimeSettingsSource,
            copyForSeekFallback = { null },
        )
        val requestId = orchestrator.updateIntent(
            intent = launchIntent,
            supersedeRequest = true,
            clearSeekFallbackAttempts = true,
        )

        var capturedIntent: Intent? = null
        var capturedPolicy: PlaybackStartupPolicy? = null
        var artworkMediaId: String? = null
        var applyAutoplay: Boolean? = null

        orchestrator.startPlayback(
            requestId = requestId,
            targetUri = launchIntent.data,
            prepareLaunch = { _, forwardedIntent, policy ->
                capturedIntent = forwardedIntent
                capturedPolicy = policy
                PlaybackLaunchResult(
                    targetEntry = PlaybackQueueEntry(
                        mediaId = "media-store:42",
                        uri = launchIntent.data.toString(),
                    ),
                    plan = PlaybackSessionPlan(
                        queue = PlaybackQueue(items = emptyList(), startIndex = 0),
                        resumePositionMs = 0L,
                        playbackSpeed = 1.25f,
                        trackSelectionRestoreRequest = null,
                    ),
                )
            },
            applyLaunch = { _, autoplay -> applyAutoplay = autoplay },
            applyArtwork = { targetEntry, _, _ -> artworkMediaId = targetEntry.mediaId },
        )

        assertFalse(applyAutoplay!!)
        assertEquals(launchIntent, capturedIntent)
        assertEquals(
            PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.25f,
                rememberTrackSelections = false,
            ),
            capturedPolicy,
        )
        assertEquals("media-store:42", artworkMediaId)
    }

    @Test
    fun handlePlaybackError_remapsIntentAndSuppressesDuplicateFallback() {
        val context = RuntimeEnvironment.getApplication()
        val original = Uri.parse("content://videos/current.mp4")
        val replacement = Uri.parse("file:///cache/current.mp4")
        val orchestrator = PlaybackLaunchOrchestrator(
            contentResolver = context.contentResolver,
            cacheDir = context.cacheDir,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            runtimeSettingsSource = fakeRuntimeSettingsSource(PlaybackRuntimeSettings()),
            copyForSeekFallback = { replacement },
        )
        val requestId = orchestrator.updateIntent(
            Intent(Intent.ACTION_VIEW).apply {
                data = original
            },
            supersedeRequest = true,
            clearSeekFallbackAttempts = true,
        )

        var callbackCount = 0
        val error = PlaybackException("boom", Throwable("boom"), PlaybackException.ERROR_CODE_IO_UNSPECIFIED)

        orchestrator.handlePlaybackError(requestId, original, error) { callbackCount += 1 }
        orchestrator.handlePlaybackError(requestId, original, error) { callbackCount += 1 }

        assertEquals(1, callbackCount)
        assertEquals(replacement, orchestrator.currentIntentData())
    }

    @Test
    fun startPlayback_doesNotApplyPreparedLaunchAfterRequestIsSuperseded() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val firstIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://videos/first.mp4")
        }
        val secondIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("content://videos/second.mp4")
        }
        val orchestrator = PlaybackLaunchOrchestrator(
            contentResolver = context.contentResolver,
            cacheDir = context.cacheDir,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            runtimeSettingsSource = fakeRuntimeSettingsSource(PlaybackRuntimeSettings()),
            copyForSeekFallback = { null },
        )
        val firstRequestId = orchestrator.updateIntent(
            intent = firstIntent,
            supersedeRequest = true,
            clearSeekFallbackAttempts = true,
        )

        var applyCount = 0
        var artworkCount = 0
        val result = orchestrator.startPlayback(
            requestId = firstRequestId,
            targetUri = firstIntent.data,
            prepareLaunch = { _, _, _ ->
                orchestrator.updateIntent(
                    intent = secondIntent,
                    supersedeRequest = true,
                    clearSeekFallbackAttempts = true,
                )
                PlaybackLaunchResult(
                    targetEntry = PlaybackQueueEntry(
                        mediaId = "media-store:1",
                        uri = firstIntent.data.toString(),
                    ),
                    plan = PlaybackSessionPlan(
                        queue = PlaybackQueue(items = emptyList(), startIndex = 0),
                        resumePositionMs = 0L,
                        playbackSpeed = 1f,
                        trackSelectionRestoreRequest = null,
                    ),
                )
            },
            applyLaunch = { _, _ -> applyCount += 1 },
            applyArtwork = { _, _, _ -> artworkCount += 1 },
        )

        assertEquals(null, result)
        assertEquals(0, applyCount)
        assertEquals(0, artworkCount)
        assertEquals(secondIntent.data, orchestrator.currentIntentData())
    }

    private fun fakeRuntimeSettingsSource(
        initialValue: PlaybackRuntimeSettings,
    ): PlaybackRuntimeSettingsSource {
        return object : PlaybackRuntimeSettingsSource {
            override val settings = MutableStateFlow(initialValue)
        }
    }
}

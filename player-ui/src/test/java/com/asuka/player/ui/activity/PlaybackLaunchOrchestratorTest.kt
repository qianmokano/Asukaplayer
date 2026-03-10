package com.asuka.player.ui.activity

import android.content.Intent
import android.net.Uri
import androidx.media3.common.PlaybackException
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.core.PlaybackRuntimeSettingsSource
import com.asuka.player.core.PlaybackSessionPlan
import com.asuka.player.core.PlaybackStartupPolicy
import com.asuka.player.core.PlayerSettings
import com.asuka.player.core.QueueBuilder
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
        orchestrator.updateIntent(launchIntent)

        var capturedAutoplay: Boolean? = null
        var capturedIntent: Intent? = null
        var capturedPolicy: PlaybackStartupPolicy? = null
        var artworkApplied = false

        orchestrator.startPlayback(
            targetUri = launchIntent.data,
            sessionStarter = { _, forwardedIntent, autoplay, policy ->
                capturedIntent = forwardedIntent
                capturedAutoplay = autoplay
                capturedPolicy = policy
                PlaybackSessionPlan(
                    queue = QueueBuilder.Queue(items = emptyList(), startIndex = 0),
                    resumePositionMs = 0L,
                    playbackSpeed = 1.25f,
                    trackSelectionRestoreRequest = null,
                )
            },
            applyArtwork = { _, _, _ -> artworkApplied = true },
        )

        assertFalse(capturedAutoplay!!)
        assertEquals(launchIntent, capturedIntent)
        assertEquals(
            PlaybackStartupPolicy(
                resumePlayback = false,
                defaultPlaybackSpeed = 1.25f,
                rememberTrackSelections = false,
            ),
            capturedPolicy,
        )
        assertTrue(artworkApplied)
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
        orchestrator.updateIntent(
            Intent(Intent.ACTION_VIEW).apply {
                data = original
            },
        )

        var callbackCount = 0
        val error = PlaybackException("boom", Throwable("boom"), PlaybackException.ERROR_CODE_IO_UNSPECIFIED)

        orchestrator.handlePlaybackError(original, error) { callbackCount += 1 }
        orchestrator.handlePlaybackError(original, error) { callbackCount += 1 }

        assertEquals(1, callbackCount)
        assertEquals(replacement, orchestrator.currentIntentData())
    }

    private fun fakeRuntimeSettingsSource(
        initialValue: PlaybackRuntimeSettings,
    ): PlaybackRuntimeSettingsSource {
        return object : PlaybackRuntimeSettingsSource {
            override val settings = MutableStateFlow(initialValue)
        }
    }
}

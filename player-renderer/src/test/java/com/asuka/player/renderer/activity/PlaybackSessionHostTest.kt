package com.asuka.player.renderer.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.media3.session.MediaController
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.contract.PlaybackDeviceController
import com.asuka.player.contract.PlaybackPreviewFrameProvider
import com.asuka.player.platform.PlaybackDeviceControllerFactory
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackRuntimeSettingsSource
import com.asuka.player.contract.PlaybackSessionPlanner
import com.asuka.player.contract.PlaybackStore
import com.asuka.player.contract.PlaybackStateRepository
import com.asuka.player.contract.PlaybackUiPersistence
import com.asuka.player.ui.R
import com.asuka.player.ui.controller.PlaybackControllerConnector
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class PlaybackSessionHostTest {

    @Test
    fun ensureControllerReady_surfacesConnectionFailureToUiState() {
        val context = RuntimeEnvironment.getApplication()
        val connector = FailingPlaybackControllerConnector()
        val host = PlaybackSessionHost(
            contentResolver = context.contentResolver,
            cacheDir = context.cacheDir,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
            dependencies = fakeDependencies(context),
            controllerContext = context,
            controllerProvider = connector,
        )

        host.ensureControllerReady(
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://videos/current.mp4")
            },
        )

        waitForState { !host.state.value.isConnectingController }

        assertEquals(
            context.getString(R.string.playback_session_connection_failed),
            host.state.value.controllerErrorMessage,
        )
        assertFalse(host.state.value.isConnectingController)
        assertNull(host.state.value.controller)
        assertTrue(connector.releaseCalled)
    }

    private fun fakeDependencies(context: Context): PlaybackActivityDependencies {
        val playbackStore = FakePlaybackStore()
        return object : PlaybackActivityDependencies {
            private val playbackStateRepository = PlaybackStateRepository(playbackStore)
            override val playbackSessionPlanner = PlaybackSessionPlanner(playbackStateRepository)
            override val playbackRuntimeSettingsSource = object : PlaybackRuntimeSettingsSource {
                override val settings = MutableStateFlow(PlaybackRuntimeSettings())
            }
            override val playbackUiPersistence = object : PlaybackUiPersistence {
                override suspend fun readZoom(mediaId: String): Float? = null
                override suspend fun saveZoom(mediaId: String, zoom: Float) {}
                override fun readRememberedBrightness(): Float? = null
                override fun saveRememberedBrightness(brightness: Float) {}
            }
            override val playbackPreviewFrameProvider = object : PlaybackPreviewFrameProvider {
                override suspend fun loadPreviewFrame(
                    mediaId: String,
                    positionMs: Long,
                    maxWidthPx: Int,
                    maxHeightPx: Int,
                ) = null
            }
            override val playbackDeviceControllerFactory = PlaybackDeviceControllerFactory { _ ->
                object : PlaybackDeviceController {
                    override fun currentVolumePercent(): Int = 50
                    override fun setVolumePercent(percent: Int) {}
                    override fun currentBrightnessPercent(): Int = 50
                    override fun setBrightnessPercent(percent: Int) {}
                }
            }
            override fun createPlaybackControllerConnector(context: Context) =
                error("PlaybackControllerConnector should be injected explicitly in this test")
        }
    }

    private fun waitForState(predicate: () -> Boolean) {
        repeat(20) {
            if (predicate()) return
            Thread.sleep(10)
        }
        error("Expected PlaybackSessionHost state update")
    }
}

private class FakePlaybackStore : PlaybackStore {
    override suspend fun loadPosition(mediaId: String): Long? = null
    override suspend fun savePosition(mediaId: String, positionMs: Long) = Unit
    override suspend fun loadPlaybackSpeed(mediaId: String): Float? = null
    override suspend fun savePlaybackSpeed(mediaId: String, speed: Float) = Unit
    override suspend fun loadAudioTrackId(mediaId: String): String? = null
    override suspend fun saveAudioTrackId(mediaId: String, trackId: String) = Unit
    override suspend fun loadSubtitleTrackId(mediaId: String): String? = null
    override suspend fun saveSubtitleTrackId(mediaId: String, trackId: String) = Unit
    override suspend fun loadZoom(mediaId: String): Float? = null
    override suspend fun saveZoom(mediaId: String, zoom: Float) = Unit
}

private class FailingPlaybackControllerConnector : PlaybackControllerConnector {
    var releaseCalled: Boolean = false

    override fun buildAsync(): ListenableFuture<MediaController> {
        return Futures.immediateFailedFuture(IllegalStateException("boom"))
    }

    override fun asPlaybackController(mediaController: MediaController) =
        error("PlaybackController should not be requested when controller creation fails")

    override fun asTrackSelectionController(mediaController: MediaController) =
        error("Track selection controller should not be requested when controller creation fails")

    override fun release() {
        releaseCalled = true
    }
}

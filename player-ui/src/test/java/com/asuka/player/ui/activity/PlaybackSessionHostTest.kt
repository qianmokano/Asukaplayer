package com.asuka.player.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Window
import androidx.media3.session.MediaController
import com.asuka.player.core.PlaybackActivityDependencies
import com.asuka.player.core.PlaybackDeviceController
import com.asuka.player.core.PlaybackDeviceControllerFactory
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.core.PlaybackRuntimeSettingsSource
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.PlaybackStateRepository
import com.asuka.player.core.PlaybackUiPersistence
import com.asuka.player.data.InMemoryPlaybackStore
import com.asuka.player.data.InMemoryQueueHistoryStore
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
        val playbackStore = InMemoryPlaybackStore()
        return object : PlaybackActivityDependencies {
            private val playbackStateRepository = PlaybackStateRepository(playbackStore)
            override val playbackSessionPlanner = PlaybackSessionPlanner(playbackStateRepository)
            override val playbackRuntimeSettingsSource = object : PlaybackRuntimeSettingsSource {
                override val settings = MutableStateFlow(PlaybackRuntimeSettings())
            }
            override val playbackUiPersistence = object : PlaybackUiPersistence {
                override fun readZoom(mediaId: String): Float? = null
                override fun saveZoom(mediaId: String, zoom: Float) {}
                override fun readRememberedBrightness(): Float? = null
                override fun saveRememberedBrightness(brightness: Float) {}
            }
            override val playbackDeviceControllerFactory = PlaybackDeviceControllerFactory { _: Context, _: Window ->
                object : PlaybackDeviceController {
                    override fun currentVolumePercent(): Int = 50
                    override fun setVolumePercent(percent: Int) {}
                    override fun currentBrightnessPercent(): Int = 50
                    override fun setBrightnessPercent(percent: Int) {}
                }
            }
            override val playbackServiceComponent = ComponentName(context, PlaybackSessionHostTest::class.java)
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

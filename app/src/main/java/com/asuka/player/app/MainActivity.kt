package com.asuka.player.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.asuka.player.platform.PlaybackIntentPayload
import com.asuka.player.platform.PlaybackIntentPayloadCodec
import com.asuka.player.runtime.IncomingPlaybackIntentReader
import com.asuka.player.runtime.PlaybackLaunchRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val mainActivityDependencies: MainActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        (application as? MainActivityDependenciesProvider)?.mainActivityDependencies
            ?: error("Application does not provide MainActivityDependencies.")
    }

    private var launchedForDirectPlayback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val incomingPlayback = runCatching { IncomingPlaybackIntentReader.read(intent) }.getOrNull()
        if (incomingPlayback != null) {
            launchedForDirectPlayback = true
            setContent {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
            requestPlayback(incomingPlayback.payload)
            return
        }

        setContent {
            MainLibraryScreen(
                viewModelFactory = mainActivityDependencies.mainLibraryViewModelFactory,
                onPlay = { mediaId, queueMediaIds ->
                    requestPlayback(
                        PlaybackIntentPayloadCodec.fromSelection(
                            targetMediaId = mediaId,
                            queueMediaIds = queueMediaIds,
                        ),
                    )
                },
            )
        }
    }

    private fun requestPlayback(payload: PlaybackIntentPayload) {
        lifecycleScope.launch {
            val launchRequest = withContext(Dispatchers.IO) {
                mainActivityDependencies.createPlaybackLaunchRequest(payload)
            }
            startPlayback(launchRequest)
        }
    }

    private fun startPlayback(launchRequest: PlaybackLaunchRequest) {
        val playbackIntent = mainActivityDependencies.createPlaybackIntent(
            context = this,
            request = launchRequest,
        )
        startActivity(playbackIntent)
        if (launchedForDirectPlayback) {
            finish()
        }
    }
}

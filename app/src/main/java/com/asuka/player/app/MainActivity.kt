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
import com.asuka.player.ui.activity.PlaybackActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var launchedForDirectPlayback = false
    private val appGraph by lazy { application.appGraph }

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
            requestPlayback(
                mediaId = incomingPlayback.mediaId,
                sourceIntent = incomingPlayback.sourceIntent,
                queueMediaIds = incomingPlayback.queueMediaIds,
            )
            return
        }

        setContent {
            MainLibraryScreen(
                onPlay = { mediaId, queueMediaIds ->
                    requestPlayback(mediaId, queueMediaIds = queueMediaIds)
                },
            )
        }
    }

    private fun requestPlayback(
        mediaId: String,
        sourceIntent: Intent? = null,
        queueMediaIds: List<String> = emptyList(),
    ) {
        lifecycleScope.launch {
            val launchRequest = withContext(Dispatchers.IO) {
                appGraph.playbackLaunchCoordinator.createLaunchRequest(
                    mediaId = mediaId,
                    sourceIntent = sourceIntent,
                    queueMediaIds = queueMediaIds,
                )
            }
            startPlayback(launchRequest)
        }
    }

    private fun startPlayback(launchRequest: PlaybackLaunchRequest) {
        val playbackIntent = appGraph.playbackLaunchCoordinator.createPlaybackIntent(
            context = this,
            activityClass = PlaybackActivity::class.java,
            request = launchRequest,
        )
        startActivity(playbackIntent)
        if (launchedForDirectPlayback) {
            finish()
        }
    }
}

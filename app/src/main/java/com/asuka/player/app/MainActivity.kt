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

        val incomingData = try { intent?.data } catch (_: Throwable) { null }
        if (incomingData != null) {
            launchedForDirectPlayback = true
            setContent {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
            requestPlayback(
                mediaId = incomingData.toString(),
                playerSettings = appGraph.playerSettingsRepository.playerSettings,
                sourceIntent = intent,
            )
            return
        }

        setContent {
            MainLibraryScreen(
                onPlay = { mediaId, playerSettings -> requestPlayback(mediaId, playerSettings) },
            )
        }
    }

    private fun requestPlayback(
        mediaId: String,
        playerSettings: PlayerSettingsConfig,
        sourceIntent: Intent? = null,
    ) {
        lifecycleScope.launch {
            val launchRequest = withContext(Dispatchers.IO) {
                appGraph.playbackLaunchCoordinator.createLaunchRequest(
                    mediaId = mediaId,
                    playerSettings = playerSettings,
                    keepConnectionInBackground = appGraph.playbackBehaviorRepository.keepConnectionInBackground,
                    sourceIntent = sourceIntent,
                )
            }
            startPlayback(launchRequest)
        }
    }

    private fun startPlayback(launchRequest: PlaybackLaunchRequest) {
        val playbackIntent = appGraph.playbackLaunchCoordinator.createPlaybackIntent(this, launchRequest)
        startActivity(playbackIntent)
        if (launchedForDirectPlayback) {
            finish()
        }
    }
}

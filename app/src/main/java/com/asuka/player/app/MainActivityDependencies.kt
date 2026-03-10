package com.asuka.player.app

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider

interface MainActivityDependencies {
    val mainLibraryViewModelFactory: ViewModelProvider.Factory

    fun createPlaybackLaunchRequest(
        mediaId: String,
        sourceIntent: Intent? = null,
        queueMediaIds: List<String> = emptyList(),
    ): PlaybackLaunchRequest

    fun createPlaybackIntent(
        context: Context,
        request: PlaybackLaunchRequest,
    ): Intent
}

interface MainActivityDependenciesProvider {
    val mainActivityDependencies: MainActivityDependencies
}

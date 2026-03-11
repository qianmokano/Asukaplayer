package com.asuka.player.app

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.asuka.player.platform.PlaybackIntentPayload
import com.asuka.player.runtime.PlaybackLaunchRequest

interface MainActivityDependencies {
    val mainLibraryViewModelFactory: ViewModelProvider.Factory

    fun createPlaybackLaunchRequest(
        payload: PlaybackIntentPayload,
    ): PlaybackLaunchRequest

    fun createPlaybackIntent(
        context: Context,
        request: PlaybackLaunchRequest,
    ): Intent
}

interface MainActivityDependenciesProvider {
    val mainActivityDependencies: MainActivityDependencies
}

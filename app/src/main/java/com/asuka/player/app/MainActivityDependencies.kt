package com.asuka.player.app

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.asuka.player.contract.PlaybackSessionRequest

interface MainActivityDependencies {
    val mainLibraryViewModelFactory: ViewModelProvider.Factory

    fun preparePlaybackRequest(
        request: PlaybackSessionRequest,
    ): PlaybackSessionRequest

    fun createPlaybackIntent(
        context: Context,
        request: PlaybackSessionRequest,
    ): Intent
}

interface MainActivityDependenciesProvider {
    val mainActivityDependencies: MainActivityDependencies
}

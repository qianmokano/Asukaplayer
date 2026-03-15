package com.asuka.player.app

import android.app.Application
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

    companion object {
        fun from(application: Application): MainActivityDependenciesProvider {
            return application as? MainActivityDependenciesProvider
                ?: error(
                    "${application::class.java.name} does not implement MainActivityDependenciesProvider. " +
                        "Ensure your Application class implements MainActivityDependenciesProvider.",
                )
        }
    }
}

package com.asuka.player.app

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.asuka.player.runtime.AsukaAppGraph
import com.asuka.player.runtime.PlaybackLaunchCoordinator
import com.asuka.player.runtime.PlaybackLaunchRequest

internal object MainLibraryFeatureInstaller {
    fun install(
        application: Application,
        graph: AsukaAppGraph,
        playbackActivityClass: Class<*>,
    ): MainActivityDependencies {
        val mediaLibraryRepository = createMediaLibraryRepository(application, graph)
        val mainLibraryViewModelFactory: ViewModelProvider.Factory = MainLibraryViewModel.Factory(
            MainLibraryViewModelDependencies(
                appContext = application,
                uiSettingsRepository = graph.uiSettingsRepository,
                playerSettingsRepository = graph.playerSettingsRepository,
                resolveVideoAccessUseCase = ResolveVideoAccessUseCase(mediaLibraryRepository),
                refreshMediaLibraryUseCase = RefreshMediaLibraryUseCase(mediaLibraryRepository),
                loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(mediaLibraryRepository),
            ),
        )
        return AppMainActivityDependencies(
            playbackLaunchCoordinator = graph.playbackLaunchCoordinator,
            playbackActivityClass = playbackActivityClass,
            mainLibraryViewModelFactory = mainLibraryViewModelFactory,
        )
    }

    private fun createMediaLibraryRepository(
        application: Application,
        graph: AsukaAppGraph,
    ): MediaLibraryRepository {
        return AndroidMediaLibraryRepository(
            videoAccessDataSource = AndroidVideoAccessDataSource(application),
            localVideoCatalogDataSource = AndroidMediaStoreVideoCatalogDataSource(application),
            recentPlaybackDataSource = PlaybackRecentMediaDataSource(
                playbackStateRepository = graph.playbackStateRepository,
                queueHistoryRepository = graph.queueHistoryRepository,
            ),
        )
    }
}

private class AppMainActivityDependencies(
    private val playbackLaunchCoordinator: PlaybackLaunchCoordinator,
    private val playbackActivityClass: Class<*>,
    override val mainLibraryViewModelFactory: ViewModelProvider.Factory,
) : MainActivityDependencies {
    override fun createPlaybackLaunchRequest(
        mediaId: String,
        sourceIntent: Intent?,
        queueMediaIds: List<String>,
    ): PlaybackLaunchRequest {
        return playbackLaunchCoordinator.createLaunchRequest(
            mediaId = mediaId,
            sourceIntent = sourceIntent,
            queueMediaIds = queueMediaIds,
        )
    }

    override fun createPlaybackIntent(
        context: android.content.Context,
        request: PlaybackLaunchRequest,
    ): Intent {
        return playbackLaunchCoordinator.createPlaybackIntent(
            context = context,
            activityClass = playbackActivityClass,
            request = request,
        )
    }
}

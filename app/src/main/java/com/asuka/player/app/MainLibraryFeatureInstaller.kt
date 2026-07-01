package com.asuka.player.app

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.asuka.player.contract.PlaybackSessionRequest
import com.asuka.player.runtime.AsukaAppGraph

private fun createMediaLibraryRepository(
    application: Application,
    graph: AsukaAppGraph,
): MediaLibraryRepository {
    return AndroidMediaLibraryRepository(
        videoAccessDataSource = AndroidVideoAccessDataSource(application),
        localVideoCatalogDataSource = AndroidMediaStoreVideoCatalogDataSource(
            context = application,
            playbackStateRepositoryProvider = { graph.playback.playbackStateRepository },
        ),
        recentPlaybackDataSource = PlaybackRecentMediaDataSource(
            playbackStateRepositoryProvider = { graph.playback.playbackStateRepository },
            queueHistoryRepositoryProvider = { graph.playback.queueHistoryRepository },
        ),
    )
}

internal class AppMainActivityDependencies(
    private val application: Application,
    private val graph: AsukaAppGraph,
    private val playbackActivityClass: Class<*>,
) : MainActivityDependencies {
    private val playbackLaunchCoordinator
        get() = graph.playback.playbackLaunchCoordinator

    override val mainLibraryViewModelFactory: ViewModelProvider.Factory by lazy(LazyThreadSafetyMode.NONE) {
        MainLibraryViewModel.Factory {
            val mediaLibraryRepository = createMediaLibraryRepository(application, graph)
            MainLibraryViewModelDependencies(
                uiSettingsRepository = graph.settings.uiSettingsRepository,
                playerSettingsRepository = graph.settings.playerSettingsRepository,
                resolveVideoAccessUseCase = ResolveVideoAccessUseCase(mediaLibraryRepository),
                loadFolderPageUseCase = LoadFolderPageUseCase(mediaLibraryRepository),
                loadVideoPageUseCase = LoadVideoPageUseCase(mediaLibraryRepository),
                loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(mediaLibraryRepository),
                resolveRecentMediaItemsUseCase = ResolveRecentMediaItemsUseCase(mediaLibraryRepository),
                observeMediaLibraryChangesUseCase = ObserveMediaLibraryChangesUseCase(mediaLibraryRepository),
                observeMediaLibrarySyncFailuresUseCase = ObserveMediaLibrarySyncFailuresUseCase(mediaLibraryRepository),
            )
        }
    }

    override fun preparePlaybackRequest(
        request: PlaybackSessionRequest,
    ): PlaybackSessionRequest {
        return playbackLaunchCoordinator.prepareRequest(request)
    }

    override fun createPlaybackIntent(
        context: android.content.Context,
        request: PlaybackSessionRequest,
    ): Intent {
        return playbackLaunchCoordinator.createPlaybackIntent(
            context = context,
            activityClass = playbackActivityClass,
            request = request,
        )
    }
}

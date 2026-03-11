package com.asuka.player.app

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import com.asuka.player.platform.PlaybackIntentPayload
import com.asuka.player.runtime.PlaybackLaunchCoordinator
import com.asuka.player.runtime.PlaybackLaunchRequest

internal object MainLibraryFeatureInstaller {
    fun install(
        application: Application,
        bindings: MainLibraryFeatureBindings,
        playbackActivityClass: Class<*>,
    ): MainActivityDependencies {
        return AppMainActivityDependencies(
            application = application,
            bindings = bindings,
            playbackLaunchCoordinator = bindings.playbackLaunchCoordinator(),
            playbackActivityClass = playbackActivityClass,
        )
    }

}

private fun createMediaLibraryRepository(
    application: Application,
    bindings: MainLibraryFeatureBindings,
): MediaLibraryRepository {
    return AndroidMediaLibraryRepository(
        videoAccessDataSource = AndroidVideoAccessDataSource(application),
        localVideoCatalogDataSource = AndroidMediaStoreVideoCatalogDataSource(application),
        recentPlaybackDataSource = PlaybackRecentMediaDataSource(
            playbackStateRepositoryProvider = bindings.playbackStateRepository,
            queueHistoryRepositoryProvider = bindings.queueHistoryRepository,
        ),
    )
}

private class AppMainActivityDependencies(
    private val application: Application,
    private val bindings: MainLibraryFeatureBindings,
    private val playbackLaunchCoordinator: PlaybackLaunchCoordinator,
    private val playbackActivityClass: Class<*>,
) : MainActivityDependencies {
    override val mainLibraryViewModelFactory: ViewModelProvider.Factory by lazy(LazyThreadSafetyMode.NONE) {
        val mediaLibraryRepository = createMediaLibraryRepository(application, bindings)
        MainLibraryViewModel.Factory(
            MainLibraryViewModelDependencies(
                uiSettingsRepository = bindings.uiSettingsRepository(),
                playerSettingsRepository = bindings.playerSettingsRepository(),
                resolveVideoAccessUseCase = ResolveVideoAccessUseCase(mediaLibraryRepository),
                loadFolderPageUseCase = LoadFolderPageUseCase(mediaLibraryRepository),
                loadVideoPageUseCase = LoadVideoPageUseCase(mediaLibraryRepository),
                loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(mediaLibraryRepository),
                resolveRecentMediaItemsUseCase = ResolveRecentMediaItemsUseCase(mediaLibraryRepository),
                observeMediaLibraryChangesUseCase = ObserveMediaLibraryChangesUseCase(mediaLibraryRepository),
            ),
        )
    }

    override fun createPlaybackLaunchRequest(
        payload: PlaybackIntentPayload,
    ): PlaybackLaunchRequest {
        return playbackLaunchCoordinator.createLaunchRequest(payload)
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

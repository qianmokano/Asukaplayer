package com.asuka.player.app

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModelProvider
import com.asuka.player.core.PlaybackActivityDependencies
import com.asuka.player.core.PlaybackDependenciesProvider
import com.asuka.player.core.PlaybackDeviceControllerFactory
import com.asuka.player.core.PlaybackRuntimeSettingsSource
import com.asuka.player.core.PlaybackSessionPlanner
import com.asuka.player.core.PlaybackServiceDependencies
import com.asuka.player.core.PlaybackStateWriter
import com.asuka.player.core.PlaybackUiPersistence
import com.asuka.player.core.QueueHistoryWriter
import com.asuka.player.ui.activity.PlaybackActivity

class AsuraPlayerApp : Application(), MainActivityDependenciesProvider, PlaybackDependenciesProvider {
    private lateinit var graph: AsukaAppGraph
    private lateinit var mainActivityEntryPointInternal: MainActivityDependencies
    private lateinit var playbackActivityEntryPointInternal: PlaybackActivityDependencies
    private lateinit var serviceDependenciesInternal: PlaybackServiceDependencies

    /**
     * Override in tests to inject a fake/stub graph without subclassing the Application.
     * Must be set before [onCreate] is called (i.e. before Robolectric starts the app).
     */
    internal var graphFactory: (Application) -> AsukaAppGraph = ::AsukaAppGraph

    override val mainActivityDependencies: MainActivityDependencies
        get() = mainActivityEntryPointInternal

    override val playbackActivityDependencies: PlaybackActivityDependencies
        get() = playbackActivityEntryPointInternal

    override val playbackServiceDependencies: PlaybackServiceDependencies
        get() = serviceDependenciesInternal

    override fun onCreate() {
        super.onCreate()
        graph = graphFactory(this)
        val videoAccessDataSource = AndroidVideoAccessDataSource(this)
        val localVideoCatalogDataSource = AndroidMediaStoreVideoCatalogDataSource(this)
        val recentPlaybackDataSource = PlaybackRecentMediaDataSource(
            playbackStateRepository = graph.playbackStateRepository,
            queueHistoryRepository = graph.queueHistoryRepository,
        )
        val mediaLibraryRepository = AndroidMediaLibraryRepository(
            videoAccessDataSource = videoAccessDataSource,
            localVideoCatalogDataSource = localVideoCatalogDataSource,
            recentPlaybackDataSource = recentPlaybackDataSource,
        )
        val mainLibraryViewModelFactory: ViewModelProvider.Factory = MainLibraryViewModel.Factory(
            MainLibraryViewModelDependencies(
                appContext = this,
                uiSettingsRepository = graph.uiSettingsRepository,
                playerSettingsRepository = graph.playerSettingsRepository,
                resolveVideoAccessUseCase = ResolveVideoAccessUseCase(mediaLibraryRepository),
                refreshMediaLibraryUseCase = RefreshMediaLibraryUseCase(mediaLibraryRepository),
                loadRecentMediaIdsUseCase = LoadRecentMediaIdsUseCase(mediaLibraryRepository),
            ),
        )
        mainActivityEntryPointInternal = AppMainActivityDependencies(
            playbackLaunchCoordinator = graph.playbackLaunchCoordinator,
            playbackActivityClass = PlaybackActivity::class.java,
            mainLibraryViewModelFactory = mainLibraryViewModelFactory,
        )
        playbackActivityEntryPointInternal = AppPlaybackActivityDependencies(
            graph = graph,
            playbackServiceComponent = graph.playbackPlatformBindings.playbackServiceComponent,
        )
        serviceDependenciesInternal = AppPlaybackServiceDependencies(
            graph = graph,
            sessionActivityClass = PlaybackActivity::class.java,
            notificationSmallIconResId = graph.playbackPlatformBindings.notificationSmallIconResId,
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

private class AppPlaybackActivityDependencies(
    private val graph: AsukaAppGraph,
    override val playbackServiceComponent: ComponentName,
) : PlaybackActivityDependencies {
    override val playbackSessionPlanner: PlaybackSessionPlanner
        get() = graph.playbackSessionPlanner
    override val playbackRuntimeSettingsSource: PlaybackRuntimeSettingsSource
        get() = graph.playbackRuntimeSettingsSource
    override val playbackUiPersistence: PlaybackUiPersistence
        get() = graph.playbackUiPersistence
    override val playbackDeviceControllerFactory: PlaybackDeviceControllerFactory
        get() = graph.playbackDeviceControllerFactory
}

private class AppPlaybackServiceDependencies(
    private val graph: AsukaAppGraph,
    override val sessionActivityClass: Class<*>?,
    @get:DrawableRes override val notificationSmallIconResId: Int,
) : PlaybackServiceDependencies {
    override fun createPlaybackStateWriter(): PlaybackStateWriter {
        return graph.createPlaybackStateWriter()
    }

    override fun createQueueHistoryWriter(): QueueHistoryWriter {
        return graph.createQueueHistoryWriter()
    }
}

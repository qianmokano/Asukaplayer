package com.asuka.player.app

import android.app.Application
import androidx.annotation.DrawableRes
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackControllerConnectorFactory
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.renderer.activity.PlaybackActivity
import com.asuka.player.runtime.AsukaAppGraph

internal class AppComposition(
    private val application: Application,
    private val mainLibraryBindings: MainLibraryFeatureBindings,
    private val playbackActivityBindings: PlaybackActivityEntryBindings,
    private val playbackServiceBindings: PlaybackServiceEntryBindings,
) {
    val mainActivityDependencies: MainActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        MainLibraryFeatureInstaller.install(
            application = application,
            bindings = mainLibraryBindings,
            playbackActivityClass = PlaybackActivity::class.java,
        )
    }
    val playbackActivityDependencies: PlaybackActivityDependencies by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackFeatureEntryPointFactory.createActivityDependencies(playbackActivityBindings)
    }
    val playbackServiceDependencies: PlaybackServiceDependencies by lazy(LazyThreadSafetyMode.NONE) {
        PlaybackFeatureEntryPointFactory.createServiceDependencies(playbackServiceBindings)
    }
}

internal object AppCompositionFactory {
    fun create(
        application: Application,
        graph: AsukaAppGraph,
    ): AppComposition {
        return AppComposition(
            application = application,
            mainLibraryBindings = MainLibraryFeatureBindings(
                uiSettingsRepository = { graph.uiSettingsRepository },
                playerSettingsRepository = { graph.playerSettingsRepository },
                playbackStateRepository = { graph.playbackStateRepository },
                queueHistoryRepository = { graph.queueHistoryRepository },
                playbackLaunchCoordinator = { graph.playbackLaunchCoordinator },
            ),
            playbackActivityBindings = PlaybackActivityEntryBindings(
                playbackSessionPlanner = { graph.playbackSessionPlanner },
                playbackRuntimeSettingsSource = { graph.playbackRuntimeSettingsSource },
                playbackUiPersistence = { graph.playbackUiPersistence },
                playbackPreviewFrameProvider = { graph.playbackPreviewFrameProvider },
                playbackDeviceControllerFactory = { graph.playbackDeviceControllerFactory },
                persistenceDegraded = { graph.persistenceDegraded },
                createPlaybackControllerConnector = { context ->
                    graph.playbackControllerConnectorFactory.create(
                        context = context,
                        playbackServiceComponent = graph.playbackPlatformBindings.playbackServiceComponent,
                    )
                },
            ),
            playbackServiceBindings = PlaybackServiceEntryBindings(
                playbackStore = { graph.playbackStore },
                queueHistoryStore = { graph.queueHistoryStore },
                sessionActivityClass = PlaybackActivity::class.java,
                notificationSmallIconResId = graph.playbackPlatformBindings.notificationSmallIconResId,
            ),
        )
    }
}

internal object PlaybackFeatureEntryPointFactory {
    fun createActivityDependencies(bindings: PlaybackActivityEntryBindings): PlaybackActivityDependencies {
        return AppPlaybackActivityDependencies(bindings)
    }

    fun createServiceDependencies(
        bindings: PlaybackServiceEntryBindings,
    ): PlaybackServiceDependencies {
        return AppPlaybackServiceDependencies(bindings)
    }
}

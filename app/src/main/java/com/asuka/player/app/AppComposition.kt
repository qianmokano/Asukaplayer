package com.asuka.player.app

import android.app.Application
import androidx.annotation.DrawableRes
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.runtime.AsukaAppGraph
import com.asuka.player.ui.activity.PlaybackActivity

internal data class AppComposition(
    val mainActivityDependencies: MainActivityDependencies,
    val playbackActivityDependencies: PlaybackActivityDependencies,
    val playbackServiceDependencies: PlaybackServiceDependencies,
)

internal object AppCompositionFactory {
    fun create(
        application: Application,
        graph: AsukaAppGraph,
    ): AppComposition {
        return AppComposition(
            mainActivityDependencies = MainLibraryFeatureInstaller.install(
                application = application,
                graph = graph,
                playbackActivityClass = PlaybackActivity::class.java,
            ),
            playbackActivityDependencies = PlaybackFeatureEntryPointFactory.createActivityDependencies(graph),
            playbackServiceDependencies = PlaybackFeatureEntryPointFactory.createServiceDependencies(
                graph = graph,
                sessionActivityClass = PlaybackActivity::class.java,
                notificationSmallIconResId = graph.playbackPlatformBindings.notificationSmallIconResId,
            ),
        )
    }
}

internal object PlaybackFeatureEntryPointFactory {
    fun createActivityDependencies(graph: AsukaAppGraph): PlaybackActivityDependencies {
        return AppPlaybackActivityDependencies(graph)
    }

    fun createServiceDependencies(
        graph: AsukaAppGraph,
        sessionActivityClass: Class<*>?,
        @DrawableRes notificationSmallIconResId: Int,
    ): PlaybackServiceDependencies {
        return AppPlaybackServiceDependencies(
            graph = graph,
            sessionActivityClass = sessionActivityClass,
            notificationSmallIconResId = notificationSmallIconResId,
        )
    }
}

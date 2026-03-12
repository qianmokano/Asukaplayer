package com.asuka.player.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.asuka.player.platform.PlaybackActivityDependencies
import com.asuka.player.platform.PlaybackDependenciesProvider
import com.asuka.player.platform.PlaybackServiceDependencies
import com.asuka.player.renderer.activity.PlaybackThemeProvider
import com.asuka.player.runtime.AsukaAppGraph

class AsuraPlayerApp : Application(), MainActivityDependenciesProvider, PlaybackDependenciesProvider, PlaybackThemeProvider {
    /**
     * Override in tests to inject a fake/stub graph without subclassing the Application.
     * Must be set before [onCreate] is called (i.e. before Robolectric starts the app).
     */
    internal var graphFactory: (Application) -> AsukaAppGraph = ::AsukaAppGraph

    private val graph: AsukaAppGraph by lazy(LazyThreadSafetyMode.NONE) {
        graphFactory(this)
    }
    private val appComposition: AppComposition by lazy(LazyThreadSafetyMode.NONE) {
        AppCompositionFactory.create(
            application = this,
            graph = graph,
        )
    }

    override val mainActivityDependencies: MainActivityDependencies
        get() = appComposition.mainActivityDependencies

    override val playbackActivityDependencies: PlaybackActivityDependencies
        get() = appComposition.playbackActivityDependencies

    override val playbackServiceDependencies: PlaybackServiceDependencies
        get() = appComposition.playbackServiceDependencies

    @Composable
    override fun ProvidePlaybackTheme(content: @Composable () -> Unit) {
        val uiSettings by graph.uiSettingsRepository.settings.collectAsState()
        AsukaTheme(themeConfig = uiSettings.themeConfig) {
            CompositionLocalProvider(LocalHapticsEnabled provides uiSettings.hapticFeedbackEnabled) {
                content()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}

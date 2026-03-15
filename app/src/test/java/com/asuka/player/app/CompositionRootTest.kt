package com.asuka.player.app

import com.asuka.player.platform.PlaybackDependenciesProvider
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34], application = AsuraPlayerApp::class)
@RunWith(RobolectricTestRunner::class)
class CompositionRootTest {

    @Test
    fun mainActivityDependencies_resolveWithoutError() {
        val app = RuntimeEnvironment.getApplication() as AsuraPlayerApp
        val deps = MainActivityDependenciesProvider.from(app).mainActivityDependencies

        assertNotNull(deps.mainLibraryViewModelFactory)
    }

    @Test
    fun playbackActivityDependencies_resolveWithoutError() {
        val app = RuntimeEnvironment.getApplication() as AsuraPlayerApp
        val deps = PlaybackDependenciesProvider.from(app).playbackActivityDependencies

        assertNotNull(deps.playbackSessionPlanner)
        assertNotNull(deps.playbackRuntimeSettingsSource)
        assertNotNull(deps.playbackUiPersistence)
        assertNotNull(deps.playbackPreviewFrameProvider)
        assertNotNull(deps.playbackDeviceControllerFactory)
        assertNotNull(deps.persistenceDegraded)
        assertNotNull(deps.createPlaybackControllerConnector(app))
    }

    @Test
    fun playbackServiceDependencies_resolveWithoutError() {
        val app = RuntimeEnvironment.getApplication() as AsuraPlayerApp
        val deps = PlaybackDependenciesProvider.from(app).playbackServiceDependencies

        assertNotNull(deps.playbackStore)
        assertNotNull(deps.queueHistoryStore)
        assertNotNull(deps.sessionActivityClass)
        assertTrue(deps.notificationSmallIconResId != 0)
    }
}

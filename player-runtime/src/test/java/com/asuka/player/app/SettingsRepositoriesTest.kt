package com.asuka.player.runtime

import android.content.Context
import com.asuka.player.contract.PlayerSettings
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.data.AppSettingsStore
import com.asuka.player.data.AppSettingsSnapshot
import com.asuka.player.data.DataStoreAppSettingsStore
import com.asuka.player.data.PlaybackBehaviorRecord
import com.asuka.player.data.PlayerSettingsRecord
import com.asuka.player.data.SharedPreferencesAppSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoriesTest {
    private fun testScope() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun uiSettingsRepository_exposesLatestSharedState() = runBlocking {
        val store = freshStore()
        val repository = UiSettingsRepository(store, testScope())

        repository.setThemeConfig(ThemeConfig(
            mode = ThemeMode.Custom,
            customSeedArgb = 0xFF2E6CF6.toInt(),
            appearance = ThemeAppearanceMode.Dark,
            pureBlack = false,
            fontScale = 1.2f,
            fontScaleEnabled = true,
        ))
        repository.setNavDurationMs(480)
        repository.setHapticFeedbackEnabled(false)

        val state = repository.settings.value
        assertEquals(ThemeMode.Custom, state.themeConfig.mode)
        assertEquals(0xFF2E6CF6.toInt(), state.themeConfig.customSeedArgb)
        assertEquals(ThemeAppearanceMode.Dark, state.themeConfig.appearance)
        assertFalse(state.themeConfig.pureBlack)
        assertEquals(1.2f, state.themeConfig.fontScale)
        assertTrue(state.themeConfig.fontScaleEnabled)
        assertEquals(480, state.navDurationMs)
        assertFalse(state.hapticFeedbackEnabled)
    }

    @Test
    fun playbackRuntimeSettingsSource_tracksRepositoryUpdates() = runBlocking {
        val store = freshStore()
        val playerSettingsRepository = PlayerSettingsRepository(store, testScope())
        val behaviorRepository = PlaybackBehaviorRepository(store, testScope())
        val source = AppPlaybackRuntimeSettingsSource(
            playerSettingsRepository = playerSettingsRepository,
            playbackBehaviorRepository = behaviorRepository,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        playerSettingsRepository.setPlayerSettings(playerSettingsRepository.settings.value.copy(
            autoPip = false,
            seekIncrementSec = 15,
            controllerTimeoutSec = 7,
        ))
        behaviorRepository.setKeepConnectionInBackground(false)

        val runtime = source.current()
        assertFalse(runtime.autoPip)
        assertEquals(15, runtime.seekIncrementSec)
        assertEquals(7, runtime.controllerTimeoutSec)
        assertFalse(runtime.keepSessionConnectionInBackground)
        assertFalse(runtime.hideButtonsBackground)
    }

    @Test
    fun playbackRuntimeSettings_defaultsStayAlignedWithPlayerSettingsDefaults() {
        assertEquals(
            PlayerSettings(),
            PlaybackRuntimeSettings().playerSettings,
        )
    }

    @Test
    fun playbackBehaviorRepository_persistsRememberedBrightness() = runBlocking {
        val repository = PlaybackBehaviorRepository(freshStore(), testScope())

        repository.setRememberedBrightness(0.42f)

        assertEquals(0.42f, repository.rememberedBrightness)
    }

    @Test
    fun repositories_waitForStoreInitializationBeforePublishingInitialValues() {
        val store = AwaitableAppSettingsStore(
            loadedSnapshot = AppSettingsSnapshot(
                playerSettings = PlayerSettingsRecord(
                    autoplay = false,
                    defaultPlaybackSpeed = 1.5f,
                ),
                playbackBehavior = PlaybackBehaviorRecord(
                    keepConnectionInBackground = false,
                ),
            ),
        )

        val playerSettingsRepository = PlayerSettingsRepository(store, testScope())
        val playbackBehaviorRepository = PlaybackBehaviorRepository(store, testScope())
        val source = AppPlaybackRuntimeSettingsSource(
            playerSettingsRepository = playerSettingsRepository,
            playbackBehaviorRepository = playbackBehaviorRepository,
            scope = testScope(),
        )

        assertTrue(store.awaitLoadedCalled)
        assertFalse(playerSettingsRepository.settings.value.autoplay)
        assertEquals(1.5f, playerSettingsRepository.settings.value.defaultPlaybackSpeed)
        assertFalse(source.current().keepSessionConnectionInBackground)
    }

    private fun freshStore(): AppSettingsStore {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences(SharedPreferencesAppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        context.filesDir.resolve("datastore/app_settings.json").delete()
        return DataStoreAppSettingsStore(
            context = context,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
    }
}

private class AwaitableAppSettingsStore(
    private val loadedSnapshot: AppSettingsSnapshot,
) : AppSettingsStore {
    private var loaded = false
    var awaitLoadedCalled: Boolean = false
        private set

    override val snapshots = MutableStateFlow(AppSettingsSnapshot())

    override suspend fun awaitLoaded() {
        awaitLoadedCalled = true
        if (loaded) return
        loaded = true
        snapshots.value = loadedSnapshot
    }

    override fun loadSnapshot(): AppSettingsSnapshot {
        return if (loaded) loadedSnapshot else AppSettingsSnapshot()
    }

    override suspend fun saveSnapshot(snapshot: AppSettingsSnapshot) {
        loaded = true
        snapshots.value = snapshot
    }
}

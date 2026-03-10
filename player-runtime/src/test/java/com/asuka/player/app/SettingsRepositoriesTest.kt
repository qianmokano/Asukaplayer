package com.asuka.player.app

import com.asuka.player.core.PlayerSettings
import android.content.Context
import com.asuka.player.core.PlaybackRuntimeSettings
import com.asuka.player.data.SharedPreferencesAppSettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoriesTest {

    @Test
    fun uiSettingsRepository_exposesLatestSharedState() {
        val store = freshStore()
        val repository = UiSettingsRepository(store)

        repository.themeConfig = ThemeConfig(
            mode = ThemeMode.Custom,
            customSeedArgb = 0xFF2E6CF6.toInt(),
            appearance = ThemeAppearanceMode.Dark,
            pureBlack = false,
            fontScale = 1.2f,
            fontScaleEnabled = true,
        )
        repository.navDurationMs = 480
        repository.hapticFeedbackEnabled = false

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
    fun playbackRuntimeSettingsSource_tracksRepositoryUpdates() {
        val store = freshStore()
        val playerSettingsRepository = PlayerSettingsRepository(store)
        val behaviorRepository = PlaybackBehaviorRepository(store)
        val source = AppPlaybackRuntimeSettingsSource(
            playerSettingsRepository = playerSettingsRepository,
            playbackBehaviorRepository = behaviorRepository,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        playerSettingsRepository.playerSettings = playerSettingsRepository.playerSettings.copy(
            autoPip = false,
            seekIncrementSec = 15,
            controllerTimeoutSec = 7,
        )
        behaviorRepository.keepConnectionInBackground = false

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
    fun playbackBehaviorRepository_persistsRememberedBrightness() {
        val repository = PlaybackBehaviorRepository(freshStore())

        repository.rememberedBrightness = 0.42f

        assertEquals(0.42f, repository.rememberedBrightness)
    }

    private fun freshStore(): SharedPreferencesAppSettingsStore {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        return SharedPreferencesAppSettingsStore(context)
    }
}

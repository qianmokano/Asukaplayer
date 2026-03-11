package com.asuka.player.data

import android.content.Context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DataStoreAppSettingsStoreTest {

    @Test
    fun saveSnapshot_roundTripsAcrossInstances() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        clearPersistence(context)

        val expected = AppSettingsSnapshot(
            uiSettings = UiSettingsRecord(
                themeMode = "Custom",
                themeAppearance = "Dark",
                customSeedArgb = 0xFF123456.toInt(),
                customThemeId = "theme-1",
                customThemes = listOf(
                    CustomThemeRecord(
                        id = "theme-1",
                        name = "Slate",
                        seedArgb = 0xFF223344.toInt(),
                        monochrome = true,
                    ),
                ),
                navDurationMs = 480,
                hapticFeedbackEnabled = false,
            ),
            playerSettings = PlayerSettingsRecord(
                seekIncrementSec = 15,
                seekSensitivity = 1.7f,
                longPressSpeed = 3.0f,
                controllerTimeoutSec = 9,
                autoplay = false,
                autoBackgroundPlay = true,
            ),
            playbackBehavior = PlaybackBehaviorRecord(
                keepConnectionInBackground = false,
                rememberedBrightness = 0.42f,
            ),
        ).normalized()

        val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val store = dataStore(context, storeScope)
        store.saveSnapshot(expected)
        store.awaitLoaded()
        storeScope.cancel()

        val restored = dataStore(context)
        restored.awaitLoaded()

        assertEquals(expected, restored.loadSnapshot())
    }

    @Test
    fun migrate_legacySharedPreferences_onFirstRead() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        clearPersistence(context)

        SharedPreferencesAppSettingsStore(context).saveSnapshot(
            AppSettingsSnapshot(
                uiSettings = UiSettingsRecord(
                    themeMode = "Custom",
                    themeAppearance = "Dark",
                    navDurationMs = 512,
                ),
                playerSettings = PlayerSettingsRecord(
                    autoplay = false,
                    defaultPlaybackSpeed = 1.5f,
                ),
                playbackBehavior = PlaybackBehaviorRecord(
                    keepConnectionInBackground = false,
                    rememberedBrightness = 0.33f,
                ),
            ),
        )

        val store = dataStore(context)
        store.awaitLoaded()
        val migrated = store.loadSnapshot()

        assertEquals("Custom", migrated.uiSettings.themeMode)
        assertEquals("Dark", migrated.uiSettings.themeAppearance)
        assertEquals(512, migrated.uiSettings.navDurationMs)
        assertEquals(false, migrated.playerSettings.autoplay)
        assertEquals(1.5f, migrated.playerSettings.defaultPlaybackSpeed)
        assertEquals(false, migrated.playbackBehavior.keepConnectionInBackground)
        assertEquals(0.33f, migrated.playbackBehavior.rememberedBrightness)
    }

    private fun dataStore(
        context: Context,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    ): DataStoreAppSettingsStore {
        return DataStoreAppSettingsStore(
            context = context,
            scope = scope,
        )
    }

    private fun clearPersistence(context: Context) {
        context.getSharedPreferences(SharedPreferencesAppSettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.filesDir.resolve("datastore/app_settings.json").delete()
    }
}

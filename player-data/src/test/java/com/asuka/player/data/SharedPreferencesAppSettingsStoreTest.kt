package com.asuka.player.data

import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesAppSettingsStoreTest {

    @Test
    fun roundTrip_persistsUiPlayerAndBehaviorSettings() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
        val store = SharedPreferencesAppSettingsStore(context)

        val uiRecord = UiSettingsRecord(
            themeMode = "Custom",
            themeAppearance = "Dark",
            customSeedArgb = 0xFF123456.toInt(),
            customThemeId = "theme-1",
            customMonochrome = true,
            pureBlack = false,
            fontScale = 1.2f,
            fontScaleEnabled = true,
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
        )
        val playerRecord = PlayerSettingsRecord(
            seekGestureEnabled = false,
            brightnessGestureEnabled = false,
            volumeGestureEnabled = false,
            zoomGestureEnabled = false,
            panGestureEnabled = false,
            doubleTapGestureEnabled = false,
            doubleTapAction = "both",
            longPressGestureEnabled = false,
            seekIncrementSec = 15,
            seekSensitivity = 1.7f,
            longPressSpeed = 3.0f,
            controllerTimeoutSec = 9,
            hideButtonsBackground = true,
            resumePlayback = false,
            defaultPlaybackSpeed = 1.5f,
            autoplay = false,
            autoPip = false,
            autoBackgroundPlay = true,
            rememberBrightness = true,
            rememberSelections = false,
        )
        val behaviorRecord = PlaybackBehaviorRecord(keepConnectionInBackground = false)

        store.saveUiSettings(uiRecord)
        store.savePlayerSettings(playerRecord)
        store.savePlaybackBehavior(behaviorRecord)

        assertEquals(uiRecord, store.loadUiSettings())
        assertEquals(playerRecord, store.loadPlayerSettings())
        assertEquals(behaviorRecord, store.loadPlaybackBehavior())
    }
}

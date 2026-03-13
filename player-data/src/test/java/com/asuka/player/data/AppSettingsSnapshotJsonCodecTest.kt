package com.asuka.player.data

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AppSettingsSnapshotJsonCodecTest {

    @Test
    fun decode_missingFieldsBackfillsDefaults() {
        val snapshot = AppSettingsSnapshotJsonCodec.decode(
            """
            {
              "uiSettings": {
                "themeMode": "Custom"
              },
              "playerSettings": {
                "defaultPlaybackSpeed": 1.5
              }
            }
            """.trimIndent(),
        )

        assertEquals("Custom", snapshot.uiSettings.themeMode)
        assertEquals("System", snapshot.uiSettings.themeAppearance)
        assertEquals(300, snapshot.uiSettings.navDurationMs)
        assertEquals(1.5f, snapshot.playerSettings.defaultPlaybackSpeed)
        assertEquals("toggle_play_pause", snapshot.playerSettings.doubleTapAction)
        assertEquals(true, snapshot.playerSettings.autoplay)
        assertEquals(true, snapshot.playbackBehavior.keepConnectionInBackground)
    }

    @Test
    fun decode_ignoresUnknownFields_andNormalizesValues() {
        val snapshot = AppSettingsSnapshotJsonCodec.decode(
            """
            {
              "schemaVersion": 999,
              "unknownRoot": true,
              "uiSettings": {
                "navDurationMs": 99999,
                "fontScale": 99.0,
                "unknownUi": "ignored"
              },
              "playerSettings": {
                "seekIncrementSec": 0,
                "defaultPlaybackSpeed": 99.0
              },
              "playbackBehavior": {
                "rememberedBrightness": 9.0
              }
            }
            """.trimIndent(),
        )

        assertEquals(AppSettingsSnapshot.CURRENT_SCHEMA_VERSION, snapshot.schemaVersion)
        assertEquals(2000, snapshot.uiSettings.navDurationMs)
        assertEquals(1.3f, snapshot.uiSettings.fontScale)
        assertEquals(1, snapshot.playerSettings.seekIncrementSec)
        assertEquals(4.0f, snapshot.playerSettings.defaultPlaybackSpeed)
        assertEquals(1.0f, snapshot.playbackBehavior.rememberedBrightness)
    }
}

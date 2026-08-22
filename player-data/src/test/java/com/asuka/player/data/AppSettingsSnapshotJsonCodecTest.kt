package com.asuka.player.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertEquals(10, snapshot.playerSettings.seekBackwardSec)
        assertEquals(10, snapshot.playerSettings.seekForwardSec)
        assertEquals(true, snapshot.playerSettings.autoplay)
        assertEquals(false, snapshot.playerSettings.rememberVolume)
        assertEquals(true, snapshot.playbackBehavior.keepConnectionInBackground)
        assertEquals(null, snapshot.playbackBehavior.rememberedVolumePercent)
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
                "seekBackwardSec": 0,
                "seekForwardSec": 61,
                "defaultPlaybackSpeed": 99.0
              },
              "playbackBehavior": {
                "rememberedBrightness": 9.0,
                "rememberedVolumePercent": 999
              }
            }
            """.trimIndent(),
        )

        assertEquals(AppSettingsSnapshot.CURRENT_SCHEMA_VERSION, snapshot.schemaVersion)
        assertEquals(2000, snapshot.uiSettings.navDurationMs)
        assertEquals(1.3f, snapshot.uiSettings.fontScale)
        assertEquals(10, snapshot.playerSettings.seekBackwardSec)
        assertEquals(10, snapshot.playerSettings.seekForwardSec)
        assertEquals(4.0f, snapshot.playerSettings.defaultPlaybackSpeed)
        assertEquals(1.0f, snapshot.playbackBehavior.rememberedBrightness)
        assertEquals(100, snapshot.playbackBehavior.rememberedVolumePercent)
    }

    @Test
    fun decode_legacySeekIncrementMigratesToBothDirections() {
        val snapshot = AppSettingsSnapshotJsonCodec.decode(
            """
            {
              "schemaVersion": 1,
              "playerSettings": {
                "seekIncrementSec": 15
              }
            }
            """.trimIndent(),
        )

        assertEquals(AppSettingsSnapshot.CURRENT_SCHEMA_VERSION, snapshot.schemaVersion)
        assertEquals(15, snapshot.playerSettings.seekBackwardSec)
        assertEquals(15, snapshot.playerSettings.seekForwardSec)
    }

    @Test
    fun encode_writesDirectionalIntervalsWithoutLegacyKey() {
        val encoded = AppSettingsSnapshotJsonCodec.encode(
            AppSettingsSnapshot(
                playerSettings = PlayerSettingsRecord(
                    seekBackwardSec = 5,
                    seekForwardSec = 15,
                ),
            ),
        )

        val decoded = AppSettingsSnapshotJsonCodec.decode(encoded)
        assertEquals(5, decoded.playerSettings.seekBackwardSec)
        assertEquals(15, decoded.playerSettings.seekForwardSec)
        assertFalse(encoded.contains("\"seekIncrementSec\""))
    }
}

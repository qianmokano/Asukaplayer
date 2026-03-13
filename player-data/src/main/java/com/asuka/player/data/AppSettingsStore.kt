package com.asuka.player.data

import kotlinx.coroutines.flow.StateFlow

data class CustomThemeRecord(
    val id: String,
    val name: String,
    val seedArgb: Int,
    val monochrome: Boolean,
)

data class UiSettingsRecord(
    val themeMode: String = "Monochrome",
    val themeAppearance: String = "System",
    val customSeedArgb: Int? = null,
    val customThemeId: String? = null,
    val customMonochrome: Boolean = false,
    val pureBlack: Boolean = true,
    val fontScale: Float = 1.0f,
    val fontScaleEnabled: Boolean = false,
    val customThemes: List<CustomThemeRecord> = emptyList(),
    val navDurationMs: Int = 300,
    val hapticFeedbackEnabled: Boolean = true,
)

data class PlayerSettingsRecord(
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val panGestureEnabled: Boolean = true,
    val doubleTapGestureEnabled: Boolean = true,
    val doubleTapAction: String = "toggle_play_pause",
    val longPressGestureEnabled: Boolean = true,
    val seekIncrementSec: Int = 10,
    val seekSensitivity: Float = 1.0f,
    val longPressSpeed: Float = 2.0f,
    val controllerTimeoutSec: Int = 3,
    val hideButtonsBackground: Boolean = false,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val rememberBrightness: Boolean = false,
    val rememberSelections: Boolean = true,
)

data class PlaybackBehaviorRecord(
    val keepConnectionInBackground: Boolean = true,
    val rememberedBrightness: Float? = null,
)

data class AppSettingsSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val uiSettings: UiSettingsRecord = UiSettingsRecord(),
    val playerSettings: PlayerSettingsRecord = PlayerSettingsRecord(),
    val playbackBehavior: PlaybackBehaviorRecord = PlaybackBehaviorRecord(),
) {
    fun normalized(): AppSettingsSnapshot {
        return copy(
            schemaVersion = CURRENT_SCHEMA_VERSION,
            uiSettings = uiSettings.normalized(),
            playerSettings = playerSettings.normalized(),
            playbackBehavior = playbackBehavior.normalized(),
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

interface AppSettingsStore {
    val snapshots: StateFlow<AppSettingsSnapshot>

    suspend fun awaitLoaded() = Unit

    // Returns the latest in-memory snapshot without forcing storage initialization.
    fun loadSnapshot(): AppSettingsSnapshot = snapshots.value
    suspend fun saveSnapshot(snapshot: AppSettingsSnapshot)

    fun loadUiSettings(): UiSettingsRecord = loadSnapshot().uiSettings
    suspend fun saveUiSettings(record: UiSettingsRecord) {
        saveSnapshot(loadSnapshot().copy(uiSettings = record).normalized())
    }

    fun loadPlayerSettings(): PlayerSettingsRecord = loadSnapshot().playerSettings
    suspend fun savePlayerSettings(record: PlayerSettingsRecord) {
        saveSnapshot(loadSnapshot().copy(playerSettings = record).normalized())
    }

    fun loadPlaybackBehavior(): PlaybackBehaviorRecord = loadSnapshot().playbackBehavior
    suspend fun savePlaybackBehavior(record: PlaybackBehaviorRecord) {
        saveSnapshot(loadSnapshot().copy(playbackBehavior = record).normalized())
    }
}

internal fun UiSettingsRecord.normalized(): UiSettingsRecord {
    return copy(
        fontScale = fontScale.coerceIn(0.85f, 1.3f),
        customThemes = customThemes.normalized(),
        navDurationMs = navDurationMs.coerceIn(0, 2000),
    )
}

internal fun PlayerSettingsRecord.normalized(): PlayerSettingsRecord {
    return copy(
        seekIncrementSec = seekIncrementSec.coerceIn(1, 60),
        seekSensitivity = seekSensitivity.coerceIn(0.1f, 2.0f),
        longPressSpeed = longPressSpeed.coerceIn(0.2f, 4.0f),
        controllerTimeoutSec = controllerTimeoutSec.coerceIn(1, 60),
        defaultPlaybackSpeed = defaultPlaybackSpeed.coerceIn(0.2f, 4.0f),
    )
}

internal fun PlaybackBehaviorRecord.normalized(): PlaybackBehaviorRecord {
    return copy(
        rememberedBrightness = rememberedBrightness?.coerceIn(0f, 1f),
    )
}

private fun List<CustomThemeRecord>.normalized(): List<CustomThemeRecord> {
    return filter { it.id.isNotBlank() && it.name.isNotBlank() }
}

internal object AppSettingsSnapshotJsonCodec {
    fun encode(snapshot: AppSettingsSnapshot): String {
        val normalized = snapshot.normalized()
        val root = org.json.JSONObject()
        root.put(Keys.SCHEMA_VERSION, normalized.schemaVersion)
        root.put(Keys.UI_SETTINGS, encodeUiSettings(normalized.uiSettings))
        root.put(Keys.PLAYER_SETTINGS, encodePlayerSettings(normalized.playerSettings))
        root.put(Keys.PLAYBACK_BEHAVIOR, encodePlaybackBehavior(normalized.playbackBehavior))
        return root.toString()
    }

    fun decode(raw: String): AppSettingsSnapshot {
        if (raw.isBlank()) return AppSettingsSnapshot()
        val root = org.json.JSONObject(raw)
        return AppSettingsSnapshot(
            schemaVersion = root.optInt(Keys.SCHEMA_VERSION, AppSettingsSnapshot.CURRENT_SCHEMA_VERSION),
            uiSettings = decodeUiSettings(root.optJSONObject(Keys.UI_SETTINGS)),
            playerSettings = decodePlayerSettings(root.optJSONObject(Keys.PLAYER_SETTINGS)),
            playbackBehavior = decodePlaybackBehavior(root.optJSONObject(Keys.PLAYBACK_BEHAVIOR)),
        ).normalized()
    }

    private fun encodeUiSettings(record: UiSettingsRecord): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put(UiKeys.THEME_MODE, record.themeMode)
            put(UiKeys.THEME_APPEARANCE, record.themeAppearance)
            if (record.customSeedArgb != null) put(UiKeys.CUSTOM_SEED_ARGB, record.customSeedArgb)
            put(UiKeys.CUSTOM_THEME_ID, record.customThemeId)
            put(UiKeys.CUSTOM_MONOCHROME, record.customMonochrome)
            put(UiKeys.PURE_BLACK, record.pureBlack)
            put(UiKeys.FONT_SCALE, record.fontScale)
            put(UiKeys.FONT_SCALE_ENABLED, record.fontScaleEnabled)
            put(UiKeys.CUSTOM_THEMES, org.json.JSONArray().apply {
                record.customThemes.forEach { theme ->
                    put(
                        org.json.JSONObject().apply {
                            put(ThemeKeys.ID, theme.id)
                            put(ThemeKeys.NAME, theme.name)
                            put(ThemeKeys.SEED_ARGB, theme.seedArgb)
                            put(ThemeKeys.MONOCHROME, theme.monochrome)
                        },
                    )
                }
            })
            put(UiKeys.NAV_DURATION_MS, record.navDurationMs)
            put(UiKeys.HAPTIC_FEEDBACK_ENABLED, record.hapticFeedbackEnabled)
        }
    }

    private fun decodeUiSettings(obj: org.json.JSONObject?): UiSettingsRecord {
        return UiSettingsRecord(
            themeMode = obj?.optString(UiKeys.THEME_MODE, UiSettingsRecord().themeMode) ?: UiSettingsRecord().themeMode,
            themeAppearance = obj?.optString(UiKeys.THEME_APPEARANCE, UiSettingsRecord().themeAppearance)
                ?: UiSettingsRecord().themeAppearance,
            customSeedArgb = obj?.takeIf { it.has(UiKeys.CUSTOM_SEED_ARGB) }?.optInt(UiKeys.CUSTOM_SEED_ARGB),
            customThemeId = obj
                ?.takeIf { it.has(UiKeys.CUSTOM_THEME_ID) }
                ?.optString(UiKeys.CUSTOM_THEME_ID),
            customMonochrome = obj?.optBoolean(UiKeys.CUSTOM_MONOCHROME, false) ?: false,
            pureBlack = obj?.optBoolean(UiKeys.PURE_BLACK, true) ?: true,
            fontScale = obj?.optDouble(UiKeys.FONT_SCALE, 1.0)?.toFloat() ?: 1.0f,
            fontScaleEnabled = obj?.optBoolean(UiKeys.FONT_SCALE_ENABLED, false) ?: false,
            customThemes = decodeCustomThemes(obj?.optJSONArray(UiKeys.CUSTOM_THEMES)),
            navDurationMs = obj?.optInt(UiKeys.NAV_DURATION_MS, 300) ?: 300,
            hapticFeedbackEnabled = obj?.optBoolean(UiKeys.HAPTIC_FEEDBACK_ENABLED, true) ?: true,
        ).normalized()
    }

    private fun encodePlayerSettings(record: PlayerSettingsRecord): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put(PlayerKeys.SEEK_GESTURE_ENABLED, record.seekGestureEnabled)
            put(PlayerKeys.BRIGHTNESS_GESTURE_ENABLED, record.brightnessGestureEnabled)
            put(PlayerKeys.VOLUME_GESTURE_ENABLED, record.volumeGestureEnabled)
            put(PlayerKeys.ZOOM_GESTURE_ENABLED, record.zoomGestureEnabled)
            put(PlayerKeys.PAN_GESTURE_ENABLED, record.panGestureEnabled)
            put(PlayerKeys.DOUBLE_TAP_GESTURE_ENABLED, record.doubleTapGestureEnabled)
            put(PlayerKeys.DOUBLE_TAP_ACTION, record.doubleTapAction)
            put(PlayerKeys.LONG_PRESS_GESTURE_ENABLED, record.longPressGestureEnabled)
            put(PlayerKeys.SEEK_INCREMENT_SEC, record.seekIncrementSec)
            put(PlayerKeys.SEEK_SENSITIVITY, record.seekSensitivity)
            put(PlayerKeys.LONG_PRESS_SPEED, record.longPressSpeed)
            put(PlayerKeys.CONTROLLER_TIMEOUT_SEC, record.controllerTimeoutSec)
            put(PlayerKeys.HIDE_BUTTONS_BACKGROUND, record.hideButtonsBackground)
            put(PlayerKeys.RESUME_PLAYBACK, record.resumePlayback)
            put(PlayerKeys.DEFAULT_PLAYBACK_SPEED, record.defaultPlaybackSpeed)
            put(PlayerKeys.AUTOPLAY, record.autoplay)
            put(PlayerKeys.AUTO_PIP, record.autoPip)
            put(PlayerKeys.AUTO_BACKGROUND_PLAY, record.autoBackgroundPlay)
            put(PlayerKeys.REMEMBER_BRIGHTNESS, record.rememberBrightness)
            put(PlayerKeys.REMEMBER_SELECTIONS, record.rememberSelections)
        }
    }

    private fun decodePlayerSettings(obj: org.json.JSONObject?): PlayerSettingsRecord {
        return PlayerSettingsRecord(
            seekGestureEnabled = obj?.optBoolean(PlayerKeys.SEEK_GESTURE_ENABLED, true) ?: true,
            brightnessGestureEnabled = obj?.optBoolean(PlayerKeys.BRIGHTNESS_GESTURE_ENABLED, true) ?: true,
            volumeGestureEnabled = obj?.optBoolean(PlayerKeys.VOLUME_GESTURE_ENABLED, true) ?: true,
            zoomGestureEnabled = obj?.optBoolean(PlayerKeys.ZOOM_GESTURE_ENABLED, true) ?: true,
            panGestureEnabled = obj?.optBoolean(PlayerKeys.PAN_GESTURE_ENABLED, true) ?: true,
            doubleTapGestureEnabled = obj?.optBoolean(PlayerKeys.DOUBLE_TAP_GESTURE_ENABLED, true) ?: true,
            doubleTapAction = obj?.optString(PlayerKeys.DOUBLE_TAP_ACTION, "toggle_play_pause") ?: "toggle_play_pause",
            longPressGestureEnabled = obj?.optBoolean(PlayerKeys.LONG_PRESS_GESTURE_ENABLED, true) ?: true,
            seekIncrementSec = obj?.optInt(PlayerKeys.SEEK_INCREMENT_SEC, 10) ?: 10,
            seekSensitivity = obj?.optDouble(PlayerKeys.SEEK_SENSITIVITY, 1.0)?.toFloat() ?: 1.0f,
            longPressSpeed = obj?.optDouble(PlayerKeys.LONG_PRESS_SPEED, 2.0)?.toFloat() ?: 2.0f,
            controllerTimeoutSec = obj?.optInt(PlayerKeys.CONTROLLER_TIMEOUT_SEC, 3) ?: 3,
            hideButtonsBackground = obj?.optBoolean(PlayerKeys.HIDE_BUTTONS_BACKGROUND, false) ?: false,
            resumePlayback = obj?.optBoolean(PlayerKeys.RESUME_PLAYBACK, true) ?: true,
            defaultPlaybackSpeed = obj?.optDouble(PlayerKeys.DEFAULT_PLAYBACK_SPEED, 1.0)?.toFloat() ?: 1.0f,
            autoplay = obj?.optBoolean(PlayerKeys.AUTOPLAY, true) ?: true,
            autoPip = obj?.optBoolean(PlayerKeys.AUTO_PIP, true) ?: true,
            autoBackgroundPlay = obj?.optBoolean(PlayerKeys.AUTO_BACKGROUND_PLAY, false) ?: false,
            rememberBrightness = obj?.optBoolean(PlayerKeys.REMEMBER_BRIGHTNESS, false) ?: false,
            rememberSelections = obj?.optBoolean(PlayerKeys.REMEMBER_SELECTIONS, true) ?: true,
        ).normalized()
    }

    private fun encodePlaybackBehavior(record: PlaybackBehaviorRecord): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put(BehaviorKeys.KEEP_CONNECTION_IN_BACKGROUND, record.keepConnectionInBackground)
            if (record.rememberedBrightness != null) {
                put(BehaviorKeys.REMEMBERED_BRIGHTNESS, record.rememberedBrightness)
            }
        }
    }

    private fun decodePlaybackBehavior(obj: org.json.JSONObject?): PlaybackBehaviorRecord {
        return PlaybackBehaviorRecord(
            keepConnectionInBackground = obj?.optBoolean(BehaviorKeys.KEEP_CONNECTION_IN_BACKGROUND, true) ?: true,
            rememberedBrightness = obj
                ?.takeIf { it.has(BehaviorKeys.REMEMBERED_BRIGHTNESS) }
                ?.optDouble(BehaviorKeys.REMEMBERED_BRIGHTNESS, 0.0)
                ?.toFloat(),
        ).normalized()
    }

    private fun decodeCustomThemes(array: org.json.JSONArray?): List<CustomThemeRecord> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.optString(ThemeKeys.ID)
                val name = obj.optString(ThemeKeys.NAME)
                if (id.isBlank() || name.isBlank() || !obj.has(ThemeKeys.SEED_ARGB)) continue
                add(
                    CustomThemeRecord(
                        id = id,
                        name = name,
                        seedArgb = obj.optInt(ThemeKeys.SEED_ARGB),
                        monochrome = obj.optBoolean(ThemeKeys.MONOCHROME, false),
                    ),
                )
            }
        }.normalized()
    }

    private object Keys {
        const val SCHEMA_VERSION = "schemaVersion"
        const val UI_SETTINGS = "uiSettings"
        const val PLAYER_SETTINGS = "playerSettings"
        const val PLAYBACK_BEHAVIOR = "playbackBehavior"
    }

    private object UiKeys {
        const val THEME_MODE = "themeMode"
        const val THEME_APPEARANCE = "themeAppearance"
        const val CUSTOM_SEED_ARGB = "customSeedArgb"
        const val CUSTOM_THEME_ID = "customThemeId"
        const val CUSTOM_MONOCHROME = "customMonochrome"
        const val PURE_BLACK = "pureBlack"
        const val FONT_SCALE = "fontScale"
        const val FONT_SCALE_ENABLED = "fontScaleEnabled"
        const val CUSTOM_THEMES = "customThemes"
        const val NAV_DURATION_MS = "navDurationMs"
        const val HAPTIC_FEEDBACK_ENABLED = "hapticFeedbackEnabled"
    }

    private object ThemeKeys {
        const val ID = "id"
        const val NAME = "name"
        const val SEED_ARGB = "seedArgb"
        const val MONOCHROME = "monochrome"
    }

    private object PlayerKeys {
        const val SEEK_GESTURE_ENABLED = "seekGestureEnabled"
        const val BRIGHTNESS_GESTURE_ENABLED = "brightnessGestureEnabled"
        const val VOLUME_GESTURE_ENABLED = "volumeGestureEnabled"
        const val ZOOM_GESTURE_ENABLED = "zoomGestureEnabled"
        const val PAN_GESTURE_ENABLED = "panGestureEnabled"
        const val DOUBLE_TAP_GESTURE_ENABLED = "doubleTapGestureEnabled"
        const val DOUBLE_TAP_ACTION = "doubleTapAction"
        const val LONG_PRESS_GESTURE_ENABLED = "longPressGestureEnabled"
        const val SEEK_INCREMENT_SEC = "seekIncrementSec"
        const val SEEK_SENSITIVITY = "seekSensitivity"
        const val LONG_PRESS_SPEED = "longPressSpeed"
        const val CONTROLLER_TIMEOUT_SEC = "controllerTimeoutSec"
        const val HIDE_BUTTONS_BACKGROUND = "hideButtonsBackground"
        const val RESUME_PLAYBACK = "resumePlayback"
        const val DEFAULT_PLAYBACK_SPEED = "defaultPlaybackSpeed"
        const val AUTOPLAY = "autoplay"
        const val AUTO_PIP = "autoPip"
        const val AUTO_BACKGROUND_PLAY = "autoBackgroundPlay"
        const val REMEMBER_BRIGHTNESS = "rememberBrightness"
        const val REMEMBER_SELECTIONS = "rememberSelections"
    }

    private object BehaviorKeys {
        const val KEEP_CONNECTION_IN_BACKGROUND = "keepConnectionInBackground"
        const val REMEMBERED_BRIGHTNESS = "rememberedBrightness"
    }
}

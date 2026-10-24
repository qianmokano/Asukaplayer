package com.asuka.player.data

import com.asuka.player.contract.SeekIntervalPolicy
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsStore {
    val snapshots: StateFlow<AppSettingsSnapshot>

    suspend fun awaitLoaded() = Unit

    // Returns the latest in-memory snapshot without forcing storage initialization.
    fun loadSnapshot(): AppSettingsSnapshot = snapshots.value
    suspend fun saveSnapshot(snapshot: AppSettingsSnapshot)

    suspend fun updateSnapshot(transform: (AppSettingsSnapshot) -> AppSettingsSnapshot) {
        saveSnapshot(transform(loadSnapshot()))
    }

    suspend fun saveUiSettings(record: UiSettingsRecord) {
        updateSnapshot { it.copy(uiSettings = record).normalized() }
    }
    suspend fun savePlayerSettings(record: PlayerSettingsRecord) {
        updateSnapshot { it.copy(playerSettings = record).normalized() }
    }
    suspend fun savePlaybackBehavior(record: PlaybackBehaviorRecord) {
        updateSnapshot { it.copy(playbackBehavior = record).normalized() }
    }
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
            put(UiKeys.APP_LANGUAGE, record.appLanguage)
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
            appLanguage = obj?.optString(UiKeys.APP_LANGUAGE, UiSettingsRecord().appLanguage)
                ?: UiSettingsRecord().appLanguage,
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
            put(PlayerKeys.SEEK_BACKWARD_SEC, record.seekBackwardSec)
            put(PlayerKeys.SEEK_FORWARD_SEC, record.seekForwardSec)
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
            put(PlayerKeys.REMEMBER_VOLUME, record.rememberVolume)
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
            seekBackwardSec = obj.readSeekInterval(PlayerKeys.SEEK_BACKWARD_SEC),
            seekForwardSec = obj.readSeekInterval(PlayerKeys.SEEK_FORWARD_SEC),
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
            rememberVolume = obj?.optBoolean(PlayerKeys.REMEMBER_VOLUME, false) ?: false,
            rememberSelections = obj?.optBoolean(PlayerKeys.REMEMBER_SELECTIONS, true) ?: true,
        ).normalized()
    }

    private fun encodePlaybackBehavior(record: PlaybackBehaviorRecord): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put(BehaviorKeys.KEEP_CONNECTION_IN_BACKGROUND, record.keepConnectionInBackground)
            if (record.rememberedBrightness != null) {
                put(BehaviorKeys.REMEMBERED_BRIGHTNESS, record.rememberedBrightness)
            }
            if (record.rememberedVolumePercent != null) {
                put(BehaviorKeys.REMEMBERED_VOLUME_PERCENT, record.rememberedVolumePercent)
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
            rememberedVolumePercent = obj
                ?.takeIf { it.has(BehaviorKeys.REMEMBERED_VOLUME_PERCENT) }
                ?.optInt(BehaviorKeys.REMEMBERED_VOLUME_PERCENT, 0),
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
        const val APP_LANGUAGE = "appLanguage"
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
        const val SEEK_BACKWARD_SEC = "seekBackwardSec"
        const val SEEK_FORWARD_SEC = "seekForwardSec"
        const val LEGACY_SEEK_INCREMENT_SEC = "seekIncrementSec"
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
        const val REMEMBER_VOLUME = "rememberVolume"
        const val REMEMBER_SELECTIONS = "rememberSelections"
    }

    private object BehaviorKeys {
        const val KEEP_CONNECTION_IN_BACKGROUND = "keepConnectionInBackground"
        const val REMEMBERED_BRIGHTNESS = "rememberedBrightness"
        const val REMEMBERED_VOLUME_PERCENT = "rememberedVolumePercent"
    }

    private fun org.json.JSONObject?.readSeekInterval(key: String): Int {
        if (this == null) return SeekIntervalPolicy.DEFAULT_SECONDS
        return when {
            has(key) -> optInt(key, SeekIntervalPolicy.DEFAULT_SECONDS)
            has(PlayerKeys.LEGACY_SEEK_INCREMENT_SEC) -> {
                optInt(PlayerKeys.LEGACY_SEEK_INCREMENT_SEC, SeekIntervalPolicy.DEFAULT_SECONDS)
            }
            else -> SeekIntervalPolicy.DEFAULT_SECONDS
        }
    }
}

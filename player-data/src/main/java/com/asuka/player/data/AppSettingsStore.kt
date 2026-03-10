package com.asuka.player.data

import android.content.Context

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
    val navDurationMs: Int = 350,
    val hapticFeedbackEnabled: Boolean = true,
)

data class PlayerSettingsRecord(
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val panGestureEnabled: Boolean = true,
    val doubleTapGestureEnabled: Boolean = true,
    val doubleTapAction: String = "seek",
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

interface AppSettingsStore {
    fun loadUiSettings(): UiSettingsRecord
    fun saveUiSettings(record: UiSettingsRecord)

    fun loadPlayerSettings(): PlayerSettingsRecord
    fun savePlayerSettings(record: PlayerSettingsRecord)

    fun loadPlaybackBehavior(): PlaybackBehaviorRecord
    fun savePlaybackBehavior(record: PlaybackBehaviorRecord)
}

class SharedPreferencesAppSettingsStore(context: Context) : AppSettingsStore {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private val lock = Any()

    override fun loadUiSettings(): UiSettingsRecord {
        synchronized(lock) {
            return UiSettingsRecord(
                themeMode = prefs.getString(Keys.THEME_MODE, UiSettingsRecord().themeMode) ?: UiSettingsRecord().themeMode,
                themeAppearance = prefs.getString(Keys.THEME_APPEARANCE, UiSettingsRecord().themeAppearance)
                    ?: UiSettingsRecord().themeAppearance,
                customSeedArgb = prefs.takeIf { it.contains(Keys.THEME_CUSTOM_SEED) }?.getInt(Keys.THEME_CUSTOM_SEED, 0),
                customThemeId = prefs.getString(Keys.THEME_CUSTOM_THEME_ID, null),
                customMonochrome = prefs.getBoolean(Keys.THEME_CUSTOM_MONO, false),
                pureBlack = prefs.getBoolean(Keys.THEME_PURE_BLACK, true),
                fontScale = prefs.getFloat(Keys.THEME_FONT_SCALE, 1.0f).coerceIn(0.85f, 1.3f),
                fontScaleEnabled = prefs.getBoolean(Keys.THEME_FONT_SCALE_ENABLED, false),
                customThemes = loadCustomThemes(),
                navDurationMs = prefs.getInt(Keys.NAV_DURATION_MS, 350).coerceIn(0, 2000),
                hapticFeedbackEnabled = prefs.getBoolean(Keys.HAPTIC_FEEDBACK_ENABLED, true),
            )
        }
    }

    override fun saveUiSettings(record: UiSettingsRecord) {
        synchronized(lock) {
            prefs.edit()
                .putString(Keys.THEME_MODE, record.themeMode)
                .putString(Keys.THEME_APPEARANCE, record.themeAppearance)
                .putBoolean(Keys.THEME_PURE_BLACK, record.pureBlack)
                .putFloat(Keys.THEME_FONT_SCALE, record.fontScale.coerceIn(0.85f, 1.3f))
                .putBoolean(Keys.THEME_FONT_SCALE_ENABLED, record.fontScaleEnabled)
                .putString(Keys.THEME_CUSTOM_THEME_ID, record.customThemeId)
                .putBoolean(Keys.THEME_CUSTOM_MONO, record.customMonochrome)
                .putInt(Keys.NAV_DURATION_MS, record.navDurationMs.coerceIn(0, 2000))
                .putBoolean(Keys.HAPTIC_FEEDBACK_ENABLED, record.hapticFeedbackEnabled)
                .putString(Keys.THEME_CUSTOM_THEMES_JSON, encodeCustomThemes(record.customThemes))
                .apply {
                    if (record.customSeedArgb != null) {
                        putInt(Keys.THEME_CUSTOM_SEED, record.customSeedArgb)
                    } else {
                        remove(Keys.THEME_CUSTOM_SEED)
                    }
                }
                .apply()
        }
    }

    override fun loadPlayerSettings(): PlayerSettingsRecord {
        synchronized(lock) {
            return PlayerSettingsRecord(
                seekGestureEnabled = prefs.getBoolean(Keys.PLAYER_SEEK_GESTURE_ENABLED, true),
                brightnessGestureEnabled = prefs.getBoolean(Keys.PLAYER_BRIGHTNESS_GESTURE_ENABLED, true),
                volumeGestureEnabled = prefs.getBoolean(Keys.PLAYER_VOLUME_GESTURE_ENABLED, true),
                zoomGestureEnabled = prefs.getBoolean(Keys.PLAYER_ZOOM_GESTURE_ENABLED, true),
                panGestureEnabled = prefs.getBoolean(Keys.PLAYER_PAN_GESTURE_ENABLED, true),
                doubleTapGestureEnabled = prefs.getBoolean(Keys.PLAYER_DOUBLE_TAP_GESTURE_ENABLED, true),
                doubleTapAction = prefs.getString(Keys.PLAYER_DOUBLE_TAP_ACTION, "seek") ?: "seek",
                longPressGestureEnabled = prefs.getBoolean(Keys.PLAYER_LONG_PRESS_GESTURE_ENABLED, true),
                seekIncrementSec = prefs.getInt(Keys.PLAYER_SEEK_INCREMENT_SEC, 10).coerceIn(1, 60),
                seekSensitivity = prefs.getFloat(Keys.PLAYER_SEEK_SENSITIVITY, 1.0f).coerceIn(0.1f, 2.0f),
                longPressSpeed = prefs.getFloat(Keys.PLAYER_LONG_PRESS_SPEED, 2.0f).coerceIn(0.2f, 4.0f),
                controllerTimeoutSec = prefs.getInt(Keys.PLAYER_CONTROLLER_TIMEOUT_SEC, 3).coerceIn(1, 60),
                hideButtonsBackground = prefs.getBoolean(Keys.PLAYER_HIDE_BUTTONS_BACKGROUND, false),
                resumePlayback = prefs.getBoolean(Keys.PLAYER_RESUME_PLAYBACK, true),
                defaultPlaybackSpeed = prefs.getFloat(Keys.PLAYER_DEFAULT_PLAYBACK_SPEED, 1.0f).coerceIn(0.2f, 4.0f),
                autoplay = prefs.getBoolean(Keys.PLAYER_AUTOPLAY, true),
                autoPip = prefs.getBoolean(Keys.PLAYER_AUTO_PIP, true),
                autoBackgroundPlay = prefs.getBoolean(Keys.PLAYER_AUTO_BACKGROUND_PLAY, false),
                rememberBrightness = prefs.getBoolean(Keys.PLAYER_REMEMBER_BRIGHTNESS, false),
                rememberSelections = prefs.getBoolean(Keys.PLAYER_REMEMBER_SELECTIONS, true),
            )
        }
    }

    override fun savePlayerSettings(record: PlayerSettingsRecord) {
        synchronized(lock) {
            prefs.edit()
                .putBoolean(Keys.PLAYER_SEEK_GESTURE_ENABLED, record.seekGestureEnabled)
                .putBoolean(Keys.PLAYER_BRIGHTNESS_GESTURE_ENABLED, record.brightnessGestureEnabled)
                .putBoolean(Keys.PLAYER_VOLUME_GESTURE_ENABLED, record.volumeGestureEnabled)
                .putBoolean(Keys.PLAYER_ZOOM_GESTURE_ENABLED, record.zoomGestureEnabled)
                .putBoolean(Keys.PLAYER_PAN_GESTURE_ENABLED, record.panGestureEnabled)
                .putBoolean(Keys.PLAYER_DOUBLE_TAP_GESTURE_ENABLED, record.doubleTapGestureEnabled)
                .putString(Keys.PLAYER_DOUBLE_TAP_ACTION, record.doubleTapAction)
                .putBoolean(Keys.PLAYER_LONG_PRESS_GESTURE_ENABLED, record.longPressGestureEnabled)
                .putInt(Keys.PLAYER_SEEK_INCREMENT_SEC, record.seekIncrementSec.coerceIn(1, 60))
                .putFloat(Keys.PLAYER_SEEK_SENSITIVITY, record.seekSensitivity.coerceIn(0.1f, 2.0f))
                .putFloat(Keys.PLAYER_LONG_PRESS_SPEED, record.longPressSpeed.coerceIn(0.2f, 4.0f))
                .putInt(Keys.PLAYER_CONTROLLER_TIMEOUT_SEC, record.controllerTimeoutSec.coerceIn(1, 60))
                .putBoolean(Keys.PLAYER_HIDE_BUTTONS_BACKGROUND, record.hideButtonsBackground)
                .putBoolean(Keys.PLAYER_RESUME_PLAYBACK, record.resumePlayback)
                .putFloat(Keys.PLAYER_DEFAULT_PLAYBACK_SPEED, record.defaultPlaybackSpeed.coerceIn(0.2f, 4.0f))
                .putBoolean(Keys.PLAYER_AUTOPLAY, record.autoplay)
                .putBoolean(Keys.PLAYER_AUTO_PIP, record.autoPip)
                .putBoolean(Keys.PLAYER_AUTO_BACKGROUND_PLAY, record.autoBackgroundPlay)
                .putBoolean(Keys.PLAYER_REMEMBER_BRIGHTNESS, record.rememberBrightness)
                .putBoolean(Keys.PLAYER_REMEMBER_SELECTIONS, record.rememberSelections)
                .apply()
        }
    }

    override fun loadPlaybackBehavior(): PlaybackBehaviorRecord {
        synchronized(lock) {
            return PlaybackBehaviorRecord(
                keepConnectionInBackground = prefs.getBoolean(Keys.KEEP_CONNECTION_IN_BACKGROUND, true),
                rememberedBrightness = prefs
                    .takeIf { it.contains(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS) }
                    ?.getFloat(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS, 0f)
                    ?.coerceIn(0f, 1f),
            )
        }
    }

    override fun savePlaybackBehavior(record: PlaybackBehaviorRecord) {
        synchronized(lock) {
            prefs.edit()
                .putBoolean(Keys.KEEP_CONNECTION_IN_BACKGROUND, record.keepConnectionInBackground)
                .apply {
                    if (record.rememberedBrightness != null) {
                        putFloat(
                            Keys.PLAYBACK_REMEMBERED_BRIGHTNESS,
                            record.rememberedBrightness.coerceIn(0f, 1f),
                        )
                    } else {
                        remove(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS)
                    }
                }
                .apply()
        }
    }

    private fun loadCustomThemes(): List<CustomThemeRecord> {
        val raw = prefs.getString(Keys.THEME_CUSTOM_THEMES_JSON, null) ?: return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            buildList {
                for (idx in 0 until array.length()) {
                    val obj = array.optJSONObject(idx) ?: continue
                    // Records written before version tagging are treated as version 1.
                    // Records with an unrecognised version are skipped to avoid loading
                    // fields that may no longer exist in the current schema.
                    val version = obj.optInt("version", 1)
                    if (version != CUSTOM_THEME_JSON_VERSION) continue
                    val id = obj.optString("id")
                    val name = obj.optString("name")
                    val seedArgb = obj.optInt("seedArgb", Int.MIN_VALUE)
                    if (id.isBlank() || name.isBlank() || seedArgb == Int.MIN_VALUE) continue
                    add(
                        CustomThemeRecord(
                            id = id,
                            name = name,
                            seedArgb = seedArgb,
                            monochrome = obj.optBoolean("monochrome", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeCustomThemes(value: List<CustomThemeRecord>): String {
        val array = org.json.JSONArray()
        value.forEach { theme ->
            val obj = org.json.JSONObject()
            obj.put("version", CUSTOM_THEME_JSON_VERSION)
            obj.put("id", theme.id)
            obj.put("name", theme.name)
            obj.put("seedArgb", theme.seedArgb)
            obj.put("monochrome", theme.monochrome)
            array.put(obj)
        }
        return array.toString()
    }

    private companion object {
        object Keys {
            const val THEME_MODE = "theme_mode"
            const val THEME_APPEARANCE = "theme_appearance"
            const val THEME_CUSTOM_SEED = "theme_custom_seed"
            const val THEME_CUSTOM_THEME_ID = "theme_custom_theme_id"
            const val THEME_CUSTOM_MONO = "theme_custom_mono"
            const val THEME_PURE_BLACK = "theme_pure_black"
            const val THEME_FONT_SCALE = "theme_font_scale"
            const val THEME_FONT_SCALE_ENABLED = "theme_font_scale_enabled"
            const val THEME_CUSTOM_THEMES_JSON = "theme_custom_themes_json"
            const val NAV_DURATION_MS = "nav_duration_ms"
            const val HAPTIC_FEEDBACK_ENABLED = "haptic_feedback_enabled"

            const val PLAYER_SEEK_GESTURE_ENABLED = "player_seek_gesture_enabled"
            const val PLAYER_BRIGHTNESS_GESTURE_ENABLED = "player_brightness_gesture_enabled"
            const val PLAYER_VOLUME_GESTURE_ENABLED = "player_volume_gesture_enabled"
            const val PLAYER_ZOOM_GESTURE_ENABLED = "player_zoom_gesture_enabled"
            const val PLAYER_PAN_GESTURE_ENABLED = "player_pan_gesture_enabled"
            const val PLAYER_DOUBLE_TAP_GESTURE_ENABLED = "player_double_tap_gesture_enabled"
            const val PLAYER_DOUBLE_TAP_ACTION = "player_double_tap_action"
            const val PLAYER_LONG_PRESS_GESTURE_ENABLED = "player_long_press_gesture_enabled"
            const val PLAYER_SEEK_INCREMENT_SEC = "player_seek_increment_sec"
            const val PLAYER_SEEK_SENSITIVITY = "player_seek_sensitivity"
            const val PLAYER_LONG_PRESS_SPEED = "player_long_press_speed"
            const val PLAYER_CONTROLLER_TIMEOUT_SEC = "player_controller_timeout_sec"
            const val PLAYER_HIDE_BUTTONS_BACKGROUND = "player_hide_buttons_background"
            const val PLAYER_RESUME_PLAYBACK = "player_resume_playback"
            const val PLAYER_DEFAULT_PLAYBACK_SPEED = "player_default_playback_speed"
            const val PLAYER_AUTOPLAY = "player_autoplay"
            const val PLAYER_AUTO_PIP = "player_auto_pip"
            const val PLAYER_AUTO_BACKGROUND_PLAY = "player_auto_background_play"
            const val PLAYER_REMEMBER_BRIGHTNESS = "player_remember_brightness"
            const val PLAYER_REMEMBER_SELECTIONS = "player_remember_selections"

            const val KEEP_CONNECTION_IN_BACKGROUND = "keep_connection_in_background"
            const val PLAYBACK_REMEMBERED_BRIGHTNESS = "playback_remembered_brightness"
        }

        // Increment when adding/removing fields in encodeCustomThemes so that old code
        // skips records it cannot safely read, rather than silently producing corrupt data.
        const val CUSTOM_THEME_JSON_VERSION = 1
    }
}

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
                themeMode = prefs.getString("theme_mode", UiSettingsRecord().themeMode) ?: UiSettingsRecord().themeMode,
                themeAppearance = prefs.getString("theme_appearance", UiSettingsRecord().themeAppearance)
                    ?: UiSettingsRecord().themeAppearance,
                customSeedArgb = prefs.takeIf { it.contains("theme_custom_seed") }?.getInt("theme_custom_seed", 0),
                customThemeId = prefs.getString("theme_custom_theme_id", null),
                customMonochrome = prefs.getBoolean("theme_custom_mono", false),
                pureBlack = prefs.getBoolean("theme_pure_black", true),
                fontScale = prefs.getFloat("theme_font_scale", 1.0f).coerceIn(0.85f, 1.3f),
                fontScaleEnabled = prefs.getBoolean("theme_font_scale_enabled", false),
                customThemes = loadCustomThemes(),
                navDurationMs = prefs.getInt("nav_duration_ms", 350).coerceIn(0, 2000),
                hapticFeedbackEnabled = prefs.getBoolean("haptic_feedback_enabled", true),
            )
        }
    }

    override fun saveUiSettings(record: UiSettingsRecord) {
        synchronized(lock) {
            prefs.edit()
                .putString("theme_mode", record.themeMode)
                .putString("theme_appearance", record.themeAppearance)
                .putBoolean("theme_pure_black", record.pureBlack)
                .putFloat("theme_font_scale", record.fontScale.coerceIn(0.85f, 1.3f))
                .putBoolean("theme_font_scale_enabled", record.fontScaleEnabled)
                .putString("theme_custom_theme_id", record.customThemeId)
                .putBoolean("theme_custom_mono", record.customMonochrome)
                .putInt("nav_duration_ms", record.navDurationMs.coerceIn(0, 2000))
                .putBoolean("haptic_feedback_enabled", record.hapticFeedbackEnabled)
                .putString("theme_custom_themes_json", encodeCustomThemes(record.customThemes))
                .apply {
                    if (record.customSeedArgb != null) {
                        putInt("theme_custom_seed", record.customSeedArgb)
                    } else {
                        remove("theme_custom_seed")
                    }
                }
                .apply()
        }
    }

    override fun loadPlayerSettings(): PlayerSettingsRecord {
        synchronized(lock) {
            return PlayerSettingsRecord(
                seekGestureEnabled = prefs.getBoolean("player_seek_gesture_enabled", true),
                brightnessGestureEnabled = prefs.getBoolean("player_brightness_gesture_enabled", true),
                volumeGestureEnabled = prefs.getBoolean("player_volume_gesture_enabled", true),
                zoomGestureEnabled = prefs.getBoolean("player_zoom_gesture_enabled", true),
                panGestureEnabled = prefs.getBoolean("player_pan_gesture_enabled", true),
                doubleTapGestureEnabled = prefs.getBoolean("player_double_tap_gesture_enabled", true),
                doubleTapAction = prefs.getString("player_double_tap_action", "seek") ?: "seek",
                longPressGestureEnabled = prefs.getBoolean("player_long_press_gesture_enabled", true),
                seekIncrementSec = prefs.getInt("player_seek_increment_sec", 10).coerceIn(1, 60),
                seekSensitivity = prefs.getFloat("player_seek_sensitivity", 1.0f).coerceIn(0.1f, 2.0f),
                longPressSpeed = prefs.getFloat("player_long_press_speed", 2.0f).coerceIn(0.2f, 4.0f),
                controllerTimeoutSec = prefs.getInt("player_controller_timeout_sec", 3).coerceIn(1, 60),
                hideButtonsBackground = prefs.getBoolean("player_hide_buttons_background", false),
                resumePlayback = prefs.getBoolean("player_resume_playback", true),
                defaultPlaybackSpeed = prefs.getFloat("player_default_playback_speed", 1.0f).coerceIn(0.2f, 4.0f),
                autoplay = prefs.getBoolean("player_autoplay", true),
                autoPip = prefs.getBoolean("player_auto_pip", true),
                autoBackgroundPlay = prefs.getBoolean("player_auto_background_play", false),
                rememberBrightness = prefs.getBoolean("player_remember_brightness", false),
                rememberSelections = prefs.getBoolean("player_remember_selections", true),
            )
        }
    }

    override fun savePlayerSettings(record: PlayerSettingsRecord) {
        synchronized(lock) {
            prefs.edit()
                .putBoolean("player_seek_gesture_enabled", record.seekGestureEnabled)
                .putBoolean("player_brightness_gesture_enabled", record.brightnessGestureEnabled)
                .putBoolean("player_volume_gesture_enabled", record.volumeGestureEnabled)
                .putBoolean("player_zoom_gesture_enabled", record.zoomGestureEnabled)
                .putBoolean("player_pan_gesture_enabled", record.panGestureEnabled)
                .putBoolean("player_double_tap_gesture_enabled", record.doubleTapGestureEnabled)
                .putString("player_double_tap_action", record.doubleTapAction)
                .putBoolean("player_long_press_gesture_enabled", record.longPressGestureEnabled)
                .putInt("player_seek_increment_sec", record.seekIncrementSec.coerceIn(1, 60))
                .putFloat("player_seek_sensitivity", record.seekSensitivity.coerceIn(0.1f, 2.0f))
                .putFloat("player_long_press_speed", record.longPressSpeed.coerceIn(0.2f, 4.0f))
                .putInt("player_controller_timeout_sec", record.controllerTimeoutSec.coerceIn(1, 60))
                .putBoolean("player_hide_buttons_background", record.hideButtonsBackground)
                .putBoolean("player_resume_playback", record.resumePlayback)
                .putFloat("player_default_playback_speed", record.defaultPlaybackSpeed.coerceIn(0.2f, 4.0f))
                .putBoolean("player_autoplay", record.autoplay)
                .putBoolean("player_auto_pip", record.autoPip)
                .putBoolean("player_auto_background_play", record.autoBackgroundPlay)
                .putBoolean("player_remember_brightness", record.rememberBrightness)
                .putBoolean("player_remember_selections", record.rememberSelections)
                .apply()
        }
    }

    override fun loadPlaybackBehavior(): PlaybackBehaviorRecord {
        synchronized(lock) {
            return PlaybackBehaviorRecord(
                keepConnectionInBackground = prefs.getBoolean("keep_connection_in_background", true),
            )
        }
    }

    override fun savePlaybackBehavior(record: PlaybackBehaviorRecord) {
        synchronized(lock) {
            prefs.edit()
                .putBoolean("keep_connection_in_background", record.keepConnectionInBackground)
                .apply()
        }
    }

    private fun loadCustomThemes(): List<CustomThemeRecord> {
        val raw = prefs.getString("theme_custom_themes_json", null) ?: return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            buildList {
                for (idx in 0 until array.length()) {
                    val obj = array.optJSONObject(idx) ?: continue
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
            obj.put("id", theme.id)
            obj.put("name", theme.name)
            obj.put("seedArgb", theme.seedArgb)
            obj.put("monochrome", theme.monochrome)
            array.put(obj)
        }
        return array.toString()
    }
}

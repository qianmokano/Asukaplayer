package com.asuka.player.app

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

internal class AppSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var themeConfig: ThemeConfig
        get() {
            val mode = runCatching {
                ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.Monochrome.name) ?: ThemeMode.Monochrome.name)
            }.getOrDefault(ThemeMode.Monochrome)
            val appearance = runCatching {
                ThemeAppearanceMode.valueOf(
                    prefs.getString("theme_appearance", ThemeAppearanceMode.System.name) ?: ThemeAppearanceMode.System.name,
                )
            }.getOrDefault(ThemeAppearanceMode.System)
            val customSeedInt = prefs.getInt("theme_custom_seed", Int.MIN_VALUE)
            val customSeed = if (customSeedInt != Int.MIN_VALUE) Color(customSeedInt) else null
            return ThemeConfig(
                mode = mode,
                customSeed = customSeed,
                customThemeId = prefs.getString("theme_custom_theme_id", null),
                customMonochrome = prefs.getBoolean("theme_custom_mono", false),
                appearance = appearance,
                pureBlack = prefs.getBoolean("theme_pure_black", true),
                fontScale = prefs.getFloat("theme_font_scale", 1.0f).coerceIn(0.85f, 1.3f),
                fontScaleEnabled = prefs.getBoolean("theme_font_scale_enabled", false),
            )
        }
        set(value) {
            prefs.edit()
                .putString("theme_mode", value.mode.name)
                .putString("theme_appearance", value.appearance.name)
                .putBoolean("theme_pure_black", value.pureBlack)
                .putFloat("theme_font_scale", value.fontScale.coerceIn(0.85f, 1.3f))
                .putBoolean("theme_font_scale_enabled", value.fontScaleEnabled)
                .putString("theme_custom_theme_id", value.customThemeId)
                .putBoolean("theme_custom_mono", value.customMonochrome)
                .apply {
                    if (value.customSeed != null) {
                        putInt("theme_custom_seed", value.customSeed.toArgb())
                    } else {
                        remove("theme_custom_seed")
                    }
                }
                .apply()
        }

    var customThemes: List<CustomThemeEntry>
        get() {
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
                            CustomThemeEntry(
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
        set(value) {
            val array = org.json.JSONArray()
            value.forEach { theme ->
                val obj = org.json.JSONObject()
                obj.put("id", theme.id)
                obj.put("name", theme.name)
                obj.put("seedArgb", theme.seedArgb)
                obj.put("monochrome", theme.monochrome)
                array.put(obj)
            }
            prefs.edit().putString("theme_custom_themes_json", array.toString()).apply()
        }

    var navDurationMs: Int
        get() = prefs.getInt("nav_duration_ms", 350).coerceIn(0, 2000)
        set(value) {
            prefs.edit().putInt("nav_duration_ms", value.coerceIn(0, 2000)).apply()
        }

    var keepConnectionInBackground: Boolean
        get() = prefs.getBoolean("keep_connection_in_background", true)
        set(value) {
            prefs.edit().putBoolean("keep_connection_in_background", value).apply()
        }

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback_enabled", true)
        set(value) {
            prefs.edit().putBoolean("haptic_feedback_enabled", value).apply()
        }

    var experimentalFeaturesEnabled: Boolean
        get() = prefs.getBoolean("experimental_features_enabled", false)
        set(value) {
            prefs.edit().putBoolean("experimental_features_enabled", value).apply()
        }

    var playerSettings: PlayerSettingsConfig
        get() = PlayerSettingsConfig(
            seekGestureEnabled = prefs.getBoolean("player_seek_gesture_enabled", true),
            brightnessGestureEnabled = prefs.getBoolean("player_brightness_gesture_enabled", true),
            volumeGestureEnabled = prefs.getBoolean("player_volume_gesture_enabled", true),
            zoomGestureEnabled = prefs.getBoolean("player_zoom_gesture_enabled", true),
            panGestureEnabled = prefs.getBoolean("player_pan_gesture_enabled", true),
            doubleTapGestureEnabled = prefs.getBoolean("player_double_tap_gesture_enabled", true),
            doubleTapAction = DoubleTapActionSetting.fromValue(prefs.getString("player_double_tap_action", "seek")),
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
        set(value) {
            prefs.edit()
                .putBoolean("player_seek_gesture_enabled", value.seekGestureEnabled)
                .putBoolean("player_brightness_gesture_enabled", value.brightnessGestureEnabled)
                .putBoolean("player_volume_gesture_enabled", value.volumeGestureEnabled)
                .putBoolean("player_zoom_gesture_enabled", value.zoomGestureEnabled)
                .putBoolean("player_pan_gesture_enabled", value.panGestureEnabled)
                .putBoolean("player_double_tap_gesture_enabled", value.doubleTapGestureEnabled)
                .putString("player_double_tap_action", value.doubleTapAction.value)
                .putBoolean("player_long_press_gesture_enabled", value.longPressGestureEnabled)
                .putInt("player_seek_increment_sec", value.seekIncrementSec.coerceIn(1, 60))
                .putFloat("player_seek_sensitivity", value.seekSensitivity.coerceIn(0.1f, 2.0f))
                .putFloat("player_long_press_speed", value.longPressSpeed.coerceIn(0.2f, 4.0f))
                .putInt("player_controller_timeout_sec", value.controllerTimeoutSec.coerceIn(1, 60))
                .putBoolean("player_hide_buttons_background", value.hideButtonsBackground)
                .putBoolean("player_resume_playback", value.resumePlayback)
                .putFloat("player_default_playback_speed", value.defaultPlaybackSpeed.coerceIn(0.2f, 4.0f))
                .putBoolean("player_autoplay", value.autoplay)
                .putBoolean("player_auto_pip", value.autoPip)
                .putBoolean("player_auto_background_play", value.autoBackgroundPlay)
                .putBoolean("player_remember_brightness", value.rememberBrightness)
                .putBoolean("player_remember_selections", value.rememberSelections)
                .apply()
        }
}

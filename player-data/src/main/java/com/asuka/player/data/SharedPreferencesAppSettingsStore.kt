package com.asuka.player.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SharedPreferencesAppSettingsStore(context: Context) : AppSettingsStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val _snapshots = MutableStateFlow(readSnapshot())

    override val snapshots: StateFlow<AppSettingsSnapshot> = _snapshots.asStateFlow()

    private fun readSnapshot(): AppSettingsSnapshot {
        synchronized(lock) {
            return AppSettingsSnapshot(
                uiSettings = UiSettingsRecord(
                    themeMode = prefs.getString(Keys.THEME_MODE, UiSettingsRecord().themeMode) ?: UiSettingsRecord().themeMode,
                    themeAppearance = prefs.getString(Keys.THEME_APPEARANCE, UiSettingsRecord().themeAppearance)
                        ?: UiSettingsRecord().themeAppearance,
                    customSeedArgb = prefs.takeIf { it.contains(Keys.THEME_CUSTOM_SEED) }?.getInt(Keys.THEME_CUSTOM_SEED, 0),
                    customThemeId = prefs.getString(Keys.THEME_CUSTOM_THEME_ID, null),
                    customMonochrome = prefs.getBoolean(Keys.THEME_CUSTOM_MONO, false),
                    pureBlack = prefs.getBoolean(Keys.THEME_PURE_BLACK, true),
                    fontScale = prefs.getFloat(Keys.THEME_FONT_SCALE, 1.0f),
                    fontScaleEnabled = prefs.getBoolean(Keys.THEME_FONT_SCALE_ENABLED, false),
                    customThemes = loadCustomThemes(),
                    navDurationMs = prefs.getInt(Keys.NAV_DURATION_MS, 300),
                    hapticFeedbackEnabled = prefs.getBoolean(Keys.HAPTIC_FEEDBACK_ENABLED, true),
                ),
                playerSettings = PlayerSettingsRecord(
                    seekGestureEnabled = prefs.getBoolean(Keys.PLAYER_SEEK_GESTURE_ENABLED, true),
                    brightnessGestureEnabled = prefs.getBoolean(Keys.PLAYER_BRIGHTNESS_GESTURE_ENABLED, true),
                    volumeGestureEnabled = prefs.getBoolean(Keys.PLAYER_VOLUME_GESTURE_ENABLED, true),
                    zoomGestureEnabled = prefs.getBoolean(Keys.PLAYER_ZOOM_GESTURE_ENABLED, true),
                    panGestureEnabled = prefs.getBoolean(Keys.PLAYER_PAN_GESTURE_ENABLED, true),
                    doubleTapGestureEnabled = prefs.getBoolean(Keys.PLAYER_DOUBLE_TAP_GESTURE_ENABLED, true),
                    doubleTapAction = prefs.getString(Keys.PLAYER_DOUBLE_TAP_ACTION, "toggle_play_pause") ?: "toggle_play_pause",
                    longPressGestureEnabled = prefs.getBoolean(Keys.PLAYER_LONG_PRESS_GESTURE_ENABLED, true),
                    seekIncrementSec = prefs.getInt(Keys.PLAYER_SEEK_INCREMENT_SEC, 10),
                    seekSensitivity = prefs.getFloat(Keys.PLAYER_SEEK_SENSITIVITY, 1.0f),
                    longPressSpeed = prefs.getFloat(Keys.PLAYER_LONG_PRESS_SPEED, 2.0f),
                    controllerTimeoutSec = prefs.getInt(Keys.PLAYER_CONTROLLER_TIMEOUT_SEC, 3),
                    hideButtonsBackground = prefs.getBoolean(Keys.PLAYER_HIDE_BUTTONS_BACKGROUND, false),
                    resumePlayback = prefs.getBoolean(Keys.PLAYER_RESUME_PLAYBACK, true),
                    defaultPlaybackSpeed = prefs.getFloat(Keys.PLAYER_DEFAULT_PLAYBACK_SPEED, 1.0f),
                    autoplay = prefs.getBoolean(Keys.PLAYER_AUTOPLAY, true),
                    autoPip = prefs.getBoolean(Keys.PLAYER_AUTO_PIP, true),
                    autoBackgroundPlay = prefs.getBoolean(Keys.PLAYER_AUTO_BACKGROUND_PLAY, false),
                    rememberBrightness = prefs.getBoolean(Keys.PLAYER_REMEMBER_BRIGHTNESS, false),
                    rememberSelections = prefs.getBoolean(Keys.PLAYER_REMEMBER_SELECTIONS, true),
                ),
                playbackBehavior = PlaybackBehaviorRecord(
                    keepConnectionInBackground = prefs.getBoolean(Keys.KEEP_CONNECTION_IN_BACKGROUND, true),
                    rememberedBrightness = prefs
                        .takeIf { it.contains(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS) }
                        ?.getFloat(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS, 0f),
                ),
            ).normalized()
        }
    }

    override suspend fun saveSnapshot(snapshot: AppSettingsSnapshot) {
        val normalized = snapshot.normalized()
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                prefs.edit()
                    .putString(Keys.THEME_MODE, normalized.uiSettings.themeMode)
                    .putString(Keys.THEME_APPEARANCE, normalized.uiSettings.themeAppearance)
                    .putBoolean(Keys.THEME_PURE_BLACK, normalized.uiSettings.pureBlack)
                    .putFloat(Keys.THEME_FONT_SCALE, normalized.uiSettings.fontScale)
                    .putBoolean(Keys.THEME_FONT_SCALE_ENABLED, normalized.uiSettings.fontScaleEnabled)
                    .putString(Keys.THEME_CUSTOM_THEME_ID, normalized.uiSettings.customThemeId)
                    .putBoolean(Keys.THEME_CUSTOM_MONO, normalized.uiSettings.customMonochrome)
                    .putInt(Keys.NAV_DURATION_MS, normalized.uiSettings.navDurationMs)
                    .putBoolean(Keys.HAPTIC_FEEDBACK_ENABLED, normalized.uiSettings.hapticFeedbackEnabled)
                    .putString(Keys.THEME_CUSTOM_THEMES_JSON, encodeCustomThemes(normalized.uiSettings.customThemes))
                    .putBoolean(Keys.PLAYER_SEEK_GESTURE_ENABLED, normalized.playerSettings.seekGestureEnabled)
                    .putBoolean(Keys.PLAYER_BRIGHTNESS_GESTURE_ENABLED, normalized.playerSettings.brightnessGestureEnabled)
                    .putBoolean(Keys.PLAYER_VOLUME_GESTURE_ENABLED, normalized.playerSettings.volumeGestureEnabled)
                    .putBoolean(Keys.PLAYER_ZOOM_GESTURE_ENABLED, normalized.playerSettings.zoomGestureEnabled)
                    .putBoolean(Keys.PLAYER_PAN_GESTURE_ENABLED, normalized.playerSettings.panGestureEnabled)
                    .putBoolean(Keys.PLAYER_DOUBLE_TAP_GESTURE_ENABLED, normalized.playerSettings.doubleTapGestureEnabled)
                    .putString(Keys.PLAYER_DOUBLE_TAP_ACTION, normalized.playerSettings.doubleTapAction)
                    .putBoolean(Keys.PLAYER_LONG_PRESS_GESTURE_ENABLED, normalized.playerSettings.longPressGestureEnabled)
                    .putInt(Keys.PLAYER_SEEK_INCREMENT_SEC, normalized.playerSettings.seekIncrementSec)
                    .putFloat(Keys.PLAYER_SEEK_SENSITIVITY, normalized.playerSettings.seekSensitivity)
                    .putFloat(Keys.PLAYER_LONG_PRESS_SPEED, normalized.playerSettings.longPressSpeed)
                    .putInt(Keys.PLAYER_CONTROLLER_TIMEOUT_SEC, normalized.playerSettings.controllerTimeoutSec)
                    .putBoolean(Keys.PLAYER_HIDE_BUTTONS_BACKGROUND, normalized.playerSettings.hideButtonsBackground)
                    .putBoolean(Keys.PLAYER_RESUME_PLAYBACK, normalized.playerSettings.resumePlayback)
                    .putFloat(Keys.PLAYER_DEFAULT_PLAYBACK_SPEED, normalized.playerSettings.defaultPlaybackSpeed)
                    .putBoolean(Keys.PLAYER_AUTOPLAY, normalized.playerSettings.autoplay)
                    .putBoolean(Keys.PLAYER_AUTO_PIP, normalized.playerSettings.autoPip)
                    .putBoolean(Keys.PLAYER_AUTO_BACKGROUND_PLAY, normalized.playerSettings.autoBackgroundPlay)
                    .putBoolean(Keys.PLAYER_REMEMBER_BRIGHTNESS, normalized.playerSettings.rememberBrightness)
                    .putBoolean(Keys.PLAYER_REMEMBER_SELECTIONS, normalized.playerSettings.rememberSelections)
                    .putBoolean(Keys.KEEP_CONNECTION_IN_BACKGROUND, normalized.playbackBehavior.keepConnectionInBackground)
                    .apply {
                        if (normalized.uiSettings.customSeedArgb != null) {
                            putInt(Keys.THEME_CUSTOM_SEED, normalized.uiSettings.customSeedArgb)
                        } else {
                            remove(Keys.THEME_CUSTOM_SEED)
                        }
                        if (normalized.playbackBehavior.rememberedBrightness != null) {
                            putFloat(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS, normalized.playbackBehavior.rememberedBrightness)
                        } else {
                            remove(Keys.PLAYBACK_REMEMBERED_BRIGHTNESS)
                        }
                    }
                    .apply()
            }
        }
        _snapshots.value = normalized
    }

    fun hasLegacyData(): Boolean {
        synchronized(lock) {
            return prefs.all.isNotEmpty()
        }
    }

    private fun loadCustomThemes(): List<CustomThemeRecord> {
        val raw = prefs.getString(Keys.THEME_CUSTOM_THEMES_JSON, null) ?: return emptyList()
        return runCatching {
            val array = org.json.JSONArray(raw)
            buildList {
                for (idx in 0 until array.length()) {
                    val obj = array.optJSONObject(idx) ?: continue
                    val version = obj.optInt("version", 1)
                    if (version != CUSTOM_THEME_JSON_VERSION) continue
                    val id = obj.optString("id")
                    val name = obj.optString("name")
                    if (id.isBlank() || name.isBlank() || !obj.has("seedArgb")) continue
                    add(
                        CustomThemeRecord(
                            id = id,
                            name = name,
                            seedArgb = obj.optInt("seedArgb"),
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

    private object Keys {
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

    companion object {
        const val PREFS_NAME = "app_settings"
        const val CUSTOM_THEME_JSON_VERSION = 1
    }
}

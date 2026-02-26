package com.asuka.player.app

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatDelegate
import android.util.LruCache
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.AutoAwesomeMotion
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.VideoSettings
import androidx.compose.material.icons.outlined.ZoomOutMap
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.DoubleArrow
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.HideSource
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PanToolAlt
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Pinch
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.ResetTv
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.AutoAwesomeMotion
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavBackStackEntry
import com.asuka.player.R
import com.asuka.player.core.SeekFallbackCopier
import com.asuka.player.ui.activity.PlaybackActivity
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var launchedForDirectPlayback = false
    private val seekFallbackCopier by lazy { SeekFallbackCopier(contentResolver, cacheDir) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        val incomingData = intent?.data
        if (incomingData != null) {
            launchedForDirectPlayback = true
            requestPlayback(incomingData.toString(), AppSettingsStore(this).playerSettings)
            return
        }

        setContent {
            MainLibraryScreen(
                onPlay = { mediaId, playerSettings -> requestPlayback(mediaId, playerSettings) },
            )
        }
    }

    private fun requestPlayback(mediaId: String, playerSettings: PlayerSettingsConfig) {
        lifecycleScope.launch {
            val resolvedUri = withContext(Dispatchers.IO) {
                resolveUriForPlayback(Uri.parse(mediaId))
            }
            startPlayback(resolvedUri, playerSettings)
        }
    }

    private fun startPlayback(mediaUri: Uri, playerSettings: PlayerSettingsConfig) {
        val playbackIntent = Intent(this, PlaybackActivity::class.java).apply {
            data = mediaUri
            if (mediaUri.scheme == "content") {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            putExtra(PlaybackActivity.EXTRA_SEEK_GESTURE_ENABLED, playerSettings.seekGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_BRIGHTNESS_GESTURE_ENABLED, playerSettings.brightnessGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_VOLUME_GESTURE_ENABLED, playerSettings.volumeGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_ZOOM_GESTURE_ENABLED, playerSettings.zoomGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_PAN_GESTURE_ENABLED, playerSettings.panGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_DOUBLE_TAP_GESTURE_ENABLED, playerSettings.doubleTapGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_DOUBLE_TAP_ACTION, playerSettings.doubleTapAction)
            putExtra(PlaybackActivity.EXTRA_LONG_PRESS_GESTURE_ENABLED, playerSettings.longPressGestureEnabled)
            putExtra(PlaybackActivity.EXTRA_SEEK_INCREMENT_SEC, playerSettings.seekIncrementSec)
            putExtra(PlaybackActivity.EXTRA_SEEK_SENSITIVITY, playerSettings.seekSensitivity)
            putExtra(PlaybackActivity.EXTRA_LONG_PRESS_SPEED, playerSettings.longPressSpeed)
            putExtra(PlaybackActivity.EXTRA_CONTROLLER_TIMEOUT_SEC, playerSettings.controllerTimeoutSec)
            putExtra(PlaybackActivity.EXTRA_HIDE_BUTTON_BG, playerSettings.hideButtonsBackground)
            putExtra(PlaybackActivity.EXTRA_RESUME_PLAYBACK, playerSettings.resumePlayback)
            putExtra(PlaybackActivity.EXTRA_DEFAULT_SPEED, playerSettings.defaultPlaybackSpeed)
            putExtra(PlaybackActivity.EXTRA_AUTOPLAY, playerSettings.autoplay)
            putExtra(PlaybackActivity.EXTRA_AUTO_PIP, playerSettings.autoPip)
            putExtra(PlaybackActivity.EXTRA_AUTO_BACKGROUND_PLAY, playerSettings.autoBackgroundPlay)
            putExtra(PlaybackActivity.EXTRA_REMEMBER_BRIGHTNESS, playerSettings.rememberBrightness)
            putExtra(PlaybackActivity.EXTRA_REMEMBER_SELECTIONS, playerSettings.rememberSelections)
        }
        startActivity(playbackIntent)
        if (launchedForDirectPlayback) {
            finish()
        }
    }

    private fun resolveUriForPlayback(sourceUri: Uri): Uri {
        if (sourceUri.scheme != "content") return sourceUri
        if (sourceUri.authority == MediaStore.AUTHORITY) return sourceUri
        if (isContentUriSeekable(sourceUri)) return sourceUri
        val copiedUri = seekFallbackCopier.copy(sourceUri)
        return copiedUri ?: sourceUri
    }

    private fun isContentUriSeekable(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                try {
                    Os.lseek(pfd.fileDescriptor, 0L, OsConstants.SEEK_CUR)
                    true
                } catch (_: ErrnoException) {
                    false
                }
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }
}

private enum class ThemeMode {
    Dynamic,
    Custom,
    Violet,
    Teal,
    Red,
    Sandstone,
    Neutral,
    Monochrome,
}

private enum class ThemeAppearanceMode {
    System,
    Light,
    Dark,
}

private data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.Monochrome,
    val customSeed: Color? = null,
    val customThemeId: String? = null,
    val customMonochrome: Boolean = false,
    val appearance: ThemeAppearanceMode = ThemeAppearanceMode.System,
    val pureBlack: Boolean = true,
    val fontScale: Float = 1.0f,
    val fontScaleEnabled: Boolean = false,
)

private data class PlayerSettingsConfig(
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

private data class CustomThemeEntry(
    val id: String,
    val name: String,
    val seedArgb: Int,
    val monochrome: Boolean,
) {
    val seed: Color
        get() = Color(seedArgb)
}

private data class ThemePreset(
    val mode: ThemeMode,
    val name: String,
    val description: String,
    val seed: Color?,
)

private val ThemePresets = listOf(
    ThemePreset(ThemeMode.Dynamic, "动态", "跟随系统壁纸/主题", null),
    ThemePreset(ThemeMode.Violet, "紫罗兰", "Material Baseline Violet", Color(0xFF6750A4)),
    ThemePreset(ThemeMode.Teal, "青绿", "Material Baseline Teal", Color(0xFF006E6A)),
    ThemePreset(ThemeMode.Red, "红宝石", "Material Baseline Red", Color(0xFFB3261E)),
    ThemePreset(ThemeMode.Sandstone, "砂岩", "Material Baseline Sand", Color(0xFF8B6B4A)),
    ThemePreset(ThemeMode.Neutral, "石板灰", "Material Baseline Neutral", Color(0xFF6B7280)),
    ThemePreset(ThemeMode.Monochrome, "黑白", "Monochrome", null),
    ThemePreset(ThemeMode.Custom, "自定义", "输入或选择任意颜色", null),
)

private class AppSettingsStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)

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
            doubleTapAction = when (prefs.getString("player_double_tap_action", "seek")) {
                "toggle_play_pause" -> "toggle_play_pause"
                "both" -> "both"
                else -> "seek"
            },
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
                .putString("player_double_tap_action", value.doubleTapAction)
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

private val LocalHapticsEnabled = staticCompositionLocalOf { true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainLibraryScreen(onPlay: (String, PlayerSettingsConfig) -> Unit) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    val appVersion = remember(context) { readAppVersion(context) }
    val appSettingsStore = remember(context) { AppSettingsStore(context) }
    var themeConfig by remember { mutableStateOf(appSettingsStore.themeConfig) }
    var customThemes by remember { mutableStateOf(appSettingsStore.customThemes) }
    var navDurationMs by rememberSaveable { mutableIntStateOf(appSettingsStore.navDurationMs) }
    var keepConnectionInBackground by rememberSaveable { mutableStateOf(appSettingsStore.keepConnectionInBackground) }
    var hapticFeedbackEnabled by rememberSaveable { mutableStateOf(appSettingsStore.hapticFeedbackEnabled) }
    var experimentalFeaturesEnabled by rememberSaveable { mutableStateOf(appSettingsStore.experimentalFeaturesEnabled) }
    var playerSettings by remember { mutableStateOf(appSettingsStore.playerSettings) }
    var permissionGranted by remember { mutableStateOf(hasVideoPermission(context)) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var hasLoadedOnce by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(emptyList<LocalVideoItem>()) }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
    val currentFolderId = backStackEntry?.arguments?.getLong(ARG_FOLDER_ID)

    LaunchedEffect(themeConfig.appearance) {
        val mode = when (themeConfig.appearance) {
            ThemeAppearanceMode.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeAppearanceMode.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeAppearanceMode.Dark -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    LaunchedEffect(themeConfig) {
        appSettingsStore.themeConfig = themeConfig
    }

    LaunchedEffect(customThemes) {
        appSettingsStore.customThemes = customThemes
    }

    LaunchedEffect(navDurationMs) {
        appSettingsStore.navDurationMs = navDurationMs
    }

    LaunchedEffect(keepConnectionInBackground) {
        appSettingsStore.keepConnectionInBackground = keepConnectionInBackground
    }

    LaunchedEffect(hapticFeedbackEnabled) {
        appSettingsStore.hapticFeedbackEnabled = hapticFeedbackEnabled
    }

    LaunchedEffect(experimentalFeaturesEnabled) {
        appSettingsStore.experimentalFeaturesEnabled = experimentalFeaturesEnabled
    }
    LaunchedEffect(playerSettings) {
        appSettingsStore.playerSettings = playerSettings
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        permissionGranted = result.values.any { it } || hasVideoPermission(context)
    }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onPlay(uri.toString(), playerSettings)
    }

    LaunchedEffect(permissionGranted, refreshTick) {
        if (!permissionGranted) return@LaunchedEffect
        val startedAtMs = System.currentTimeMillis()
        val isUserRefresh = hasLoadedOnce
        loading = true
        val latestItems = withContext(Dispatchers.IO) {
            queryLocalVideos(context)
        }
        if (isUserRefresh) {
            val elapsed = System.currentTimeMillis() - startedAtMs
            val minRefreshAnimMs = 500L
            if (elapsed < minRefreshAnimMs) {
                delay(minRefreshAnimMs - elapsed)
            }
        }
        items = latestItems
        if (!isUserRefresh) {
            uiScope.launch(Dispatchers.IO) {
                warmupInitialThumbnails(
                    context = context,
                    videos = latestItems,
                    limit = INITIAL_THUMB_WARMUP_LIMIT,
                )
            }
        }
        loading = false
        hasLoadedOnce = true
        if (isUserRefresh) {
            Toast.makeText(
                context,
                context.getString(R.string.refresh_done, latestItems.size),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val folders = remember(items) { buildFolderGroups(items) }
    val selectedFolderExists = remember(folders, currentFolderId) {
        currentFolderId?.let { id -> folders.any { it.id == id } } ?: false
    }

    LaunchedEffect(currentRoute, currentFolderId, folders) {
        if (currentRoute.startsWith("folder/") && currentFolderId != null && !selectedFolderExists) {
            navController.popBackStack(route = ROUTE_HOME, inclusive = false)
        }
    }

    AsukaTheme(themeConfig = themeConfig) {
        CompositionLocalProvider(LocalHapticsEnabled provides hapticFeedbackEnabled) {
            NavHost(
                navController = navController,
                startDestination = ROUTE_HOME,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    pageEnterTransition(navDurationMs)
                },
                exitTransition = {
                    pageExitTransition(navDurationMs)
                },
                popEnterTransition = {
                    pageEnterTransition(navDurationMs)
                },
                popExitTransition = {
                    pageExitTransition(navDurationMs)
                },
            ) {
            composable(route = ROUTE_HOME) {
                LibraryPageScaffold(
                    title = stringResource(id = R.string.launcher_title),
                    showBack = false,
                    showSettingsAction = true,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    HomePageContent(
                        modifier = Modifier.padding(innerPadding),
                        permissionGranted = permissionGranted,
                        initialLoading = loading && !hasLoadedOnce,
                        isRefreshing = loading && hasLoadedOnce,
                        folders = folders,
                        onRequestPermission = { permissionLauncher.launch(videoPermissionsForRuntime()) },
                        onOpenLocalVideo = { picker.launch(arrayOf("video/*")) },
                        onRefresh = {
                            if (!loading) refreshTick++
                        },
                        onOpenAllVideos = {
                            navController.navigate(ROUTE_ALL_VIDEOS) {
                                launchSingleTop = true
                            }
                        },
                        onOpenFolder = { folderId ->
                            val targetFolder = folders.firstOrNull { it.id == folderId }
                            uiScope.launch(Dispatchers.IO) {
                                prefetchFolderThumbnails(context, targetFolder, limit = 12)
                            }
                            navController.navigate(folderRoute(folderId)) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
            }

            composable(route = ROUTE_ALL_VIDEOS) {
                LibraryPageScaffold(
                    title = stringResource(id = R.string.tab_all_videos),
                    showBack = true,
                    showSettingsAction = true,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    VideosPageContent(
                        modifier = Modifier.padding(innerPadding),
                        initialLoading = loading && !hasLoadedOnce,
                        isRefreshing = loading && hasLoadedOnce,
                        videos = items,
                        onPlay = { mediaId -> onPlay(mediaId, playerSettings) },
                        onRefresh = {
                            if (!loading) refreshTick++
                        },
                    )
                }
            }

            composable(
                route = ROUTE_FOLDER,
                arguments = listOf(
                    navArgument(ARG_FOLDER_ID) { type = NavType.LongType },
                ),
            ) { entry ->
                val folderId = entry.arguments?.getLong(ARG_FOLDER_ID)
                val folder = folders.firstOrNull { it.id == folderId }
                LibraryPageScaffold(
                    title = folder?.name ?: stringResource(id = R.string.launcher_title),
                    showBack = true,
                    showSettingsAction = true,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    FolderPageContent(
                        modifier = Modifier.padding(innerPadding),
                        initialLoading = loading && !hasLoadedOnce,
                        isRefreshing = loading && hasLoadedOnce,
                        folder = folder,
                        onPlay = { mediaId -> onPlay(mediaId, playerSettings) },
                        onRefresh = {
                            if (!loading) refreshTick++
                        },
                    )
                }
            }

            composable(route = ROUTE_SETTINGS) {
                LibraryPageScaffold(
                    title = stringResource(id = R.string.settings_title),
                    showBack = true,
                    showSettingsAction = false,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    SettingsPageContent(
                        modifier = Modifier.padding(innerPadding),
                        appVersion = appVersion,
                        hapticFeedbackEnabled = hapticFeedbackEnabled,
                        onHapticFeedbackEnabledChange = { hapticFeedbackEnabled = it },
                        onOpenPlayer = { navController.navigate(ROUTE_SETTINGS_PLAYER) },
                        onOpenTheme = { navController.navigate(ROUTE_SETTINGS_THEME) },
                        onOpenMotion = { navController.navigate(ROUTE_SETTINGS_MOTION) },
                    )
                }
            }

            composable(route = ROUTE_SETTINGS_PLAYER) {
                LibraryPageScaffold(
                    title = "播放器",
                    showBack = true,
                    showSettingsAction = false,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    PlayerSettingsPlaceholderPageContent(
                        modifier = Modifier.padding(innerPadding),
                        playerSettings = playerSettings,
                        onPlayerSettingsChange = { playerSettings = it },
                    )
                }
            }

            composable(route = ROUTE_SETTINGS_THEME) {
                LibraryPageScaffold(
                    title = "主题",
                    showBack = true,
                    showSettingsAction = false,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    ThemeSettingsPageContent(
                        modifier = Modifier.padding(innerPadding),
                        themeConfig = themeConfig,
                        customThemes = customThemes,
                        hapticsEnabled = hapticFeedbackEnabled,
                        onThemeConfigChange = { themeConfig = it },
                        onCustomThemesChange = { customThemes = it },
                    )
                }
            }

            composable(route = ROUTE_SETTINGS_MOTION) {
                LibraryPageScaffold(
                    title = "动画",
                    showBack = true,
                    showSettingsAction = false,
                    onBack = { navController.navigateUp() },
                    onOpenSettings = {
                        navController.navigate(ROUTE_SETTINGS) {
                            launchSingleTop = true
                        }
                    },
                ) { innerPadding ->
                    MotionSettingsPageContent(
                        modifier = Modifier.padding(innerPadding),
                        navDurationMs = navDurationMs,
                        onNavDurationChange = { navDurationMs = it },
                    )
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryPageScaffold(
    title: String,
    showBack: Boolean,
    showSettingsAction: Boolean,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
            ) {
                TopAppBar(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        if (showBack) {
                            Row {
                                IconButton(
                                    modifier = Modifier.size(36.dp),
                                    onClick = onBack,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(id = R.string.back_to_folders),
                                    )
                                }
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    actions = {
                        if (showSettingsAction) {
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = {
                                    if (hapticsEnabled) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                                    }
                                    onOpenSettings()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(id = R.string.settings_title),
                                )
                            }
                        }
                    },
                    windowInsets = TopAppBarDefaults.windowInsets,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
        content = content,
    )
}

@Composable
private fun HomePageContent(
    modifier: Modifier = Modifier,
    permissionGranted: Boolean,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    folders: List<LocalVideoFolder>,
    onRequestPermission: () -> Unit,
    onOpenLocalVideo: () -> Unit,
    onRefresh: () -> Unit,
    onOpenAllVideos: () -> Unit,
    onOpenFolder: (Long) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            if (!permissionGranted) {
                item {
                    SplicedColumnGroup(title = stringResource(id = R.string.permission_required_title)) {
                        item {
                            SettingsNavigationItem(
                                icon = Icons.Rounded.Lock,
                                title = stringResource(id = R.string.grant_permission),
                                description = stringResource(id = R.string.permission_hint),
                                onClick = onRequestPermission,
                            )
                        }
                        item {
                            SettingsNavigationItem(
                                icon = Icons.Rounded.PlayCircle,
                                title = stringResource(id = R.string.open_local_video),
                                description = stringResource(id = R.string.open_video_without_permission_hint),
                                onClick = onOpenLocalVideo,
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            item {
                SplicedColumnGroup(title = stringResource(id = R.string.library_actions_title)) {
                    item {
                        SettingsNavigationItem(
                            icon = Icons.Rounded.PlayCircle,
                            title = stringResource(id = R.string.open_local_video),
                            description = stringResource(id = R.string.open_local_video_hint),
                            onClick = onOpenLocalVideo,
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.size(4.dp))
            }

            if (initialLoading) {
                item { LoadingBlock() }
            } else if (folders.isEmpty()) {
                item { EmptyBlock(text = stringResource(id = R.string.empty_video_list)) }
            } else {
                item {
                    SectionTitle(text = stringResource(id = R.string.folders_group_title, folders.size))
                }
                itemsIndexed(
                    items = folders,
                    key = { _, folder -> folder.id },
                ) { index, folder ->
                    GroupedListRow(
                        index = index,
                        totalCount = folders.size,
                    ) {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.FolderOpen,
                            title = folder.name,
                            description = stringResource(
                                id = R.string.folder_meta_no_size,
                                folder.videoCount,
                                folder.totalDurationLabel,
                            ),
                            onClick = { onOpenFolder(folder.id) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

@Composable
private fun VideosPageContent(
    modifier: Modifier = Modifier,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    videos: List<LocalVideoItem>,
    onPlay: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            if (initialLoading) {
                item { LoadingBlock() }
            } else if (videos.isEmpty()) {
                item { EmptyBlock(text = stringResource(id = R.string.empty_video_list)) }
            } else {
                item {
                    SectionTitle(text = stringResource(id = R.string.videos_group_title, videos.size))
                }
                itemsIndexed(
                    items = videos,
                    key = { _, item -> item.id },
                ) { index, item ->
                    GroupedListRow(
                        index = index,
                        totalCount = videos.size,
                        horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                    ) {
                        SettingsNavigationItem(
                            icon = Icons.Outlined.VideoLibrary,
                            thumbnailUri = item.uri,
                            thumbnailId = item.id,
                            durationLabel = item.durationLabel,
                            title = item.title,
                            description = item.folderPath,
                            onClick = { onPlay(item.uri.toString()) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

@Composable
private fun FolderPageContent(
    modifier: Modifier = Modifier,
    initialLoading: Boolean,
    isRefreshing: Boolean,
    folder: LocalVideoFolder?,
    onPlay: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullToRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = pullToRefreshState,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            item { Spacer(modifier = Modifier.size(12.dp)) }

            if (initialLoading) {
                item { LoadingBlock() }
            } else {
                val videos = folder?.videos.orEmpty()
                if (videos.isEmpty()) {
                    item { EmptyBlock(text = stringResource(id = R.string.empty_video_list)) }
                } else {
                    item {
                        SectionTitle(text = stringResource(id = R.string.selected_folder_group_title, videos.size))
                    }
                    itemsIndexed(
                        items = videos,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        GroupedListRow(
                            index = index,
                            totalCount = videos.size,
                            horizontalPadding = VIDEO_GROUP_HORIZONTAL_PADDING,
                        ) {
                            SettingsNavigationItem(
                                icon = Icons.Outlined.VideoLibrary,
                                thumbnailUri = item.uri,
                                thumbnailId = item.id,
                                durationLabel = item.durationLabel,
                                title = item.title,
                                description = item.folderPath,
                                onClick = { onPlay(item.uri.toString()) },
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.size(6.dp)) }
        }
    }
}

@Composable
private fun SettingsPageContent(
    modifier: Modifier = Modifier,
    appVersion: String,
    hapticFeedbackEnabled: Boolean,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenMotion: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = "播放") {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.PlayCircle,
                        title = "播放器",
                        description = "手势、界面与播放偏好",
                        onClick = onOpenPlayer,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = "外观") {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Palette,
                        title = "主题",
                        description = "主题色彩、深色模式和文本缩放",
                        onClick = onOpenTheme,
                    )
                }
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.AutoAwesomeMotion,
                        title = "动画",
                        description = "页面切换动画时长",
                        onClick = onOpenMotion,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = "交互") {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.TouchApp,
                        title = "振动",
                        description = "控制操作时的振动反馈",
                        checked = hapticFeedbackEnabled,
                        onCheckedChange = onHapticFeedbackEnabledChange,
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = stringResource(id = R.string.settings_general_title)) {
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Info,
                        title = stringResource(id = R.string.settings_about_title),
                        description = stringResource(id = R.string.settings_about_desc, appVersion),
                        onClick = {},
                    )
                }
                item {
                    SettingsNavigationItem(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(id = R.string.settings_refresh_title),
                        description = stringResource(id = R.string.settings_refresh_desc),
                        onClick = {},
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }

}

@Composable
private fun DoubleTapActionOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PlayerSettingsPlaceholderPageContent(
    modifier: Modifier = Modifier,
    playerSettings: PlayerSettingsConfig,
    onPlayerSettingsChange: (PlayerSettingsConfig) -> Unit,
) {
    val gestureSeekEnabled = playerSettings.seekGestureEnabled
    val gestureBrightnessEnabled = playerSettings.brightnessGestureEnabled
    val gestureVolumeEnabled = playerSettings.volumeGestureEnabled
    val gestureZoomEnabled = playerSettings.zoomGestureEnabled
    val gesturePanEnabled = playerSettings.panGestureEnabled
    val gestureDoubleTapEnabled = playerSettings.doubleTapGestureEnabled
    val doubleTapAction = playerSettings.doubleTapAction
    val doubleTapActionLabel = when (doubleTapAction) {
        "toggle_play_pause" -> "播放/暂停"
        "both" -> "快进/快退 + 播放/暂停"
        else -> "快进/快退"
    }
    val gestureLongPressEnabled = playerSettings.longPressGestureEnabled
    val seekIncrementSec = playerSettings.seekIncrementSec.toFloat()
    val seekSensitivity = playerSettings.seekSensitivity
    val longPressSpeed = playerSettings.longPressSpeed
    val controllerTimeoutSec = playerSettings.controllerTimeoutSec.toFloat()
    val hideButtonsBackground = playerSettings.hideButtonsBackground
    val resumePlaybackEnabled = playerSettings.resumePlayback
    val autoplayEnabled = playerSettings.autoplay
    val autoPipEnabled = playerSettings.autoPip
    val backgroundPlayEnabled = playerSettings.autoBackgroundPlay
    val rememberBrightnessEnabled = playerSettings.rememberBrightness
    val rememberSelectionsEnabled = playerSettings.rememberSelections
    val defaultPlaybackSpeed = playerSettings.defaultPlaybackSpeed
    var showDoubleTapActionDialog by remember { mutableStateOf(false) }
    var showLongPressSpeedDialog by remember { mutableStateOf(false) }
    var editingLongPressSpeed by remember { mutableFloatStateOf(longPressSpeed) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = "手势") {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Swipe,
                        title = "拖动快进手势",
                        description = "启用左右滑动快进/快退",
                        checked = gestureSeekEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(seekGestureEnabled = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.SwipeVertical,
                        title = "亮度手势",
                        description = "启用亮度上下滑动调节",
                        checked = gestureBrightnessEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(brightnessGestureEnabled = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.SwipeVertical,
                        title = "音量手势",
                        description = "启用音量上下滑动调节",
                        checked = gestureVolumeEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(volumeGestureEnabled = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Pinch,
                        title = "缩放手势",
                        description = "启用双指缩放",
                        checked = gestureZoomEnabled,
                        onCheckedChange = { enabled ->
                            onPlayerSettingsChange(
                                playerSettings.copy(
                                    zoomGestureEnabled = enabled,
                                    panGestureEnabled = if (enabled) playerSettings.panGestureEnabled else false,
                                ),
                            )
                        },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.PanToolAlt,
                        title = "平移手势",
                        description = if (gestureZoomEnabled) "缩放后可拖动画面" else "需先启用缩放手势",
                        checked = gesturePanEnabled,
                        enabled = gestureZoomEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(panGestureEnabled = it && gestureZoomEnabled)) },
                    )
                }
                item {
                    SettingsToggleNavigationItem(
                        icon = Icons.Rounded.DoubleArrow,
                        title = "双击手势",
                        description = if (gestureDoubleTapEnabled) "双击动作：$doubleTapActionLabel" else "已关闭",
                        checked = gestureDoubleTapEnabled,
                        onCheckedChange = { enabled ->
                            onPlayerSettingsChange(playerSettings.copy(doubleTapGestureEnabled = enabled))
                        },
                        onClick = { showDoubleTapActionDialog = true },
                    )
                }
                item {
                    SettingsToggleNavigationItem(
                        icon = Icons.Rounded.TouchApp,
                        title = "长按手势",
                        description = String.format(Locale.US, "长按临时加速播放（%.1fx）", longPressSpeed),
                        checked = gestureLongPressEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(longPressGestureEnabled = it)) },
                        onClick = {
                            editingLongPressSpeed = longPressSpeed
                            showLongPressSpeedDialog = true
                        },
                    )
                }
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.Replay10,
                        title = "快进步长",
                        description = "${seekIncrementSec.toInt()} 秒",
                        value = seekIncrementSec,
                        valueRange = 1f..60f,
                        steps = 58,
                        enabled = gestureSeekEnabled,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(seekIncrementSec = it.toInt().coerceIn(1, 60))) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(seekIncrementSec = 10)) },
                    )
                }
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.FastForward,
                        title = "快进灵敏度",
                        description = String.format(Locale.US, "%.1f", seekSensitivity),
                        value = seekSensitivity,
                        valueRange = 0.1f..2.0f,
                        enabled = gestureSeekEnabled,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(seekSensitivity = (it * 10).toInt() / 10f)) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(seekSensitivity = 1.0f)) },
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = "界面") {
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.Timer,
                        title = "控制层自动隐藏",
                        description = "${controllerTimeoutSec.toInt()} 秒",
                        value = controllerTimeoutSec,
                        valueRange = 1f..60f,
                        steps = 58,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(controllerTimeoutSec = it.toInt().coerceIn(1, 60))) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(controllerTimeoutSec = 3)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.HideSource,
                        title = "隐藏按钮背景",
                        description = "减少按钮背景遮挡",
                        checked = hideButtonsBackground,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(hideButtonsBackground = it)) },
                    )
                }
            }
        }

        item {
            SplicedColumnGroup(title = "播放") {
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.ResetTv,
                        title = "续播",
                        description = "自动恢复上次播放位置",
                        checked = resumePlaybackEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(resumePlayback = it)) },
                    )
                }
                item {
                    SettingsSliderItem(
                        icon = Icons.Rounded.Speed,
                        title = "默认播放速度",
                        description = String.format(Locale.US, "%.1fx", defaultPlaybackSpeed),
                        value = defaultPlaybackSpeed,
                        valueRange = 0.2f..4.0f,
                        onValueChange = { onPlayerSettingsChange(playerSettings.copy(defaultPlaybackSpeed = (it * 10).toInt() / 10f)) },
                        onReset = { onPlayerSettingsChange(playerSettings.copy(defaultPlaybackSpeed = 1.0f)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.PlayCircle,
                        title = "自动播放",
                        description = "播放结束后自动播放下一个",
                        checked = autoplayEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoplay = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.PictureInPictureAlt,
                        title = "自动进入画中画",
                        description = "离开播放器时进入画中画",
                        checked = autoPipEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoPip = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Headset,
                        title = "后台播放",
                        description = "切后台后继续播放",
                        checked = backgroundPlayEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(autoBackgroundPlay = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.BrightnessHigh,
                        title = "记住亮度",
                        description = "记住播放器亮度级别",
                        checked = rememberBrightnessEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(rememberBrightness = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        icon = Icons.Rounded.DoneAll,
                        title = "记住轨道选择",
                        description = "记住音轨和字幕选择",
                        checked = rememberSelectionsEnabled,
                        onCheckedChange = { onPlayerSettingsChange(playerSettings.copy(rememberSelections = it)) },
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.size(6.dp)) }
    }

    if (showDoubleTapActionDialog) {
        AlertDialog(
            onDismissRequest = { showDoubleTapActionDialog = false },
            title = { Text(text = "双击动作") },
            text = {
                Column {
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier.selectableGroup(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        item {
                            DoubleTapActionOptionRow(
                                text = "快进/快退",
                                selected = doubleTapAction == "seek",
                                onClick = {
                                    onPlayerSettingsChange(
                                        playerSettings.copy(doubleTapGestureEnabled = true, doubleTapAction = "seek"),
                                    )
                                    showDoubleTapActionDialog = false
                                },
                            )
                        }
                        item {
                            DoubleTapActionOptionRow(
                                text = "播放/暂停",
                                selected = doubleTapAction == "toggle_play_pause",
                                onClick = {
                                    onPlayerSettingsChange(
                                        playerSettings.copy(doubleTapGestureEnabled = true, doubleTapAction = "toggle_play_pause"),
                                    )
                                    showDoubleTapActionDialog = false
                                },
                            )
                        }
                        item {
                            DoubleTapActionOptionRow(
                                text = "快进/快退 + 播放/暂停",
                                selected = doubleTapAction == "both",
                                onClick = {
                                    onPlayerSettingsChange(
                                        playerSettings.copy(doubleTapGestureEnabled = true, doubleTapAction = "both"),
                                    )
                                    showDoubleTapActionDialog = false
                                },
                            )
                        }
                    }
                    HorizontalDivider()
                }
            },
            dismissButton = {
                TextButton(onClick = { showDoubleTapActionDialog = false }) {
                    Text(text = "取消")
                }
            },
            confirmButton = {},
        )
    }

    if (showLongPressSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showLongPressSpeedDialog = false },
            title = { Text(text = "长按手势") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", editingLongPressSpeed),
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Slider(
                        value = editingLongPressSpeed,
                        onValueChange = { value ->
                            editingLongPressSpeed = ((value * 10).toInt() / 10f).coerceIn(0.2f, 4.0f)
                        },
                        valueRange = 0.2f..4.0f,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLongPressSpeedDialog = false }) {
                    Text(text = "取消")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPlayerSettingsChange(
                            playerSettings.copy(longPressSpeed = editingLongPressSpeed.coerceIn(0.2f, 4.0f)),
                        )
                        showLongPressSpeedDialog = false
                    },
                ) {
                    Text(text = "完成")
                }
            },
        )
    }
}

@Composable
private fun SettingsSliderItem(
    icon: ImageVector? = null,
    title: String,
    description: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    steps: Int = 0,
    enabled: Boolean = true,
    onReset: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (enabled) 1f else 0.45f),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onReset != null) {
                    IconButton(
                        onClick = onReset,
                        enabled = enabled,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = "重置",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Slider(
                value = value.coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector? = null,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val onToggle: (Boolean) -> Unit = { checkedValue ->
        if (hapticsEnabled) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
        }
        onCheckedChange(checkedValue)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled) { onToggle(!checked) }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
        )
    }
}

@Composable
private fun SettingsToggleNavigationItem(
    icon: ImageVector? = null,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .clickable {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                }
                onClick()
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerticalDivider(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .height(40.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            )
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ThemeSettingsPageContent(
    modifier: Modifier = Modifier,
    themeConfig: ThemeConfig,
    customThemes: List<CustomThemeEntry>,
    hapticsEnabled: Boolean,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onCustomThemesChange: (List<CustomThemeEntry>) -> Unit,
) {
    val isDynamicSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val context = LocalContext.current
    val previewDark = themeConfig.appearance == ThemeAppearanceMode.Dark ||
        (themeConfig.appearance == ThemeAppearanceMode.System && androidx.compose.foundation.isSystemInDarkTheme())
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val dynamicPreviewScheme = if (isDynamicSupported) {
        if (previewDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else {
        colorSchemeFromSeed(Color(0xFF2E6CF6), previewDark)
    }
    val customPreviewScheme = if (previewDark) {
        androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFB6B6B6),
            onPrimary = Color(0xFF111111),
            primaryContainer = Color(0xFF3F3F3F),
            onPrimaryContainer = Color(0xFFEDEDED),
            surface = Color(0xFF1C1C1C),
            onSurface = Color(0xFFEDEDED),
            surfaceContainer = Color(0xFF2A2A2A),
            onSurfaceVariant = Color(0xFFBDBDBD),
            outline = Color(0xFF3A3A3A),
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF6E6E6E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0E0E0),
            onPrimaryContainer = Color(0xFF2A2A2A),
            surface = Color(0xFFF3F3F3),
            onSurface = Color(0xFF2B2B2B),
            surfaceContainer = Color(0xFFE7E7E7),
            onSurfaceVariant = Color(0xFF6C6C6C),
            outline = Color(0xFFB5B5B5),
        )
    }
    val monoPreviewScheme = monochromeColorScheme(previewDark)

    var customHex by remember(themeConfig.customSeed) { mutableStateOf(themeConfig.customSeed?.toHex() ?: "#2E6CF6") }
    var customName by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    var fontScaleSlider by remember(themeConfig.fontScale) { mutableStateOf(themeConfig.fontScale) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pendingDeleteId) {
                if (pendingDeleteId == null) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Final)
                        val anyConsumed = event.changes.any { it.isConsumed }
                        val allUp = event.changes.all { it.changedToUp() }
                        if (!anyConsumed && allUp) {
                            pendingDeleteId = null
                        }
                    }
                }
            },
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }

        item {
            SplicedColumnGroup(title = "主题模式") {
                item {
                    ThemeSectionBlock(
                        title = "外观",
                        description = "选择浅色、深色或跟随系统",
                    ) {
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ThemeAppearanceMode.values().forEach { appearance ->
                                androidx.compose.material3.FilterChip(
                                    selected = themeConfig.appearance == appearance,
                                    onClick = {
                                        if (hapticsEnabled) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                                        }
                                        onThemeConfigChange(themeConfig.copy(appearance = appearance))
                                    },
                                    label = {
                                        Text(
                                            when (appearance) {
                                                ThemeAppearanceMode.System -> "自动"
                                                ThemeAppearanceMode.Light -> "浅色"
                                                ThemeAppearanceMode.Dark -> "深色"
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SplicedColumnGroup(title = "主题色彩") {
                item {
                    ThemeSectionBlock(
                        title = "色彩方案",
                        description = if (isDynamicSupported) {
                            "已支持动态主题（Android 12+），可随系统壁纸变化。"
                        } else {
                            "当前系统不支持动态主题（Android 12+），将使用预设主题色。"
                        },
                    ) {
                        val swatches = buildList<ThemeSwatchItem> {
                            ThemePresets.filter { it.mode != ThemeMode.Custom }.forEach { add(ThemeSwatchItem.Preset(it)) }
                            customThemes.forEach { add(ThemeSwatchItem.CustomTheme(it)) }
                            add(ThemeSwatchItem.CustomAdd)
                        }
                        val columns = 4
                        val itemSize = 84.dp
                        val itemSpacing = 2.dp
                        val rows = (swatches.size + columns - 1) / columns
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(columns),
                            userScrollEnabled = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemSize * rows + itemSpacing * (rows - 1)),
                            verticalArrangement = Arrangement.spacedBy(itemSpacing),
                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                        ) {
                            items(swatches.size) { index ->
                                val swatch = swatches[index]
                                val preset = (swatch as? ThemeSwatchItem.Preset)?.preset
                                val disabled = preset?.mode == ThemeMode.Dynamic && !isDynamicSupported
                                val shape = gridItemShape(index = index, total = swatches.size, columns = columns, outerCorner = 16.dp, innerCorner = 6.dp)
                                when (swatch) {
                                    is ThemeSwatchItem.Preset -> {
                                        ThemeSwatch(
                                            label = swatch.preset.name,
                                            scheme = when (swatch.preset.mode) {
                                                ThemeMode.Dynamic -> dynamicPreviewScheme
                                                ThemeMode.Monochrome -> monoPreviewScheme
                                                else -> swatch.preset.seed?.let { colorSchemeFromSeed(it, previewDark) } ?: dynamicPreviewScheme
                                            },
                                            selected = themeConfig.mode == swatch.preset.mode,
                                            disabled = disabled,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            showDynamicIcon = swatch.preset.mode == ThemeMode.Dynamic,
                                            onClick = {
                                                pendingDeleteId = null
                                                if (disabled) return@ThemeSwatch
                                                onThemeConfigChange(
                                                    themeConfig.copy(
                                                        mode = swatch.preset.mode,
                                                        customSeed = swatch.preset.seed,
                                                        customThemeId = null,
                                                        customMonochrome = false,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                    is ThemeSwatchItem.CustomAdd -> {
                                        ThemeSwatch(
                                            label = "自定义",
                                            scheme = customPreviewScheme,
                                            selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == null,
                                            disabled = false,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            icon = Icons.Outlined.Add,
                                            onClick = {
                                                pendingDeleteId = null
                                                customName = ""
                                                showCustomDialog = true
                                            },
                                        )
                                    }
                                    is ThemeSwatchItem.CustomTheme -> {
                                        ThemeSwatch(
                                            label = swatch.theme.name,
                                            scheme = colorSchemeFromSeed(swatch.theme.seed, previewDark),
                                            selected = themeConfig.mode == ThemeMode.Custom && themeConfig.customThemeId == swatch.theme.id,
                                            disabled = false,
                                            shape = shape,
                                            size = itemSize,
                                            hapticsEnabled = hapticsEnabled,
                                            showDelete = pendingDeleteId == swatch.theme.id,
                                            onClick = {
                                                if (pendingDeleteId == swatch.theme.id) {
                                                    confirmDeleteId = swatch.theme.id
                                                } else {
                                                    pendingDeleteId = null
                                                    onThemeConfigChange(
                                                        themeConfig.copy(
                                                            mode = ThemeMode.Custom,
                                                            customThemeId = swatch.theme.id,
                                                            customSeed = swatch.theme.seed,
                                                            customMonochrome = swatch.theme.monochrome,
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongPress = { pendingDeleteId = swatch.theme.id },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SplicedColumnGroup(title = "显示") {
                item {
                    SettingsToggleItem(
                        title = "纯黑模式",
                        description = "深色下使用 AMOLED 纯黑背景",
                        checked = themeConfig.pureBlack,
                        onCheckedChange = { onThemeConfigChange(themeConfig.copy(pureBlack = it)) },
                    )
                }
                item {
                    SettingsToggleItem(
                        title = "文本缩放",
                        description = "关闭后使用默认字号",
                        checked = themeConfig.fontScaleEnabled,
                        onCheckedChange = { enabled ->
                            onThemeConfigChange(themeConfig.copy(fontScaleEnabled = enabled))
                        },
                    )
                }
                item {
                    ThemeSectionBlock(
                        title = "缩放比例",
                        description = "当前 ${(fontScaleSlider * 100).toInt()}%",
                    ) {
                        Slider(
                            value = fontScaleSlider,
                            onValueChange = {
                                fontScaleSlider = it
                                onThemeConfigChange(
                                    themeConfig.copy(
                                        fontScale = it,
                                        fontScaleEnabled = true,
                                    ),
                                )
                            },
                            valueRange = 0.85f..1.3f,
                            enabled = themeConfig.fontScaleEnabled,
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(6.dp)) }
    }

    if (showCustomDialog) {
        CustomThemeSheet(
            initialHex = customHex,
            initialName = customName,
            previewDark = previewDark,
            hapticsEnabled = hapticsEnabled,
            onDismiss = { showCustomDialog = false },
            onSave = { name, seed ->
                val safeName = name.ifBlank { nextCustomThemeName(customThemes) }
                val entry = CustomThemeEntry(
                    id = java.util.UUID.randomUUID().toString(),
                    name = safeName,
                    seedArgb = seed.toArgb(),
                    monochrome = false,
                )
                customHex = seed.toHex()
                onCustomThemesChange(customThemes + entry)
                onThemeConfigChange(
                    themeConfig.copy(
                        mode = ThemeMode.Custom,
                        customThemeId = entry.id,
                        customSeed = entry.seed,
                        customMonochrome = false,
                    ),
                )
                showCustomDialog = false
            },
        )
    }

    if (confirmDeleteId != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                confirmDeleteId = null
                pendingDeleteId = null
            },
            title = { Text("删除自定义主题？") },
            text = { Text("此操作无法撤回。") },
            confirmButton = {
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        val id = confirmDeleteId ?: return@FilledTonalButton
                        val updated = customThemes.filterNot { it.id == id }
                        onCustomThemesChange(updated)
                        if (themeConfig.customThemeId == id) {
                            onThemeConfigChange(
                                themeConfig.copy(
                                    mode = ThemeMode.Monochrome,
                                    customThemeId = null,
                                    customSeed = null,
                                    customMonochrome = false,
                                ),
                            )
                        }
                        confirmDeleteId = null
                        pendingDeleteId = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmDeleteId = null
                        pendingDeleteId = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ThemeSectionBlock(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )
        content()
    }
}

@Composable
private fun ThemeSwatch(
    label: String,
    scheme: androidx.compose.material3.ColorScheme,
    selected: Boolean,
    disabled: Boolean,
    shape: androidx.compose.foundation.shape.RoundedCornerShape,
    size: androidx.compose.ui.unit.Dp,
    hapticsEnabled: Boolean,
    icon: ImageVector? = null,
    showDynamicIcon: Boolean = false,
    showDelete: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        shape = shape,
        tonalElevation = if (selected) 4.dp else 1.dp,
        modifier = Modifier
            .size(size)
            .clip(shape)
            .combinedClickable(
                enabled = !disabled,
                onClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                    }
                    onClick()
                },
                onLongClick = {
                    if (hapticsEnabled) {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }
                    onLongPress?.invoke()
                },
            ),
        color = scheme.surfaceContainer,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    if (showDelete) {
                        DeleteSwatch(borderColor = scheme.primary)
                    } else {
                        ColorSwatch(
                            color = scheme.primary,
                            selected = selected,
                            icon = icon,
                            iconTint = if (icon != null) scheme.onPrimaryContainer else null,
                            borderColor = scheme.primary,
                        )
                        if (showDynamicIcon) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = null,
                                tint = scheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 3.dp, y = 3.dp),
                            )
                        }
                    }
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (disabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

private fun gridItemShape(
    index: Int,
    total: Int,
    columns: Int,
    outerCorner: androidx.compose.ui.unit.Dp,
    innerCorner: androidx.compose.ui.unit.Dp,
): androidx.compose.foundation.shape.RoundedCornerShape {
    val rows = (total + columns - 1) / columns
    val row = index / columns
    val col = index % columns
    val isTop = row == 0
    val isBottom = row == rows - 1
    val isLeft = col == 0
    val isRight = col == columns - 1 || index == total - 1
    return androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (isTop && isLeft) outerCorner else innerCorner,
        topEnd = if (isTop && isRight) outerCorner else innerCorner,
        bottomStart = if (isBottom && isLeft) outerCorner else innerCorner,
        bottomEnd = if (isBottom && isRight) outerCorner else innerCorner,
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean = false,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    borderColor: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = androidx.compose.foundation.shape.CircleShape
    val borderWidth = if (selected) 2.dp else 1.dp
    val strokeColor = if (selected) borderColor else borderColor.copy(alpha = 0.45f)
    val innerPadding = if (selected) 2.dp else 0.dp
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(borderWidth, strokeColor, shape)
            .padding(innerPadding)
            .clip(shape),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.85f)))
            }
            Row(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.65f)))
                Box(modifier = Modifier.weight(1f).fillMaxSize().background(color.copy(alpha = 0.5f)))
            }
        }
        if (icon != null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint ?: MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            }
        } else if (selected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun DeleteSwatch(borderColor: Color) {
    val shape = androidx.compose.foundation.shape.CircleShape
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(2.dp, borderColor, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CustomThemeSheet(
    initialHex: String,
    initialName: String,
    previewDark: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Color) -> Unit,
) {
    var hex by remember(initialHex) { mutableStateOf(initialHex) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var isError by remember { mutableStateOf(false) }
    val parsed = parseHexColor(hex)
    val defaultColor = MaterialTheme.colorScheme.primary
    var currentColor by remember { mutableStateOf(parsed ?: defaultColor) }
    val paletteMode = remember { mutableStateOf(CustomPaletteMode.Grid) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    if (parsed != null && parsed.toArgb() != currentColor.toArgb()) {
        currentColor = parsed
    }

    val previewScheme = colorSchemeFromSeed(currentColor, previewDark)
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("自定义主题", style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CustomPaletteMode.values().forEach { mode ->
                    androidx.compose.material3.FilterChip(
                        selected = paletteMode.value == mode,
                        onClick = {
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                            }
                            paletteMode.value = mode
                        },
                        label = { Text(mode.label) },
                    )
                }
            }

            when (paletteMode.value) {
                CustomPaletteMode.Grid -> GridPalette(colors = CustomGridPalette, selected = currentColor, cellSize = GridCellSize) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }
                CustomPaletteMode.Spectrum -> SpectrumPalette(color = currentColor, width = GridPaletteWidth, height = GridPaletteHeight) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }
                CustomPaletteMode.Sliders -> RgbSliders(color = currentColor) {
                    currentColor = it
                    hex = it.toHex()
                    isError = false
                }
            }

            androidx.compose.material3.OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("主题名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.OutlinedTextField(
                value = hex,
                onValueChange = {
                    hex = it
                    isError = false
                },
                label = { Text("输入 #RRGGBB") },
                singleLine = true,
                isError = isError,
                trailingIcon = { ColorSwatch(color = parsed ?: currentColor) },
                supportingText = {
                    Text(
                        if (isError) "请输入合法的十六进制颜色" else "示例: #2E6CF6 或 #34D6A6",
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewChip("主色", previewScheme.primary, previewScheme.onPrimary, Modifier.weight(1f))
                PreviewChip("容器", previewScheme.primaryContainer, previewScheme.onPrimaryContainer, Modifier.weight(1f))
                PreviewChip("强调", previewScheme.tertiaryContainer, previewScheme.onTertiaryContainer, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                androidx.compose.material3.FilledTonalButton(
                    onClick = {
                        val color = parsed
                        if (color != null) onSave(name, color) else isError = true
                    },
                    enabled = parsed != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存并启用")
                }
            }
        }
    }
}

@Composable
private fun PreviewChip(label: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = background, tonalElevation = 1.dp, modifier = modifier) {
        Box(modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = foreground)
        }
    }
}

@Composable
private fun GridPalette(
    colors: List<List<Color>>,
    selected: Color,
    cellSize: androidx.compose.ui.unit.Dp,
    onPick: (Color) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .size(GridPaletteWidth, GridPaletteHeight)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            colors.forEach { row ->
                Row {
                    row.forEach { color ->
                        val isSelected = color.toArgb() == selected.toArgb()
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                    else Modifier,
                                )
                                .clickable { onPick(color) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpectrumPalette(
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onPick: (Color) -> Unit,
) {
    val hsv = remember(color) { color.toHsv() }
    var boxSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(colors = SpectrumHueColors))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(colors = listOf(Color.White, Color.Transparent)))
                    .background(androidx.compose.ui.graphics.Brush.horizontalGradient(colors = listOf(Color.Transparent, Color.Black)))
                    .onSizeChanged { boxSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> onPick(spectrumColorFor(offset, size)) },
                            onDrag = { change, _ -> onPick(spectrumColorFor(change.position, size)) },
                        )
                    },
            ) {
                val indicatorX = hsv[1].coerceIn(0f, 1f)
                val indicatorY = (hsv[0] / 360f).coerceIn(0f, 1f)
                val offsetX = with(density) { (indicatorX * (boxSize.width - 24.dp.toPx()).coerceAtLeast(0f)).toDp() }
                val offsetY = with(density) { (indicatorY * (boxSize.height - 24.dp.toPx()).coerceAtLeast(0f)).toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(24.dp)
                        .border(2.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                )
            }
        }
    }
}

@Composable
private fun RgbSliders(color: Color, onColorChange: (Color) -> Unit) {
    val rgb = color.toRgb()
    var r by remember(color) { mutableIntStateOf(rgb.first) }
    var g by remember(color) { mutableIntStateOf(rgb.second) }
    var b by remember(color) { mutableIntStateOf(rgb.third) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        RgbSliderRow("红色", r) {
            r = it
            onColorChange(Color(r / 255f, g / 255f, b / 255f))
        }
        RgbSliderRow("绿色", g) {
            g = it
            onColorChange(Color(r / 255f, g / 255f, b / 255f))
        }
        RgbSliderRow("蓝色", b) {
            b = it
            onColorChange(Color(r / 255f, g / 255f, b / 255f))
        }
    }
}

@Composable
private fun RgbSliderRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Slider(
                value = value.toFloat(),
                onValueChange = {
                    val next = it.toInt().coerceIn(0, 255)
                    textValue = next.toString()
                    onValueChange(next)
                },
                valueRange = 0f..255f,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.OutlinedTextField(
                value = textValue,
                onValueChange = { raw ->
                    val clean = raw.filter { it.isDigit() }.take(3)
                    textValue = clean
                    clean.toIntOrNull()?.let { onValueChange(it.coerceIn(0, 255)) }
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                modifier = Modifier.width(72.dp),
            )
        }
    }
}

private fun nextCustomThemeName(existing: List<CustomThemeEntry>): String = "自定义主题 ${existing.size + 1}"
private fun parseHexColor(value: String): Color? = runCatching { Color(android.graphics.Color.parseColor(value.trim())) }.getOrNull()
private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
private fun Color.toRgb(): Triple<Int, Int, Int> {
    val argb = toArgb()
    return Triple((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}
private fun Color.toHsv(): FloatArray = FloatArray(3).also { android.graphics.Color.colorToHSV(toArgb(), it) }
private fun hsvColor(hue: Float, saturation: Float, value: Float): Color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
private fun spectrumColorFor(offset: androidx.compose.ui.geometry.Offset, size: androidx.compose.ui.unit.IntSize): Color {
    val x = (offset.x / size.width).coerceIn(0f, 1f)
    val y = (offset.y / size.height).coerceIn(0f, 1f)
    return hsvColor(y * 360f, x, 1f - x)
}

private const val GridColumns = 12
private const val GridRows = 9
private val GridCellSize = 24.dp
private val GridPaletteWidth = GridCellSize * GridColumns
private val GridPaletteHeight = GridCellSize * GridRows
private val SpectrumHueColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000),
)
private val CustomGridPalette = buildGridPalette()

private fun buildGridPalette(): List<List<Color>> {
    val grid = mutableListOf<List<Color>>()
    val grayRow = (0 until GridColumns).map { i ->
        val t = i / (GridColumns - 1f)
        val v = 1f - t * 0.9f
        Color(v, v, v)
    }
    grid.add(grayRow)
    for (r in 0 until GridRows - 1) {
        val value = 0.95f - r * 0.08f
        val saturation = 0.85f - r * 0.06f
        val row = (0 until GridColumns).map { c ->
            val hue = (c / (GridColumns - 1f)) * 330f
            hsvColor(hue, saturation.coerceIn(0.2f, 1f), value.coerceIn(0.25f, 1f))
        }
        grid.add(row)
    }
    return grid
}

private enum class CustomPaletteMode(val label: String) {
    Grid("网格"),
    Spectrum("光谱"),
    Sliders("滑块"),
}

private sealed interface ThemeSwatchItem {
    data class Preset(val preset: ThemePreset) : ThemeSwatchItem
    data class CustomTheme(val theme: CustomThemeEntry) : ThemeSwatchItem
    data object CustomAdd : ThemeSwatchItem
}

@Composable
private fun MotionSettingsPageContent(
    modifier: Modifier = Modifier,
    navDurationMs: Int,
    onNavDurationChange: (Int) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item { Spacer(modifier = Modifier.size(12.dp)) }
        item {
            SplicedColumnGroup(title = "动画") {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        val minMs = 0
                        val maxMs = 600
                        val stepMs = 10
                        val sliderValue = navDurationMs.coerceIn(minMs, maxMs).toFloat()
                        Text(
                            text = "过渡时长",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "当前 ${sliderValue.toInt()} ms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        Slider(
                            value = sliderValue,
                            valueRange = minMs.toFloat()..maxMs.toFloat(),
                            steps = ((maxMs - minMs) / stepMs) - 1,
                            onValueChange = {
                                val stepped = ((it / stepMs).toInt() * stepMs).coerceIn(minMs, maxMs)
                                onNavDurationChange(stepped)
                            },
                        )
                        Text(
                            text = "提示：0ms 表示无过渡（瞬切）。默认值：350ms。若系统“动画时长比例”为 0，系统可能强制关闭动画。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.size(6.dp)) }
    }
}

@Composable
private fun AsukaTheme(
    themeConfig: ThemeConfig,
    content: @Composable () -> Unit,
) {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = when (themeConfig.appearance) {
        ThemeAppearanceMode.System -> isSystemDark
        ThemeAppearanceMode.Light -> false
        ThemeAppearanceMode.Dark -> true
    }
    val presetSeed = ThemePresets.firstOrNull { it.mode == themeConfig.mode }?.seed
    val seed = when (themeConfig.mode) {
        ThemeMode.Custom -> themeConfig.customSeed ?: Color(0xFF2E6CF6)
        ThemeMode.Dynamic -> null
        ThemeMode.Monochrome -> null
        else -> presetSeed ?: Color(0xFF2E6CF6)
    }
    val baseScheme = when {
        themeConfig.mode == ThemeMode.Dynamic && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ->
            if (dark) androidx.compose.material3.dynamicDarkColorScheme(LocalContext.current)
            else androidx.compose.material3.dynamicLightColorScheme(LocalContext.current)
        themeConfig.mode == ThemeMode.Monochrome || (themeConfig.mode == ThemeMode.Custom && themeConfig.customMonochrome) ->
            monochromeColorScheme(dark)
        seed != null -> colorSchemeFromSeed(seed = seed, darkTheme = dark)
        else -> if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    }
    val scheme = if (themeConfig.pureBlack && dark) applyPureBlackScheme(baseScheme) else baseScheme
    val scaledTypography = MaterialTheme.typography.scaled(
        if (themeConfig.fontScaleEnabled) themeConfig.fontScale else 1f,
    )
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    if (window != null && !view.isInEditMode) {
        SideEffect {
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.setBackgroundDrawable(ColorDrawable(scheme.surfaceContainerLowest.toArgb()))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = scaledTypography,
        content = content,
    )
}

private fun colorSchemeFromSeed(seed: Color, darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    val scheme = SchemeTonalSpot(Hct.fromInt(seed.toArgb()), darkTheme, 0.0)
    return colorSchemeFromDynamic(scheme = scheme, darkTheme = darkTheme)
}

private fun colorSchemeFromDynamic(
    scheme: DynamicScheme,
    darkTheme: Boolean,
): androidx.compose.material3.ColorScheme {
    val materialColors = MaterialDynamicColors()
    fun DynamicColor.c(): Color = Color(getArgb(scheme))

    return if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = materialColors.primary().c(),
            onPrimary = materialColors.onPrimary().c(),
            primaryContainer = materialColors.primaryContainer().c(),
            onPrimaryContainer = materialColors.onPrimaryContainer().c(),
            inversePrimary = materialColors.inversePrimary().c(),
            secondary = materialColors.secondary().c(),
            onSecondary = materialColors.onSecondary().c(),
            secondaryContainer = materialColors.secondaryContainer().c(),
            onSecondaryContainer = materialColors.onSecondaryContainer().c(),
            tertiary = materialColors.tertiary().c(),
            onTertiary = materialColors.onTertiary().c(),
            tertiaryContainer = materialColors.tertiaryContainer().c(),
            onTertiaryContainer = materialColors.onTertiaryContainer().c(),
            background = materialColors.background().c(),
            onBackground = materialColors.onBackground().c(),
            surface = materialColors.surface().c(),
            onSurface = materialColors.onSurface().c(),
            surfaceDim = materialColors.surfaceDim().c(),
            surfaceBright = materialColors.surfaceBright().c(),
            surfaceContainerLowest = materialColors.surfaceContainerLowest().c(),
            surfaceContainerLow = materialColors.surfaceContainerLow().c(),
            surfaceContainer = materialColors.surfaceContainer().c(),
            surfaceContainerHigh = materialColors.surfaceContainerHigh().c(),
            surfaceContainerHighest = materialColors.surfaceContainerHighest().c(),
            surfaceVariant = materialColors.surfaceVariant().c(),
            onSurfaceVariant = materialColors.onSurfaceVariant().c(),
            surfaceTint = materialColors.primary().c(),
            inverseSurface = materialColors.inverseSurface().c(),
            inverseOnSurface = materialColors.inverseOnSurface().c(),
            error = materialColors.error().c(),
            onError = materialColors.onError().c(),
            errorContainer = materialColors.errorContainer().c(),
            onErrorContainer = materialColors.onErrorContainer().c(),
            outline = materialColors.outline().c(),
            outlineVariant = materialColors.outlineVariant().c(),
            scrim = Color(0x66000000),
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = materialColors.primary().c(),
            onPrimary = materialColors.onPrimary().c(),
            primaryContainer = materialColors.primaryContainer().c(),
            onPrimaryContainer = materialColors.onPrimaryContainer().c(),
            inversePrimary = materialColors.inversePrimary().c(),
            secondary = materialColors.secondary().c(),
            onSecondary = materialColors.onSecondary().c(),
            secondaryContainer = materialColors.secondaryContainer().c(),
            onSecondaryContainer = materialColors.onSecondaryContainer().c(),
            tertiary = materialColors.tertiary().c(),
            onTertiary = materialColors.onTertiary().c(),
            tertiaryContainer = materialColors.tertiaryContainer().c(),
            onTertiaryContainer = materialColors.onTertiaryContainer().c(),
            background = materialColors.background().c(),
            onBackground = materialColors.onBackground().c(),
            surface = materialColors.surface().c(),
            onSurface = materialColors.onSurface().c(),
            surfaceDim = materialColors.surfaceDim().c(),
            surfaceBright = materialColors.surfaceBright().c(),
            surfaceContainerLowest = materialColors.surfaceContainerLowest().c(),
            surfaceContainerLow = materialColors.surfaceContainerLow().c(),
            surfaceContainer = materialColors.surfaceContainer().c(),
            surfaceContainerHigh = materialColors.surfaceContainerHigh().c(),
            surfaceContainerHighest = materialColors.surfaceContainerHighest().c(),
            surfaceVariant = materialColors.surfaceVariant().c(),
            onSurfaceVariant = materialColors.onSurfaceVariant().c(),
            surfaceTint = materialColors.primary().c(),
            inverseSurface = materialColors.inverseSurface().c(),
            inverseOnSurface = materialColors.inverseOnSurface().c(),
            error = materialColors.error().c(),
            onError = materialColors.onError().c(),
            errorContainer = materialColors.errorContainer().c(),
            onErrorContainer = materialColors.onErrorContainer().c(),
            outline = materialColors.outline().c(),
            outlineVariant = materialColors.outlineVariant().c(),
            scrim = Color(0x66000000),
        )
    }
}

private fun monochromeColorScheme(darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    val scheme = SchemeMonochrome(Hct.fromInt(Color(0xFF808080).toArgb()), darkTheme, 0.0)
    return colorSchemeFromDynamic(scheme = scheme, darkTheme = darkTheme)
}

private fun applyPureBlackScheme(
    base: androidx.compose.material3.ColorScheme,
): androidx.compose.material3.ColorScheme {
    return base.copy(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF141414),
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF050505),
        surfaceContainer = Color(0xFF0A0A0A),
        surfaceContainerHigh = Color(0xFF101010),
        surfaceContainerHighest = Color(0xFF161616),
        surfaceVariant = Color(0xFF121212),
        onSurfaceVariant = Color(0xFFD7D7D7),
        surfaceTint = base.primary.copy(alpha = 0.9f),
        inverseSurface = Color(0xFFE6E6E6),
        inverseOnSurface = Color(0xFF121212),
        outline = Color(0xFF4A4A4A),
        outlineVariant = Color(0xFF2A2A2A),
        scrim = Color(0xB3000000),
    )
}

private fun androidx.compose.material3.Typography.scaled(factor: Float): androidx.compose.material3.Typography {
    val safeFactor = factor.coerceIn(0.85f, 1.3f)
    fun androidx.compose.ui.text.TextStyle.scaled() = copy(
        fontSize = fontSize * safeFactor,
        lineHeight = lineHeight * safeFactor,
        letterSpacing = letterSpacing * safeFactor,
    )
    return androidx.compose.material3.Typography(
        displayLarge = displayLarge.scaled(),
        displayMedium = displayMedium.scaled(),
        displaySmall = displaySmall.scaled(),
        headlineLarge = headlineLarge.scaled(),
        headlineMedium = headlineMedium.scaled(),
        headlineSmall = headlineSmall.scaled(),
        titleLarge = titleLarge.scaled(),
        titleMedium = titleMedium.scaled(),
        titleSmall = titleSmall.scaled(),
        bodyLarge = bodyLarge.scaled(),
        bodyMedium = bodyMedium.scaled(),
        bodySmall = bodySmall.scaled(),
        labelLarge = labelLarge.scaled(),
        labelMedium = labelMedium.scaled(),
        labelSmall = labelSmall.scaled(),
    )
}

private fun parseColorHexOrNull(value: String): Color? {
    val raw = value.trim().removePrefix("#")
    if (raw.length != 6 && raw.length != 8) return null
    return runCatching {
        val normalized = if (raw.length == 6) "FF$raw" else raw
        Color(normalized.toLong(16).toInt())
    }.getOrNull()
}

@Composable
private fun SettingsRadioItem(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .clickable {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                }
                onClick()
            }
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CustomThemeRow(
    theme: CustomThemeEntry,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DEFAULT_ITEM_ROW_HEIGHT)
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(theme.seed),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = theme.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (theme.monochrome) "黑白模式" else "彩色模式",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(
            modifier = Modifier.size(28.dp),
            onClick = onDelete,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun LoadingBlock() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun EmptyBlock(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 32.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun GroupedListRow(
    index: Int,
    totalCount: Int,
    itemSpacing: androidx.compose.ui.unit.Dp = GROUP_ITEM_SPACING_DEFAULT,
    horizontalPadding: androidx.compose.ui.unit.Dp = GROUP_HORIZONTAL_PADDING_DEFAULT,
    useSoftCornersOnly: Boolean = false,
    useLargeCornersOnly: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isFirst = index == 0
    val isLast = index == totalCount - 1
    val shape = if (useLargeCornersOnly) {
        androidx.compose.foundation.shape.RoundedCornerShape(VIDEO_PAGE_CORNER_RADIUS)
    } else if (useSoftCornersOnly) {
        androidx.compose.foundation.shape.RoundedCornerShape(GROUP_SOFT_CORNER_RADIUS)
    } else {
        androidx.compose.foundation.shape.RoundedCornerShape(
            topStart = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
            topEnd = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
            bottomStart = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
            bottomEnd = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
        )
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(top = if (isFirst) 0.dp else itemSpacing),
        color = MaterialTheme.colorScheme.surfaceBright,
        shape = shape,
    ) {
        content()
    }
}

private data class SplicedItemData(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

private class SplicedGroupScope {
    val items = mutableListOf<SplicedItemData>()

    fun item(
        key: Any? = null,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        items.add(SplicedItemData(key ?: items.size, visible, content))
    }
}

@Composable
private fun SplicedColumnGroup(
    title: String,
    content: SplicedGroupScope.() -> Unit,
) {
    val scope = remember { SplicedGroupScope() }
    scope.items.clear()
    scope.content()

    val allItems = scope.items
    if (allItems.isEmpty()) return

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )

        allItems.forEachIndexed { index, itemData ->
            key(itemData.key) {
                if (itemData.visible) {
                    val isFirst = index == 0
                    val isLast = index == allItems.lastIndex
                    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                        topEnd = if (isFirst) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                        bottomStart = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                        bottomEnd = if (isLast) GROUP_OUTER_CORNER_RADIUS else GROUP_SOFT_CORNER_RADIUS,
                    )
                    val topPadding = if (index == 0) 0.dp else 2.dp
                    Column(
                        modifier = Modifier
                            .padding(top = topPadding)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surfaceBright),
                    ) {
                        itemData.content()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    thumbnailUri: Uri? = null,
    thumbnailId: Long? = null,
    durationLabel: String? = null,
    title: String,
    description: String,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val hapticsEnabled = LocalHapticsEnabled.current
    val alpha = if (selected) 1f else 0.92f
    val useVideoSubtitleStyle = thumbnailUri != null || thumbnailId != null
    val isVideoRow = thumbnailUri != null || thumbnailId != null
    val rowHorizontalPadding = if (isVideoRow) VIDEO_ROW_HORIZONTAL_PADDING else DEFAULT_ROW_HORIZONTAL_PADDING
    val itemSpacing = if (isVideoRow) VIDEO_ROW_ITEM_SPACING else DEFAULT_ROW_ITEM_SPACING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (isVideoRow) VIDEO_ITEM_ROW_HEIGHT else DEFAULT_ITEM_ROW_HEIGHT)
            .clickable {
                if (hapticsEnabled) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
                }
                onClick()
            }
            .padding(horizontal = rowHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumbOrIcon(
            icon = icon,
            thumbnailUri = thumbnailUri,
            thumbnailId = thumbnailId,
            durationLabel = durationLabel,
            selected = selected,
        )

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                maxLines = if (isVideoRow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = if (useVideoSubtitleStyle) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                maxLines = if (isVideoRow) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!isVideoRow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

@Composable
private fun VideoThumbOrIcon(
    icon: ImageVector,
    thumbnailUri: Uri?,
    thumbnailId: Long?,
    durationLabel: String?,
    selected: Boolean,
) {
    val thumb = rememberVideoThumbnail(thumbnailUri, thumbnailId)
    val shouldUseThumbnailSlot = thumbnailUri != null || thumbnailId != null
    if (shouldUseThumbnailSlot) {
        Crossfade(
            targetState = thumb,
            animationSpec = tween(durationMillis = 220),
            label = "VideoThumbCrossfade",
        ) { image ->
            Box(
                modifier = Modifier
                    .size(width = VIDEO_ITEM_THUMB_WIDTH, height = VIDEO_ITEM_THUMB_HEIGHT)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(VIDEO_PAGE_CORNER_RADIUS)),
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
                if (!durationLabel.isNullOrBlank()) {
                    Text(
                        text = durationLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 3.dp, bottom = 3.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun rememberVideoThumbnail(
    uri: Uri?,
    thumbnailId: Long?,
): androidx.compose.ui.graphics.ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = thumbnailCacheKey(thumbnailId = thumbnailId, uri = uri)
    val cached = cacheKey?.let { VideoThumbnailCache.get(it) }
    val bitmap by produceState<Bitmap?>(initialValue = cached, cacheKey) {
        if (cacheKey == null || uri == null || value != null) return@produceState
        value = withContext(Dispatchers.IO) {
            VideoThumbnailCache.loadSemaphore.withPermit {
                loadOrCreateVideoThumbnail(
                    context = context,
                    uri = uri,
                    thumbnailId = thumbnailId,
                )
            }
        }?.also { loaded ->
            VideoThumbnailCache.put(cacheKey, loaded)
        }
    }
    return bitmap?.asImageBitmap()
}

private fun loadOrCreateVideoThumbnail(
    context: android.content.Context,
    uri: Uri,
    thumbnailId: Long?,
): Bitmap? {
    val cachedFile = thumbnailId?.let { videoThumbnailFile(context, it) }
    cachedFile?.takeIf { it.exists() }?.let { file ->
        BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
    }

    val generated = loadVideoThumbnail(context, uri) ?: return null
    if (cachedFile != null) {
        runCatching {
            FileOutputStream(cachedFile).use { out ->
                generated.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
        }
    }
    return generated
}

private fun videoThumbnailFile(context: android.content.Context, thumbnailId: Long): File {
    val dir = File(context.cacheDir, "video_thumb_cache").apply { mkdirs() }
    return File(dir, "${thumbnailId}_v$VIDEO_THUMB_VERSION.jpg")
}

private fun thumbnailCacheKey(thumbnailId: Long?, uri: Uri?): String? {
    return when {
        thumbnailId != null -> "${thumbnailId}_v$VIDEO_THUMB_VERSION"
        uri != null -> "u_${uri.hashCode()}_v$VIDEO_THUMB_VERSION"
        else -> null
    }
}

private suspend fun prefetchFolderThumbnails(
    context: android.content.Context,
    folder: LocalVideoFolder?,
    limit: Int,
) {
    val videos = folder?.videos.orEmpty().take(limit.coerceAtLeast(1))
    videos.forEach { video ->
        val key = thumbnailCacheKey(thumbnailId = video.id, uri = video.uri) ?: return@forEach
        if (VideoThumbnailCache.get(key) != null) return@forEach
        val loaded = VideoThumbnailCache.loadSemaphore.withPermit {
            loadOrCreateVideoThumbnail(
                context = context,
                uri = video.uri,
                thumbnailId = video.id,
            )
        } ?: return@forEach
        VideoThumbnailCache.put(key, loaded)
    }
}

private suspend fun warmupInitialThumbnails(
    context: android.content.Context,
    videos: List<LocalVideoItem>,
    limit: Int,
) {
    videos.take(limit.coerceAtLeast(1)).forEach { video ->
        VideoThumbnailCache.loadSemaphore.withPermit {
            ensureThumbnailFile(
                context = context,
                uri = video.uri,
                thumbnailId = video.id,
            )
        }
    }
}

private fun ensureThumbnailFile(
    context: android.content.Context,
    uri: Uri,
    thumbnailId: Long,
) {
    val cachedFile = videoThumbnailFile(context, thumbnailId)
    if (cachedFile.exists()) return
    val generated = loadVideoThumbnail(context, uri) ?: return
    runCatching {
        FileOutputStream(cachedFile).use { out ->
            generated.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
    }
}

private fun loadVideoThumbnail(context: android.content.Context, uri: Uri): Bitmap? {
    val fromFrame = runCatching {
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource(context, uri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            val targetTimeUs = (durationMs * 1000L / 3L).coerceAtLeast(0L)
            val frame = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frame?.let { limitBitmapEdge(it, maxEdge = 960) }
        }
    }.getOrNull()
    return fromFrame
}

private fun limitBitmapEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
    if (maxEdge <= 0) return bitmap
    val width = bitmap.width
    val height = bitmap.height
    val longest = maxOf(width, height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest.toFloat()
    val targetWidth = (width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private object VideoThumbnailCache {
    private val cache = LruCache<String, Bitmap>(120)
    val loadSemaphore = Semaphore(2)

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }
}

private const val VIDEO_THUMB_VERSION = 2
private const val INITIAL_THUMB_WARMUP_LIMIT = 80
private val VIDEO_ITEM_THUMB_WIDTH = 116.dp
private val VIDEO_ITEM_THUMB_HEIGHT = 74.dp
private val VIDEO_ITEM_ROW_HEIGHT = 92.dp
private val DEFAULT_ITEM_ROW_HEIGHT = 64.dp
private val GROUP_OUTER_CORNER_RADIUS = 24.dp
private val GROUP_SOFT_CORNER_RADIUS = 6.dp
private val VIDEO_PAGE_CORNER_RADIUS = 8.dp
private val GROUP_ITEM_SPACING_DEFAULT = 2.dp
private val GROUP_HORIZONTAL_PADDING_DEFAULT = 16.dp
private val VIDEO_GROUP_HORIZONTAL_PADDING = 16.dp
private val DEFAULT_ROW_HORIZONTAL_PADDING = 16.dp
private val VIDEO_ROW_HORIZONTAL_PADDING = 12.dp
private val DEFAULT_ROW_ITEM_SPACING = 16.dp
private val VIDEO_ROW_ITEM_SPACING = 12.dp

private const val ROUTE_HOME = "home"
private const val ROUTE_ALL_VIDEOS = "all_videos"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SETTINGS_PLAYER = "settings/player"
private const val ROUTE_SETTINGS_THEME = "settings/theme"
private const val ROUTE_SETTINGS_MOTION = "settings/motion"
private const val ARG_FOLDER_ID = "folderId"
private const val ROUTE_FOLDER = "folder/{$ARG_FOLDER_ID}"

private fun folderRoute(folderId: Long): String = "folder/$folderId"

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnterTransition(
    durationMs: Int,
): EnterTransition {
    val safeDuration = durationMs.coerceAtLeast(0)
    return fadeIn(animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing)) +
        slideInHorizontally(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            initialOffsetX = { (it * 0.08f).toInt() },
        ) +
        scaleIn(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            initialScale = 0.98f,
        )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExitTransition(
    durationMs: Int,
): ExitTransition {
    val safeDuration = durationMs.coerceAtLeast(0)
    return fadeOut(animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            targetOffsetX = { (-it * 0.06f).toInt() },
        ) +
        scaleOut(
            animationSpec = tween(durationMillis = safeDuration, easing = FastOutSlowInEasing),
            targetScale = 0.985f,
        )
}

private data class LocalVideoFolder(
    val id: Long,
    val name: String,
    val videos: List<LocalVideoItem>,
) {
    val videoCount: Int
        get() = videos.size

    private val totalDurationMs: Long
        get() = videos.sumOf { it.durationMs }.coerceAtLeast(0L)

    private val totalSizeBytes: Long
        get() = videos.sumOf { it.sizeBytes }.coerceAtLeast(0L)

    val totalDurationLabel: String
        get() = formatDuration(totalDurationMs)

    val totalSizeLabel: String
        get() = formatSize(totalSizeBytes)
}

private fun buildFolderGroups(items: List<LocalVideoItem>): List<LocalVideoFolder> {
    return items
        .groupBy { it.folderId }
        .map { (folderId, videos) ->
            LocalVideoFolder(
                id = folderId,
                name = videos.firstOrNull()?.folderName.orEmpty(),
                videos = videos.sortedByDescending { it.dateAddedSec },
            )
        }
        .sortedBy { it.name.lowercase() }
}

private fun formatDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}

private fun formatSize(sizeBytes: Long): String {
    val mb = sizeBytes / (1024f * 1024f)
    return String.format("%.1f MB", mb)
}

private data class LocalVideoItem(
    val id: Long,
    val uri: android.net.Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val folderName: String,
    val folderPath: String,
    val folderId: Long,
    val dateAddedSec: Long,
) {
    val durationLabel: String
        get() = formatDuration(durationMs)

    val sizeLabel: String
        get() = formatSize(sizeBytes)
}

private fun hasVideoPermission(context: android.content.Context): Boolean {
    val permissions = videoPermissionsForRuntime()
    return permissions.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun videoPermissionsForRuntime(): Array<String> {
    return if (android.os.Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun queryLocalVideos(context: android.content.Context): List<LocalVideoItem> {
    val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.BUCKET_ID,
        MediaStore.Video.Media.DATE_ADDED,
    )
    val selection = if (android.os.Build.VERSION.SDK_INT >= 29) {
        "${MediaStore.Video.Media.IS_PENDING}=0"
    } else {
        null
    }
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    return buildList {
        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dataPathIdx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
            val folderNameIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val folderIdIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val dateAddedIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val uri = ContentUris.withAppendedId(collection, id)
                val fallbackFolderName = cursor.getString(folderNameIdx)
                    ?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.unknown_folder)
                val fullFolderPath = if (dataPathIdx >= 0) {
                    cursor.getString(dataPathIdx)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { File(it).parent }
                        ?.takeIf { it.isNotBlank() }
                } else {
                    null
                }?.replace("/storage/emulated/0", "手机存储") ?: fallbackFolderName
                add(
                    LocalVideoItem(
                        id = id,
                        uri = uri,
                        title = cursor.getString(titleIdx) ?: uri.lastPathSegment.orEmpty(),
                        durationMs = cursor.getLong(durationIdx).coerceAtLeast(0L),
                        sizeBytes = cursor.getLong(sizeIdx).coerceAtLeast(0L),
                        folderName = fallbackFolderName,
                        folderPath = fullFolderPath,
                        folderId = cursor.getLong(folderIdIdx),
                        dateAddedSec = cursor.getLong(dateAddedIdx).coerceAtLeast(0L),
                    ),
                )
            }
        }
    }
}

private fun readAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Throwable) {
        "unknown"
    }
}

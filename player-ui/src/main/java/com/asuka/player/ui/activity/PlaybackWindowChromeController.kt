package com.asuka.player.ui.activity

import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.asuka.player.contract.PlaybackRuntimeSettings
import com.asuka.player.contract.PlaybackUiPersistence

internal class PlaybackWindowChromeController(
    private val window: Window,
    private val playbackUiPersistence: PlaybackUiPersistence,
) {
    fun applyBaseWindowStyle() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    fun applySystemBarsForOrientation(orientation: Int) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    fun applyRememberedBrightnessIfNeeded(settings: PlaybackRuntimeSettings) {
        if (!settings.rememberBrightness) return
        val remembered = playbackUiPersistence.readRememberedBrightness() ?: return
        val attrs = window.attributes
        attrs.screenBrightness = remembered.coerceIn(0f, 1f)
        window.attributes = attrs
    }

    fun saveRememberedBrightnessIfNeeded(settings: PlaybackRuntimeSettings) {
        if (!settings.rememberBrightness) return
        val brightness = window.attributes.screenBrightness
        if (brightness >= 0f) {
            playbackUiPersistence.saveRememberedBrightness(brightness)
        }
    }

    fun toggleOrientation(
        requestedOrientation: Int,
        currentOrientation: Int,
    ): Int {
        return resolveNextOrientation(requestedOrientation, currentOrientation)
    }

    companion object {
        internal fun resolveNextOrientation(
            requestedOrientation: Int,
            currentOrientation: Int,
        ): Int {
            val effectiveCurrent = if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                requestedOrientation
            } else {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            return if (effectiveCurrent == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }
}

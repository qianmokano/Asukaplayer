package com.asuka.player.core

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Canonical player settings model shared by app configuration and playback runtime policy.
 */
private object PlayerSettingsDoubleTapActionParceler : Parceler<PlayerSettings.DoubleTapAction> {
    override fun create(parcel: Parcel): PlayerSettings.DoubleTapAction {
        val name = parcel.readString()
        return PlayerSettings.DoubleTapAction.entries
            .firstOrNull { it.name == name }
            ?: PlayerSettings.DoubleTapAction.Seek
    }

    override fun PlayerSettings.DoubleTapAction.write(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }
}

@Parcelize
@TypeParceler<PlayerSettings.DoubleTapAction, PlayerSettingsDoubleTapActionParceler>
data class PlayerSettings(
    val seekGestureEnabled: Boolean = true,
    val brightnessGestureEnabled: Boolean = true,
    val volumeGestureEnabled: Boolean = true,
    val zoomGestureEnabled: Boolean = true,
    val panGestureEnabled: Boolean = true,
    val doubleTapGestureEnabled: Boolean = true,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.Seek,
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
) : Parcelable {
    enum class DoubleTapAction {
        Seek,
        TogglePlayPause,
        Both,
    }
}

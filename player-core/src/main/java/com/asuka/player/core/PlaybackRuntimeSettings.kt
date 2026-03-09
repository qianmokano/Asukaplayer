package com.asuka.player.core

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

/**
 * Serialises [DoubleTapAction] by enum name rather than ordinal, so reordering
 * or inserting enum entries never silently corrupts persisted / in-flight parcels.
 */
private object DoubleTapActionParceler : Parceler<PlaybackRuntimeSettings.DoubleTapAction> {
    override fun create(parcel: Parcel): PlaybackRuntimeSettings.DoubleTapAction {
        val name = parcel.readString()
        return PlaybackRuntimeSettings.DoubleTapAction.entries
            .firstOrNull { it.name == name }
            ?: PlaybackRuntimeSettings.DoubleTapAction.Seek
    }

    override fun PlaybackRuntimeSettings.DoubleTapAction.write(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }
}

@Parcelize
@TypeParceler<PlaybackRuntimeSettings.DoubleTapAction, DoubleTapActionParceler>
data class PlaybackRuntimeSettings(
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
    val hideButtonsBackground: Boolean = true,
    val resumePlayback: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val autoplay: Boolean = true,
    val autoPip: Boolean = true,
    val autoBackgroundPlay: Boolean = false,
    val rememberBrightness: Boolean = false,
    val rememberSelections: Boolean = true,
    val keepSessionConnectionInBackground: Boolean = true,
) : Parcelable {
    enum class DoubleTapAction {
        Seek,
        TogglePlayPause,
        Both,
    }

    companion object {
        const val EXTRA_KEY = "player_runtime_settings"
    }
}

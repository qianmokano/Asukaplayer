package com.asuka.player.ui

import android.os.Parcel
import android.os.Parcelable

data class PlayerRuntimeSettings(
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

    constructor(parcel: Parcel) : this(
        seekGestureEnabled = parcel.readByte() != 0.toByte(),
        brightnessGestureEnabled = parcel.readByte() != 0.toByte(),
        volumeGestureEnabled = parcel.readByte() != 0.toByte(),
        zoomGestureEnabled = parcel.readByte() != 0.toByte(),
        panGestureEnabled = parcel.readByte() != 0.toByte(),
        doubleTapGestureEnabled = parcel.readByte() != 0.toByte(),
        doubleTapAction = DoubleTapAction.entries.getOrElse(parcel.readInt()) { DoubleTapAction.Seek },
        longPressGestureEnabled = parcel.readByte() != 0.toByte(),
        seekIncrementSec = parcel.readInt(),
        seekSensitivity = parcel.readFloat(),
        longPressSpeed = parcel.readFloat(),
        controllerTimeoutSec = parcel.readInt(),
        hideButtonsBackground = parcel.readByte() != 0.toByte(),
        resumePlayback = parcel.readByte() != 0.toByte(),
        defaultPlaybackSpeed = parcel.readFloat(),
        autoplay = parcel.readByte() != 0.toByte(),
        autoPip = parcel.readByte() != 0.toByte(),
        autoBackgroundPlay = parcel.readByte() != 0.toByte(),
        rememberBrightness = parcel.readByte() != 0.toByte(),
        rememberSelections = parcel.readByte() != 0.toByte(),
        keepSessionConnectionInBackground = parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeByte(if (seekGestureEnabled) 1 else 0)
        parcel.writeByte(if (brightnessGestureEnabled) 1 else 0)
        parcel.writeByte(if (volumeGestureEnabled) 1 else 0)
        parcel.writeByte(if (zoomGestureEnabled) 1 else 0)
        parcel.writeByte(if (panGestureEnabled) 1 else 0)
        parcel.writeByte(if (doubleTapGestureEnabled) 1 else 0)
        parcel.writeInt(doubleTapAction.ordinal)
        parcel.writeByte(if (longPressGestureEnabled) 1 else 0)
        parcel.writeInt(seekIncrementSec)
        parcel.writeFloat(seekSensitivity)
        parcel.writeFloat(longPressSpeed)
        parcel.writeInt(controllerTimeoutSec)
        parcel.writeByte(if (hideButtonsBackground) 1 else 0)
        parcel.writeByte(if (resumePlayback) 1 else 0)
        parcel.writeFloat(defaultPlaybackSpeed)
        parcel.writeByte(if (autoplay) 1 else 0)
        parcel.writeByte(if (autoPip) 1 else 0)
        parcel.writeByte(if (autoBackgroundPlay) 1 else 0)
        parcel.writeByte(if (rememberBrightness) 1 else 0)
        parcel.writeByte(if (rememberSelections) 1 else 0)
        parcel.writeByte(if (keepSessionConnectionInBackground) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PlayerRuntimeSettings> {
        const val EXTRA_KEY = "player_runtime_settings"

        override fun createFromParcel(parcel: Parcel): PlayerRuntimeSettings = PlayerRuntimeSettings(parcel)
        override fun newArray(size: Int): Array<PlayerRuntimeSettings?> = arrayOfNulls(size)
    }
}

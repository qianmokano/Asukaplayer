package com.asuka.player.platform

import android.app.Activity
import com.asuka.player.contract.PlaybackDeviceController

fun interface PlaybackDeviceControllerFactory {
    fun create(activity: Activity): PlaybackDeviceController
}

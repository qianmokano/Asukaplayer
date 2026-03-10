package com.asuka.player.app

import android.Manifest
import android.os.Build

internal fun videoPermissionsForRuntime(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 34) {
        arrayOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
    } else if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

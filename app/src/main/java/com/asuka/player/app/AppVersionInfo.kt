package com.asuka.player.app

import android.content.Context

internal fun readAppVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }
}
